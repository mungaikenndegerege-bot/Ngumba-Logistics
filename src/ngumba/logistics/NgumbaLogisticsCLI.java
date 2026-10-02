package ngumba.logistics;

import java.io.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * NGUMBA LOGISTICS - Fleet Management (console / CLI app)
 * This is the original text-based version. The main GUI app is NgumbaLogistics (NgumbaFleet2).
 * Compile and run via NetBeans, or:
 *   javac -d out src/ngumba/logistics/NgumbaLogisticsCLI.java
 *   java -cp out ngumba.logistics.NgumbaLogisticsCLI
 * Data is saved automatically to ngumba_fleet.dat in the working directory.
 * Requires Java 8 or newer.
 */
public class NgumbaLogisticsCLI {

    // ===================== MODELS =====================
    static class Truck implements Serializable {
        String plate, model, driver;
        LocalDate nextService, insuranceExpiry;
        Truck(String plate, String model, String driver, LocalDate nextService, LocalDate insuranceExpiry) {
            this.plate = plate; this.model = model; this.driver = driver;
            this.nextService = nextService; this.insuranceExpiry = insuranceExpiry;
        }
    }

    static class Trip implements Serializable {
        int id; String plate, route, cargo; LocalDate date;
        double income, fuel, borderAndTolls, other;
        double cost() { return fuel + borderAndTolls + other; }
        double profit() { return income - cost(); }
    }

    static class Part implements Serializable {
        LocalDate date; String plate, name; int qty; double cost;
    }

    static class Staff implements Serializable {
        String name, role; double monthlySalary;
    }

    static class Data implements Serializable {
        List<Truck> trucks = new ArrayList<>();
        List<Trip> trips = new ArrayList<>();
        List<Part> parts = new ArrayList<>();
        List<Staff> staff = new ArrayList<>();
        int nextTripId = 1;
    }

    // ===================== STATE =====================
    static final String FILE = "ngumba_fleet.dat";
    static final String[] ROUTES = {
        "Nairobi - Mombasa", "Mombasa - Kampala", "Nairobi - Kampala",
        "Kampala - Kigali", "Mombasa - Lusaka", "Mombasa - DRC (Lubumbashi)"
    };
    static Data data = new Data();
    static final Scanner in = new Scanner(System.in);

    // ===================== MAIN =====================
    public static void main(String[] args) {
        load();
        System.out.println("=====================================");
        System.out.println("   NGUMBA LOGISTICS - FLEET MANAGER  ");
        System.out.println("=====================================");
        showAlerts();
        while (true) {
            System.out.println("\n--- MENU ---");
            System.out.println("1. Add truck");
            System.out.println("2. List trucks");
            System.out.println("3. Record trip");
            System.out.println("4. List trips");
            System.out.println("5. Record spare part");
            System.out.println("6. Add staff");
            System.out.println("7. List staff / payroll");
            System.out.println("8. Financial summary");
            System.out.println("9. Service & insurance alerts");
            System.out.println("10. Update truck service/insurance dates");
            System.out.println("0. Save & exit");
            int choice = readInt("Choose: ");
            switch (choice) {
                case 1: addTruck(); break;
                case 2: listTrucks(); break;
                case 3: addTrip(); break;
                case 4: listTrips(); break;
                case 5: addPart(); break;
                case 6: addStaff(); break;
                case 7: listStaff(); break;
                case 8: summary(); break;
                case 9: showAlerts(); break;
                case 10: updateDates(); break;
                case 0: save(); System.out.println("Saved. Safe travels!"); return;
                default: System.out.println("Invalid option.");
            }
            save();
        }
    }

    // ===================== ACTIONS =====================
    static void addTruck() {
        String plate = readStr("Plate number (e.g. KDG 123X): ").toUpperCase();
        if (findTruck(plate) != null) { System.out.println("Truck already exists."); return; }
        String model = readStrDefault("Model", "HOWO T7H 440");
        String driver = readStr("Assigned driver: ");
        LocalDate svc = readDate("Next service date (yyyy-MM-dd): ");
        LocalDate ins = readDate("Insurance expiry (yyyy-MM-dd): ");
        data.trucks.add(new Truck(plate, model, driver, svc, ins));
        System.out.println("Truck added.");
    }

