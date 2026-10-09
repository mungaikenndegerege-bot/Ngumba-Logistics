package ngumba.logistics;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared data models and persistence for Ngumba Logistics.
 * No UI imports — used by both the Swing app and the REST API server.
 */
public class DataStore {

    // ========================= MODELS =========================

    public static class Truck implements Serializable {
        private static final long serialVersionUID = 1L;
        public String plate, model, driver;
        public LocalDate nextService, insuranceExpiry;

        public Truck() {}
        public Truck(String plate, String model, String driver,
                     LocalDate nextService, LocalDate insuranceExpiry) {
            this.plate = plate;
            this.model = model;
            this.driver = driver;
            this.nextService = nextService;
            this.insuranceExpiry = insuranceExpiry;
        }
    }

    public static class Trip implements Serializable {
        private static final long serialVersionUID = 2L;  // bumped — new fields
        public int id;
        public String plate, route, cargo;
        public LocalDate date;
        public double income, fuel, borderAndTolls, broker, other;
        public int mileage;             // km driven
        public String otherDesc;        // description of "other" costs on the road

        public double cost()   { return fuel + borderAndTolls + broker + other; }
        public double profit() { return income - cost(); }
    }

    public static class Part implements Serializable {
        private static final long serialVersionUID = 1L;
        public int id;
        public LocalDate date;
        public String plate, name;
        public int qty;
        public double cost;
    }

    public static class Staff implements Serializable {
        private static final long serialVersionUID = 2L;  // bumped — new fields added
        public int id;
        public String name, role;
        public String idNumber;       // National ID number
        public String licenseNumber;  // Driving licence number
        public double monthlySalary;
    }

    public static class Misc implements Serializable {
        private static final long serialVersionUID = 1L;
        public int id;
        public LocalDate date;
        public String category, description, plate; // plate="" means general/fleet-wide
        public double amount;
    }

    /** A cash deposit made to a bank account. */
    public static class BankDeposit implements Serializable {
        private static final long serialVersionUID = 1L;
        public int id;
        public LocalDate date;
        public String bank;        // e.g. "Equity", "KCB"
        public String reference;   // deposit slip / receipt number
        public double amount;
    }

    /** Record that payroll was authorised and paid for a given month. */
    public static class PayrollRun implements Serializable {
        private static final long serialVersionUID = 1L;
        public int id;
        public LocalDate date;       // date authorised
        public String month;         // e.g. "2026-10"
        public double totalPaid;     // sum of all salaries at time of authorisation
        public String authorisedBy;
    }

    public static class AppData implements Serializable {
        private static final long serialVersionUID = 4L;  // bumped — new models added
        public List<Truck> trucks  = new ArrayList<Truck>();
        public List<Trip>  trips   = new ArrayList<Trip>();
        public List<Part>  parts   = new ArrayList<Part>();
        public List<Staff> staff   = new ArrayList<Staff>();
        public List<Misc>  misc    = new ArrayList<Misc>();
        public List<BankDeposit>  bankDeposits  = new ArrayList<BankDeposit>();
        public List<PayrollRun>   payrollRuns   = new ArrayList<PayrollRun>();
        public int nextTripId      = 1;
        public int nextPartId      = 1;
        public int nextStaffId     = 1;
        public int nextMiscId      = 1;
        public int nextDepositId   = 1;
        public int nextPayrollRunId = 1;
    }

    // ========================= SINGLETON STATE =========================

    private static final String FILENAME = "ngumba_fleet_app.dat";
    private static AppData data = new AppData();

    public static AppData get() { return data; }

    /**
     * Resolve the data file path using this priority:
     *  1. DATA_DIR environment variable (set on Railway / any cloud host)
     *  2. Same directory as the running JAR (local development)
     *  3. Working directory (last-resort fallback)
     */
    static File dataFile() {
        // 1. Cloud: honour DATA_DIR env var (Railway persistent volume path)
        String dataDir = System.getenv("DATA_DIR");
        if (dataDir != null && !dataDir.trim().isEmpty()) {
            File dir = new File(dataDir.trim());
            dir.mkdirs(); // ensure the directory exists
            return new File(dir, FILENAME);
        }
        // 2. Local: resolve beside the running JAR
        try {
            java.security.CodeSource cs =
                DataStore.class.getProtectionDomain().getCodeSource();
            if (cs != null) {
                File jar = new File(cs.getLocation().toURI());
                File dir = jar.isFile() ? jar.getParentFile() : jar;
                return new File(dir, FILENAME);
            }
        } catch (Exception ignored) { }
        // 3. Fallback
        return new File(FILENAME);
    }

