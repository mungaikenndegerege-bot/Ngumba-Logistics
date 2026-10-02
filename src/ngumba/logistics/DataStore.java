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
        private static final long serialVersionUID = 1L;
        public int id;
        public String plate, route, cargo;
        public LocalDate date;
        public double income, fuel, borderAndTolls, other;

        public double cost()   { return fuel + borderAndTolls + other; }
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
        private static final long serialVersionUID = 1L;
        public int id;
        public String name, role;
        public double monthlySalary;
    }

    public static class Misc implements Serializable {
        private static final long serialVersionUID = 1L;
        public int id;
        public LocalDate date;
        public String category, description, plate; // plate="" means general/fleet-wide
        public double amount;
    }

    public static class AppData implements Serializable {
        private static final long serialVersionUID = 2L;
        public List<Truck> trucks  = new ArrayList<Truck>();
        public List<Trip>  trips   = new ArrayList<Trip>();
        public List<Part>  parts   = new ArrayList<Part>();
        public List<Staff> staff   = new ArrayList<Staff>();
        public List<Misc>  misc    = new ArrayList<Misc>();
        public int nextTripId  = 1;
        public int nextPartId  = 1;
        public int nextStaffId = 1;
        public int nextMiscId  = 1;
    }

    // ========================= SINGLETON STATE =========================

    private static final String FILENAME = "ngumba_fleet_app.dat";
    private static AppData data = new AppData();

    public static AppData get() { return data; }

    /**
     * Resolve the data file path to the same directory as the running JAR.
     * This ensures save/load always use the same file regardless of what the
     * working directory happens to be when the JVM is launched.
     */
    static File dataFile() {
        try {
            // CodeSource gives us the location of the .jar (or .class) file
            java.security.CodeSource cs =
                DataStore.class.getProtectionDomain().getCodeSource();
            if (cs != null) {
                File jar = new File(cs.getLocation().toURI());
                // If it's the JAR itself, go up one level to the dist/ parent, then save beside it
                File dir = jar.isFile() ? jar.getParentFile() : jar;
                return new File(dir, FILENAME);
            }
        } catch (Exception ignored) { }
        // Fallback: working directory
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
        for (Staff s : data.staff) if (s.id == 0) s.id = data.nextStaffId++;
        for (Misc m : data.misc)   if (m.id == 0) m.id = data.nextMiscId++;
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