    static void listTrucks() {
        if (data.trucks.isEmpty()) { System.out.println("No trucks yet."); return; }
        System.out.printf("%n%-12s %-16s %-14s %-12s %-12s%n", "PLATE", "MODEL", "DRIVER", "SERVICE", "INSURANCE");
        for (Truck t : data.trucks)
            System.out.printf("%-12s %-16s %-14s %-12s %-12s%n", t.plate, t.model, t.driver, t.nextService, t.insuranceExpiry);
    }

    static void addTrip() {
        if (data.trucks.isEmpty()) { System.out.println("Add a truck first."); return; }
        Truck t = pickTruck();
        if (t == null) return;
        Trip trip = new Trip();
        trip.id = data.nextTripId++;
        trip.plate = t.plate;
        System.out.println("Routes:");
        for (int i = 0; i < ROUTES.length; i++) System.out.println("  " + (i + 1) + ". " + ROUTES[i]);
        System.out.println("  0. Custom route");
        int r = readInt("Route: ");
        trip.route = (r >= 1 && r <= ROUTES.length) ? ROUTES[r - 1] : readStr("Enter route: ");
        trip.cargo = readStr("Cargo: ");
        trip.date = readDate("Trip date (yyyy-MM-dd): ");
        trip.income = readDouble("Income (KSh): ");
        trip.fuel = readDouble("Fuel cost (KSh): ");
        trip.borderAndTolls = readDouble("Border/toll/weighbridge costs (KSh): ");
        trip.other = readDouble("Other expenses (KSh): ");
        data.trips.add(trip);
        System.out.printf("Trip #%d recorded. Profit: KSh %,.2f%n", trip.id, trip.profit());
    }

    static void listTrips() {
        if (data.trips.isEmpty()) { System.out.println("No trips yet."); return; }
        System.out.printf("%n%-4s %-12s %-12s %-28s %-12s %12s %12s%n",
                "ID", "DATE", "PLATE", "ROUTE", "CARGO", "INCOME", "PROFIT");
        for (Trip t : data.trips)
            System.out.printf("%-4d %-12s %-12s %-28s %-12s %,12.0f %,12.0f%n",
                    t.id, t.date, t.plate, t.route, t.cargo, t.income, t.profit());
    }

    static void addPart() {
        if (data.trucks.isEmpty()) { System.out.println("Add a truck first."); return; }
        Truck t = pickTruck();
        if (t == null) return;
        Part p = new Part();
        p.plate = t.plate;
        p.name = readStr("Part name: ");
        p.qty = readInt("Quantity: ");
        p.cost = readDouble("Total cost (KSh): ");
        p.date = readDate("Date (yyyy-MM-dd): ");
        data.parts.add(p);
        System.out.println("Spare part recorded.");
    }

    static void addStaff() {
        Staff s = new Staff();
        s.name = readStr("Name: ");
        s.role = readStr("Role (Driver/Turn boy/Mechanic/Office): ");
        s.monthlySalary = readDouble("Monthly salary (KSh): ");
        data.staff.add(s);
        System.out.println("Staff added.");
    }

    static void listStaff() {
        if (data.staff.isEmpty()) { System.out.println("No staff yet."); return; }
        double total = 0;
        System.out.printf("%n%-20s %-16s %14s%n", "NAME", "ROLE", "SALARY (KSh)");
        for (Staff s : data.staff) {
            System.out.printf("%-20s %-16s %,14.2f%n", s.name, s.role, s.monthlySalary);
            total += s.monthlySalary;
        }
        System.out.printf("%-37s %,14.2f%n", "TOTAL MONTHLY PAYROLL", total);
    }