    // ========================= PERSISTENCE =========================

    public static synchronized void save() {
        File f = dataFile();
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f))) {
            out.writeObject(data);
        } catch (IOException e) {
            System.err.println("[DataStore] ERROR saving to " + f.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    public static synchronized void load() {
        File f = dataFile();
        System.out.println("[DataStore] Data file: " + f.getAbsolutePath());
        if (!f.exists()) {
            System.out.println("[DataStore] No existing data file — starting with empty database.");
            data = new AppData();
            return;
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            data = (AppData) in.readObject();
            backfillIds();
            System.out.println("[DataStore] Loaded: "
                + data.trucks.size() + " trucks, "
                + data.trips.size()  + " trips, "
                + data.staff.size()  + " staff.");
        } catch (Exception e) {
            System.err.println("[DataStore] ERROR loading " + f.getAbsolutePath()
                + ": " + e.getMessage()
                + " — renaming corrupt file and starting fresh.");
            // Rename the corrupt file instead of deleting, so data isn't lost silently
            File backup = new File(f.getParent(), FILENAME + ".corrupt_" + System.currentTimeMillis());
            f.renameTo(backup);
            System.err.println("[DataStore] Corrupt file saved as: " + backup.getName());
            data = new AppData();
        }
    }

    /** Assign IDs to any records that were saved without them (migration). */
    private static void backfillIds() {
        for (Part p : data.parts)  if (p.id == 0) p.id = data.nextPartId++;
        for (Staff s : data.staff) {
            if (s.id == 0)               s.id           = data.nextStaffId++;
            if (s.idNumber == null)      s.idNumber     = "";
            if (s.licenseNumber == null) s.licenseNumber = "";
        }
        for (Misc m : data.misc) if (m.id == 0) m.id = data.nextMiscId++;
        // New collections — null-safe init for older .dat files
        if (data.bankDeposits  == null) data.bankDeposits  = new java.util.ArrayList<>();
        if (data.payrollRuns   == null) data.payrollRuns   = new java.util.ArrayList<>();
        for (BankDeposit d : data.bankDeposits) if (d.id == 0) d.id = data.nextDepositId++;
        for (PayrollRun  r : data.payrollRuns)  if (r.id == 0) r.id = data.nextPayrollRunId++;
        // Trip migration — fill nulls for new fields
        for (Trip t : data.trips) {
            if (t.otherDesc == null) t.otherDesc = "";
        }
    }

    // ========================= LOOKUP HELPERS =========================

    public static Truck findTruck(String plate) {
        for (Truck t : data.trucks)
            if (t.plate.equalsIgnoreCase(plate.trim())) return t;
        return null;
    }

    public static Trip findTrip(int id) {
        for (Trip t : data.trips) if (t.id == id) return t;
        return null;
    }

    public static Part findPart(int id) {
        for (Part p : data.parts) if (p.id == id) return p;
        return null;
    }

    public static Staff findStaff(int id) {
        for (Staff s : data.staff) if (s.id == id) return s;
        return null;
    }

    public static Misc findMisc(int id) {
        for (Misc m : data.misc) if (m.id == id) return m;
        return null;
    }

    // ========================= ROUTE CONSTANTS =========================

    public static final String[] ROUTES = {
        "Nairobi - Mombasa", "Mombasa - Kampala", "Nairobi - Kampala",
        "Kampala - Kigali", "Mombasa - Lusaka", "Mombasa - DRC (Lubumbashi)"
    };

    public static final String[] MISC_CATEGORIES = {
        "Licences & permits", "Fines", "Insurance", "Loading / offloading",
        "Accommodation & meals", "Airtime & communication",
        "Office & admin", "Tyres & washing", "Other"
    };
}