    static void summary() {
        double income = 0, tripCosts = 0, partsCost = 0, payroll = 0;
        for (Trip t : data.trips) { income += t.income; tripCosts += t.cost(); }
        for (Part p : data.parts) partsCost += p.cost;
        for (Staff s : data.staff) payroll += s.monthlySalary;

        System.out.println("\n========= FINANCIAL SUMMARY =========");
        System.out.printf("Total income:           KSh %,15.2f%n", income);
        System.out.printf("Trip expenses:          KSh %,15.2f%n", tripCosts);
        System.out.printf("Spare parts:            KSh %,15.2f%n", partsCost);
        System.out.printf("Monthly payroll:        KSh %,15.2f%n", payroll);
        System.out.printf("NET (before payroll):   KSh %,15.2f%n", income - tripCosts - partsCost);
        System.out.printf("NET (after 1 mth pay):  KSh %,15.2f%n", income - tripCosts - partsCost - payroll);

        if (!data.trucks.isEmpty()) {
            System.out.println("\n--- Per truck ---");
            System.out.printf("%-12s %6s %14s %14s %14s%n", "PLATE", "TRIPS", "INCOME", "COSTS", "PROFIT");
            for (Truck tr : data.trucks) {
                double inc = 0, cost = 0; int n = 0;
                for (Trip t : data.trips) if (t.plate.equals(tr.plate)) { inc += t.income; cost += t.cost(); n++; }
                for (Part p : data.parts) if (p.plate.equals(tr.plate)) cost += p.cost;
                System.out.printf("%-12s %6d %,14.0f %,14.0f %,14.0f%n", tr.plate, n, inc, cost, inc - cost);
            }
        }
    }

    static void showAlerts() {
        LocalDate today = LocalDate.now();
        boolean any = false;
        for (Truck t : data.trucks) {
            long svc = ChronoUnit.DAYS.between(today, t.nextService);
            long ins = ChronoUnit.DAYS.between(today, t.insuranceExpiry);
            if (svc <= 30) { any = true; System.out.printf("[!] %s service %s (%s)%n", t.plate, svc < 0 ? "OVERDUE by " + (-svc) + " days" : "due in " + svc + " days", t.nextService); }
            if (ins <= 30) { any = true; System.out.printf("[!] %s insurance %s (%s)%n", t.plate, ins < 0 ? "EXPIRED " + (-ins) + " days ago" : "expires in " + ins + " days", t.insuranceExpiry); }
        }
        if (!any) System.out.println("No service or insurance alerts. All good.");
    }

    static void updateDates() {
        Truck t = pickTruck();
        if (t == null) return;
        t.nextService = readDate("New next service date (yyyy-MM-dd): ");
        t.insuranceExpiry = readDate("New insurance expiry (yyyy-MM-dd): ");
        System.out.println("Updated.");
    }

    // ===================== HELPERS =====================
    static Truck findTruck(String plate) {
        for (Truck t : data.trucks) if (t.plate.equalsIgnoreCase(plate.trim())) return t;
        return null;
    }

    static Truck pickTruck() {
        listTrucks();
        Truck t = findTruck(readStr("Enter truck plate: "));
        if (t == null) System.out.println("Truck not found.");
        return t;
    }

    static String readStr(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = in.nextLine().trim();
            if (!s.isEmpty()) return s;
        }
    }

    static String readStrDefault(String prompt, String def) {
        System.out.print(prompt + " [" + def + "]: ");
        String s = in.nextLine().trim();
        return s.isEmpty() ? def : s;
    }

    static int readInt(String prompt) {
        while (true) {
            try { return Integer.parseInt(readStr(prompt)); }
            catch (NumberFormatException e) { System.out.println("Enter a whole number."); }
        }
    }

    static double readDouble(String prompt) {
        while (true) {
            try { return Double.parseDouble(readStr(prompt).replace(",", "")); }
            catch (NumberFormatException e) { System.out.println("Enter a valid number."); }
        }
    }

    static LocalDate readDate(String prompt) {
        while (true) {
            try { return LocalDate.parse(readStr(prompt)); }
            catch (Exception e) { System.out.println("Use format yyyy-MM-dd, e.g. 2026-10-15"); }
        }
    }

    // ===================== STORAGE =====================
    static void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE))) {
            out.writeObject(data);
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    static void load() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (ObjectInputStream inp = new ObjectInputStream(new FileInputStream(f))) {
            data = (Data) inp.readObject();
        } catch (Exception e) {
            System.out.println("Could not load saved data, starting fresh: " + e.getMessage());
        }
    }
}