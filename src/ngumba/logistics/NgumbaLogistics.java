package ngumba.logistics;

import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * NGUMBA LOGISTICS - Fleet Manager (Swing desktop app, Java 8+)
 *
 * Tabs: Dashboard | Trucks | Trips | Spare Parts | Staff | Misc Expenses | Monthly Report
 * Data saves automatically to ngumba_fleet_app.dat (same folder as the program).
 * Reports export to a "reports" folder as HTML (print to PDF) and CSV (opens in Excel).
 */
public class NgumbaLogistics {

    // ============================ MODELS ============================
    static class Truck implements Serializable {
        private static final long serialVersionUID = 1L;
        String plate, model, driver;
        LocalDate nextService, insuranceExpiry;
        Truck(String plate, String model, String driver, LocalDate nextService, LocalDate insuranceExpiry) {
            this.plate = plate; this.model = model; this.driver = driver;
            this.nextService = nextService; this.insuranceExpiry = insuranceExpiry;
        }
    }

    static class Trip implements Serializable {
        private static final long serialVersionUID = 1L;
        int id; String plate, route, cargo; LocalDate date;
        double income, fuel, borderAndTolls, other;
        double cost() { return fuel + borderAndTolls + other; }
        double profit() { return income - cost(); }
    }

    static class Part implements Serializable {
        private static final long serialVersionUID = 1L;
        LocalDate date; String plate, name; int qty; double cost;
    }

    static class Staff implements Serializable {
        private static final long serialVersionUID = 1L;
        String name, role; double monthlySalary;
    }

    static class Misc implements Serializable {
        private static final long serialVersionUID = 1L;
        LocalDate date; String category, description, plate; double amount; // plate "" = general/fleet-wide
    }

    static class Data implements Serializable {
        private static final long serialVersionUID = 1L;
        List<Truck> trucks = new ArrayList<Truck>();
        List<Trip> trips = new ArrayList<Trip>();
        List<Part> parts = new ArrayList<Part>();
        List<Staff> staff = new ArrayList<Staff>();
        List<Misc> misc = new ArrayList<Misc>();
        int nextTripId = 1;
    }

    static class Totals {
        double income, tripCost, parts, misc, payroll;
        double net() { return income - tripCost - parts - misc - payroll; }
    }

    // ============================ STATE ============================
    static final String FILE = "ngumba_fleet_app.dat";
    static final String[] ROUTES = {
        "Nairobi - Mombasa", "Mombasa - Kampala", "Nairobi - Kampala",
        "Kampala - Kigali", "Mombasa - Lusaka", "Mombasa - DRC (Lubumbashi)"
    };
    static final String[] MISC_CATEGORIES = {
        "Licences & permits", "Fines", "Insurance", "Loading / offloading", "Accommodation & meals",
        "Airtime & communication", "Office & admin", "Tyres & washing", "Other"
    };
    static final String[] MONTHS = {"January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"};
    static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    static Data data = new Data();
    static JFrame frame;
    static DefaultTableModel truckM, tripM, partM, staffM, miscM;
    static JTable truckT, tripT, partT, staffT, miscT;
    static JTextArea dashArea, reportArea;
    static JComboBox<String> monthBox;
    static JComboBox<Integer> yearBox;

    // ============================ MAIN ============================
    public static void main(String[] args) {
        load();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
                buildUI();
                refreshAll();
                generateReport();
                frame.setVisible(true);
            }
        });
    }

    // ============================ UI ============================
    static void buildUI() {
        frame = new JFrame("Ngumba Logistics - Fleet Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 660);
        frame.setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();

        dashArea = area();
        tabs.addTab("Dashboard", new JScrollPane(dashArea));

        truckM = model("Plate", "Model", "Driver", "Next service", "Insurance expiry");
        truckT = new JTable(truckM);
        tabs.addTab("Trucks", tablePanel(truckT,
                btn("Add truck", e -> addTruck()),
                btn("Update dates", e -> updateDates()),
                btn("Delete", e -> deleteRow(truckT, data.trucks, "this truck"))));

        tripM = model("ID", "Date", "Truck", "Route", "Cargo", "Income", "Costs", "Profit");
        tripT = new JTable(tripM);
        tabs.addTab("Trips", tablePanel(tripT,
                btn("Record trip", e -> addTrip()),
                btn("Delete", e -> deleteRow(tripT, data.trips, "this trip"))));

        partM = model("Date", "Truck", "Part", "Qty", "Cost");
        partT = new JTable(partM);
        tabs.addTab("Spare Parts", tablePanel(partT,
                btn("Add spare part", e -> addPart()),
                btn("Delete", e -> deleteRow(partT, data.parts, "this entry"))));

        staffM = model("Name", "Role", "Monthly salary");
        staffT = new JTable(staffM);
        tabs.addTab("Staff", tablePanel(staffT,
                btn("Add staff", e -> addStaff()),
                btn("Delete", e -> deleteRow(staffT, data.staff, "this staff member"))));

        miscM = model("Date", "Category", "Description", "Truck", "Amount");
        miscT = new JTable(miscM);
        tabs.addTab("Misc Expenses", tablePanel(miscT,
                btn("Add expense", e -> addMisc()),
                btn("Delete", e -> deleteRow(miscT, data.misc, "this expense"))));

        tabs.addTab("Monthly Report", reportPanel());

        frame.add(tabs);
    }

    static JPanel reportPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        monthBox = new JComboBox<String>(MONTHS);
        monthBox.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        int thisYear = LocalDate.now().getYear();
        Integer[] years = new Integer[7];
        for (int i = 0; i < years.length; i++) years[i] = thisYear - 5 + i;
        yearBox = new JComboBox<Integer>(years);
        yearBox.setSelectedItem(Integer.valueOf(thisYear));
        top.add(new JLabel("Month:"));
        top.add(monthBox);
        top.add(new JLabel("Year:"));
        top.add(yearBox);
        top.add(btn("Generate", e -> generateReport()));
        top.add(btn("Export HTML (print / PDF)", e -> exportHtml()));
        top.add(btn("Export CSV (Excel)", e -> exportCsv()));
        reportArea = area();
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(reportArea), BorderLayout.CENTER);
        return p;
    }

    static JTextArea area() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        a.setMargin(new Insets(10, 10, 10, 10));
        return a;
    }

    static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    static JPanel tablePanel(JTable t, JButton... buttons) {
        t.setRowHeight(24);
        t.setFillsViewportHeight(true);
        JPanel p = new JPanel(new BorderLayout());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        for (JButton b : buttons) bottom.add(b);
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    static JButton btn(String text, ActionListener al) {
        JButton b = new JButton(text);
        b.addActionListener(al);
        return b;
    }

    // ============================ REFRESH ============================
    static void refreshAll() {
        truckM.setRowCount(0);
        for (Truck t : data.trucks)
            truckM.addRow(new Object[]{t.plate, t.model, t.driver, t.nextService, t.insuranceExpiry});

        tripM.setRowCount(0);
        for (Trip t : data.trips)
            tripM.addRow(new Object[]{t.id, t.date, t.plate, t.route, t.cargo, m(t.income), m(t.cost()), m(t.profit())});

        partM.setRowCount(0);
        for (Part p : data.parts)
            partM.addRow(new Object[]{p.date, p.plate, p.name, p.qty, m(p.cost)});

        staffM.setRowCount(0);
        for (Staff s : data.staff)
            staffM.addRow(new Object[]{s.name, s.role, m(s.monthlySalary)});

        miscM.setRowCount(0);
        for (Misc x : data.misc)
            miscM.addRow(new Object[]{x.date, x.category, x.description, x.plate.isEmpty() ? "(General)" : x.plate, m(x.amount)});

        dashArea.setText(dashboardText());
    }

    static void changed() {
        save();
        refreshAll();
        if (reportArea != null) generateReport();
    }

    // ============================ ACTIONS ============================
    static void addTruck() {
        JTextField plate = tf(""), model = tf("HOWO T7H 440"), driver = tf("");
        JTextField svc = tf(LocalDate.now().plusMonths(3).toString());
        JTextField ins = tf(LocalDate.now().plusYears(1).toString());
        if (!form("Add truck", new String[]{"Plate number", "Model", "Driver", "Next service (yyyy-MM-dd)", "Insurance expiry (yyyy-MM-dd)"},
                new JComponent[]{plate, model, driver, svc, ins})) return;
        String p = plate.getText().trim().toUpperCase();
        if (p.isEmpty()) { err("Plate number is required."); return; }
        if (findTruck(p) != null) { err("That truck already exists."); return; }
        LocalDate s = date(svc.getText()), i = date(ins.getText());
        if (s == null || i == null) return;
        data.trucks.add(new Truck(p, model.getText().trim(), driver.getText().trim(), s, i));
        changed();
    }

    static void updateDates() {
        int r = selected(truckT);
        if (r < 0) return;
        Truck t = data.trucks.get(r);
        JTextField svc = tf(t.nextService.toString()), ins = tf(t.insuranceExpiry.toString());
        if (!form("Update dates - " + t.plate, new String[]{"Next service (yyyy-MM-dd)", "Insurance expiry (yyyy-MM-dd)"},
                new JComponent[]{svc, ins})) return;
        LocalDate s = date(svc.getText()), i = date(ins.getText());
        if (s == null || i == null) return;
        t.nextService = s;
        t.insuranceExpiry = i;
        changed();
    }

    static void addTrip() {
        if (data.trucks.isEmpty()) { err("Add a truck first."); return; }
        JComboBox<String> truck = truckCombo(false);
        JComboBox<String> route = new JComboBox<String>(ROUTES);
        route.setEditable(true);
        JTextField cargo = tf(""), date = tf(LocalDate.now().toString());
        JTextField income = tf(""), fuel = tf("0"), border = tf("0"), other = tf("0");
        if (!form("Record trip", new String[]{"Truck", "Route (you can type your own)", "Cargo", "Date (yyyy-MM-dd)",
                "Income (KSh)", "Fuel cost (KSh)", "Border / tolls / weighbridge (KSh)", "Other trip costs (KSh)"},
                new JComponent[]{truck, route, cargo, date, income, fuel, border, other})) return;
        LocalDate d = date(date.getText());
        Double inc = num(income.getText(), "Income"), f = num(fuel.getText(), "Fuel"),
                b = num(border.getText(), "Border/tolls"), o = num(other.getText(), "Other costs");
        if (d == null || inc == null || f == null || b == null || o == null) return;
        Trip t = new Trip();
        t.id = data.nextTripId++;
        t.plate = (String) truck.getSelectedItem();
        t.route = String.valueOf(route.getSelectedItem()).trim();
        t.cargo = cargo.getText().trim();
        t.date = d; t.income = inc; t.fuel = f; t.borderAndTolls = b; t.other = o;
        data.trips.add(t);
        changed();
    }

    static void addPart() {
        if (data.trucks.isEmpty()) { err("Add a truck first."); return; }
        JComboBox<String> truck = truckCombo(false);
        JTextField name = tf(""), qty = tf("1"), cost = tf(""), date = tf(LocalDate.now().toString());
        if (!form("Add spare part", new String[]{"Truck", "Part name", "Quantity", "Total cost (KSh)", "Date (yyyy-MM-dd)"},
                new JComponent[]{truck, name, qty, cost, date})) return;
        LocalDate d = date(date.getText());
        Double q = num(qty.getText(), "Quantity"), c = num(cost.getText(), "Cost");
        if (d == null || q == null || c == null) return;
        Part p = new Part();
        p.plate = (String) truck.getSelectedItem();
        p.name = name.getText().trim();
        p.qty = q.intValue(); p.cost = c; p.date = d;
        data.parts.add(p);
        changed();
    }

    static void addStaff() {
        JTextField name = tf(""), role = tf("Driver"), salary = tf("");
        if (!form("Add staff", new String[]{"Name", "Role", "Monthly salary (KSh)"}, new JComponent[]{name, role, salary})) return;
        Double s = num(salary.getText(), "Salary");
        if (s == null) return;
        if (name.getText().trim().isEmpty()) { err("Name is required."); return; }
        Staff st = new Staff();
        st.name = name.getText().trim(); st.role = role.getText().trim(); st.monthlySalary = s;
        data.staff.add(st);
        changed();
    }

    static void addMisc() {
        JComboBox<String> cat = new JComboBox<String>(MISC_CATEGORIES);
        cat.setEditable(true);
        JComboBox<String> truck = truckCombo(true);
        JTextField desc = tf(""), amount = tf(""), date = tf(LocalDate.now().toString());
        if (!form("Add miscellaneous expense", new String[]{"Category (or type your own)", "Description", "Truck (optional)", "Amount (KSh)", "Date (yyyy-MM-dd)"},
                new JComponent[]{cat, desc, truck, amount, date})) return;
        LocalDate d = date(date.getText());
        Double a = num(amount.getText(), "Amount");
        if (d == null || a == null) return;
        Misc x = new Misc();
        x.category = String.valueOf(cat.getSelectedItem()).trim();
        x.description = desc.getText().trim();
        String sel = (String) truck.getSelectedItem();
        x.plate = (sel == null || sel.startsWith("(")) ? "" : sel;
        x.amount = a; x.date = d;
        data.misc.add(x);
        changed();
    }

    static <T> void deleteRow(JTable t, List<T> list, String what) {
        int r = selected(t);
        if (r < 0) return;
        if (JOptionPane.showConfirmDialog(frame, "Delete " + what + "?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            list.remove(r);
            changed();
        }
    }

    // ============================ REPORTS ============================
    static YearMonth selectedMonth() {
        return YearMonth.of((Integer) yearBox.getSelectedItem(), monthBox.getSelectedIndex() + 1);
    }

    static void generateReport() {
        reportArea.setText(reportText(selectedMonth()));
        reportArea.setCaretPosition(0);
    }

    static boolean in(LocalDate d, YearMonth ym) {
        return YearMonth.from(d).equals(ym);
    }

    static Totals totals(YearMonth ym) {
        Totals t = new Totals();
        for (Trip x : data.trips) if (in(x.date, ym)) { t.income += x.income; t.tripCost += x.cost(); }
        for (Part x : data.parts) if (in(x.date, ym)) t.parts += x.cost;
        for (Misc x : data.misc) if (in(x.date, ym)) t.misc += x.amount;
        for (Staff x : data.staff) t.payroll += x.monthlySalary;
        return t;
    }

    /** per-truck rows: {plate, trips, income, costs, profit} */
    static List<double[]> truckRows(YearMonth ym, List<String> plates) {
        List<double[]> rows = new ArrayList<double[]>();
        for (Truck tr : data.trucks) {
            double inc = 0, cost = 0, n = 0;
            for (Trip x : data.trips) if (x.plate.equals(tr.plate) && in(x.date, ym)) { inc += x.income; cost += x.cost(); n++; }
            for (Part x : data.parts) if (x.plate.equals(tr.plate) && in(x.date, ym)) cost += x.cost;
            for (Misc x : data.misc) if (x.plate.equals(tr.plate) && in(x.date, ym)) cost += x.amount;
            plates.add(tr.plate);
            rows.add(new double[]{n, inc, cost, inc - cost});
        }
        return rows;
    }

    static String reportText(YearMonth ym) {
        Totals t = totals(ym);
        String bar = "======================================================================\n";
        StringBuilder s = new StringBuilder();
        s.append(bar).append(" NGUMBA LOGISTICS - MONTHLY REPORT: ").append(ym.format(MONTH_FMT).toUpperCase()).append("\n").append(bar).append("\n");
        s.append("SUMMARY\n");
        s.append(String.format("  Trip income ............ KSh %,16.2f%n", t.income));
        s.append(String.format("  Trip expenses .......... KSh %,16.2f%n", t.tripCost));
        s.append(String.format("  Spare parts ............ KSh %,16.2f%n", t.parts));
        s.append(String.format("  Miscellaneous .......... KSh %,16.2f%n", t.misc));
        s.append(String.format("  Staff payroll .......... KSh %,16.2f%n", t.payroll));
        s.append(String.format("  NET PROFIT / (LOSS) .... KSh %,16.2f%n", t.net()));

        s.append("\nPER TRUCK (income less trip costs, parts and misc linked to the truck)\n");
        List<String> plates = new ArrayList<String>();
        List<double[]> rows = truckRows(ym, plates);
        if (rows.isEmpty()) s.append("  No trucks.\n");
        else s.append(String.format("  %-12s %5s %14s %14s %14s%n", "TRUCK", "TRIPS", "INCOME", "COSTS", "PROFIT"));
        for (int i = 0; i < rows.size(); i++) {
            double[] r = rows.get(i);
            s.append(String.format("  %-12s %5d %,14.0f %,14.0f %,14.0f%n", plates.get(i), (int) r[0], r[1], r[2], r[3]));
        }

        s.append("\nTRIPS\n");
        boolean any = false;
        for (Trip x : data.trips) if (in(x.date, ym)) {
            if (!any) s.append(String.format("  %-11s %-11s %-26s %-12s %12s %12s%n", "DATE", "TRUCK", "ROUTE", "CARGO", "INCOME", "PROFIT"));
            any = true;
            s.append(String.format("  %-11s %-11s %-26s %-12s %,12.0f %,12.0f%n", x.date, x.plate, cut(x.route, 26), cut(x.cargo, 12), x.income, x.profit()));
        }
        if (!any) s.append("  No trips this month.\n");

        s.append("\nSPARE PARTS\n");
        any = false;
        for (Part x : data.parts) if (in(x.date, ym)) {
            any = true;
            s.append(String.format("  %-11s %-11s %-28s x%-3d %,12.0f%n", x.date, x.plate, cut(x.name, 28), x.qty, x.cost));
        }
        if (!any) s.append("  None.\n");

        s.append("\nMISCELLANEOUS EXPENSES\n");
        any = false;
        for (Misc x : data.misc) if (in(x.date, ym)) {
            any = true;
            s.append(String.format("  %-11s %-22s %-24s %-10s %,12.0f%n", x.date, cut(x.category, 22), cut(x.description, 24),
                    x.plate.isEmpty() ? "General" : x.plate, x.amount));
        }
        if (!any) s.append("  None.\n");

        s.append("\nPAYROLL (monthly salaries)\n");
        if (data.staff.isEmpty()) s.append("  No staff.\n");
        for (Staff x : data.staff) s.append(String.format("  %-24s %-16s %,12.2f%n", cut(x.name, 24), cut(x.role, 16), x.monthlySalary));

        s.append("\nSERVICE / INSURANCE ALERTS\n");
        s.append(alertsText());
        return s.toString();
    }

    static String reportHtml(YearMonth ym) {
        Totals t = totals(ym);
        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Ngumba Logistics - ").append(ym.format(MONTH_FMT)).append("</title>");
        h.append("<style>body{font-family:Arial,sans-serif;margin:30px;color:#222}h1{margin:0}h2{margin-top:28px;border-bottom:2px solid #0a7d3b;padding-bottom:4px;font-size:18px}");
        h.append("table{border-collapse:collapse;width:100%}th,td{border:1px solid #ccc;padding:6px 8px;text-align:left;font-size:13px}th{background:#f0f0f0}");
        h.append("td.n{text-align:right}.pos{color:#0a7d3b}.neg{color:#c00}.tot td{font-weight:bold;background:#fafafa}.sub{color:#666;margin-top:4px}");
        h.append("@media print{body{margin:10px}}</style></head><body>");
        h.append("<h1>NGUMBA LOGISTICS</h1><div class='sub'>Monthly Report - ").append(ym.format(MONTH_FMT)).append(" &nbsp;|&nbsp; Generated ").append(LocalDate.now()).append("</div>");

        h.append("<h2>Summary</h2><table>");
        h.append(sumRow("Trip income", t.income, false)).append(sumRow("Trip expenses", t.tripCost, false));
        h.append(sumRow("Spare parts", t.parts, false)).append(sumRow("Miscellaneous expenses", t.misc, false));
        h.append(sumRow("Staff payroll", t.payroll, false)).append(sumRow("NET PROFIT / (LOSS)", t.net(), true));
        h.append("</table>");

        h.append("<h2>Per truck</h2><table><tr><th>Truck</th><th>Trips</th><th>Income</th><th>Costs</th><th>Profit</th></tr>");
        List<String> plates = new ArrayList<String>();
        List<double[]> rows = truckRows(ym, plates);
        for (int i = 0; i < rows.size(); i++) {
            double[] r = rows.get(i);
            h.append("<tr><td>").append(esc(plates.get(i))).append("</td><td class='n'>").append((int) r[0]).append("</td><td class='n'>").append(m(r[1]))
                    .append("</td><td class='n'>").append(m(r[2])).append("</td><td class='n ").append(r[3] >= 0 ? "pos" : "neg").append("'>").append(m(r[3])).append("</td></tr>");
        }
        h.append("</table>");

        h.append("<h2>Trips</h2><table><tr><th>Date</th><th>Truck</th><th>Route</th><th>Cargo</th><th>Income</th><th>Costs</th><th>Profit</th></tr>");
        for (Trip x : data.trips) if (in(x.date, ym))
            h.append("<tr><td>").append(x.date).append("</td><td>").append(esc(x.plate)).append("</td><td>").append(esc(x.route)).append("</td><td>").append(esc(x.cargo))
                    .append("</td><td class='n'>").append(m(x.income)).append("</td><td class='n'>").append(m(x.cost())).append("</td><td class='n ")
                    .append(x.profit() >= 0 ? "pos" : "neg").append("'>").append(m(x.profit())).append("</td></tr>");
        h.append("</table>");

        h.append("<h2>Spare parts</h2><table><tr><th>Date</th><th>Truck</th><th>Part</th><th>Qty</th><th>Cost</th></tr>");
        for (Part x : data.parts) if (in(x.date, ym))
            h.append("<tr><td>").append(x.date).append("</td><td>").append(esc(x.plate)).append("</td><td>").append(esc(x.name)).append("</td><td class='n'>")
                    .append(x.qty).append("</td><td class='n'>").append(m(x.cost)).append("</td></tr>");
        h.append("</table>");

        h.append("<h2>Miscellaneous expenses</h2><table><tr><th>Date</th><th>Category</th><th>Description</th><th>Truck</th><th>Amount</th></tr>");
        for (Misc x : data.misc) if (in(x.date, ym))
            h.append("<tr><td>").append(x.date).append("</td><td>").append(esc(x.category)).append("</td><td>").append(esc(x.description)).append("</td><td>")
                    .append(x.plate.isEmpty() ? "General" : esc(x.plate)).append("</td><td class='n'>").append(m(x.amount)).append("</td></tr>");
        h.append("</table>");

        h.append("<h2>Payroll</h2><table><tr><th>Name</th><th>Role</th><th>Monthly salary</th></tr>");
        for (Staff x : data.staff)
            h.append("<tr><td>").append(esc(x.name)).append("</td><td>").append(esc(x.role)).append("</td><td class='n'>").append(m(x.monthlySalary)).append("</td></tr>");
        h.append("</table></body></html>");
        return h.toString();
    }

    static String sumRow(String label, double v, boolean total) {
        String cls = total ? " class='tot'" : "";
        String vc = total ? (v >= 0 ? "n pos" : "n neg") : "n";
        return "<tr" + cls + "><td>" + label + "</td><td class='" + vc + "'>KSh " + m(v) + "</td></tr>";
    }

    static String reportCsv(YearMonth ym) {
        Totals t = totals(ym);
        StringBuilder s = new StringBuilder();
        s.append("NGUMBA LOGISTICS MONTHLY REPORT,").append(ym.format(MONTH_FMT)).append("\n\nSUMMARY\nItem,Amount (KSh)\n");
        s.append("Trip income,").append(plain(t.income)).append("\nTrip expenses,").append(plain(t.tripCost)).append("\nSpare parts,").append(plain(t.parts));
        s.append("\nMiscellaneous,").append(plain(t.misc)).append("\nStaff payroll,").append(plain(t.payroll)).append("\nNet profit/(loss),").append(plain(t.net())).append("\n");

        s.append("\nTRIPS\nID,Date,Truck,Route,Cargo,Income,Fuel,Border/Tolls,Other,Profit\n");
        for (Trip x : data.trips) if (in(x.date, ym))
            s.append(x.id).append(',').append(x.date).append(',').append(q(x.plate)).append(',').append(q(x.route)).append(',').append(q(x.cargo)).append(',')
                    .append(plain(x.income)).append(',').append(plain(x.fuel)).append(',').append(plain(x.borderAndTolls)).append(',').append(plain(x.other)).append(',').append(plain(x.profit())).append("\n");

        s.append("\nSPARE PARTS\nDate,Truck,Part,Qty,Cost\n");
        for (Part x : data.parts) if (in(x.date, ym))
            s.append(x.date).append(',').append(q(x.plate)).append(',').append(q(x.name)).append(',').append(x.qty).append(',').append(plain(x.cost)).append("\n");

        s.append("\nMISCELLANEOUS\nDate,Category,Description,Truck,Amount\n");
        for (Misc x : data.misc) if (in(x.date, ym))
            s.append(x.date).append(',').append(q(x.category)).append(',').append(q(x.description)).append(',').append(q(x.plate)).append(',').append(plain(x.amount)).append("\n");

        s.append("\nPAYROLL\nName,Role,Monthly salary\n");
        for (Staff x : data.staff) s.append(q(x.name)).append(',').append(q(x.role)).append(',').append(plain(x.monthlySalary)).append("\n");
        return s.toString();
    }

    static void exportHtml() {
        YearMonth ym = selectedMonth();
        File f = new File(reportsDir(), "Ngumba_Report_" + ym + ".html");
        if (writeFile(f, reportHtml(ym))) {
            openFile(f);
            JOptionPane.showMessageDialog(frame, "Report saved:\n" + f.getAbsolutePath()
                    + "\n\nIt opens in your browser. Press Ctrl+P and choose 'Save as PDF' to make a PDF.");
        }
    }

    static void exportCsv() {
        YearMonth ym = selectedMonth();
        File f = new File(reportsDir(), "Ngumba_Report_" + ym + ".csv");
        if (writeFile(f, reportCsv(ym))) {
            openFile(f);
            JOptionPane.showMessageDialog(frame, "CSV saved:\n" + f.getAbsolutePath());
        }
    }

    static File reportsDir() {
        File d = new File("reports");
        d.mkdirs();
        return d;
    }

    static boolean writeFile(File f, String content) {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8")) {
            w.write(content);
            return true;
        } catch (IOException e) {
            err("Could not save file: " + e.getMessage());
            return false;
        }
    }

    static void openFile(File f) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(f);
        } catch (Exception ignored) { }
    }

    // ============================ DASHBOARD / ALERTS ============================
    static String dashboardText() {
        YearMonth now = YearMonth.now();
        Totals t = totals(now);
        StringBuilder s = new StringBuilder();
        s.append("NGUMBA LOGISTICS - FLEET MANAGER\n\n");
        s.append("Trucks: ").append(data.trucks.size()).append("   Staff: ").append(data.staff.size())
                .append("   Trips recorded: ").append(data.trips.size()).append("\n\n");
        s.append("ALERTS (service / insurance within 30 days)\n").append(alertsText()).append("\n");
        s.append("THIS MONTH SO FAR - ").append(now.format(MONTH_FMT)).append("\n");
        s.append(String.format("  Income ............ KSh %,16.2f%n", t.income));
        s.append(String.format("  Trip expenses ..... KSh %,16.2f%n", t.tripCost));
        s.append(String.format("  Spare parts ....... KSh %,16.2f%n", t.parts));
        s.append(String.format("  Miscellaneous ..... KSh %,16.2f%n", t.misc));
        s.append(String.format("  Payroll ........... KSh %,16.2f%n", t.payroll));
        s.append(String.format("  NET ............... KSh %,16.2f%n", t.net()));
        return s.toString();
    }

    static String alertsText() {
        LocalDate today = LocalDate.now();
        StringBuilder s = new StringBuilder();
        for (Truck t : data.trucks) {
            long svc = ChronoUnit.DAYS.between(today, t.nextService);
            long ins = ChronoUnit.DAYS.between(today, t.insuranceExpiry);
            if (svc <= 30)
                s.append("  [!] ").append(t.plate).append(" service ").append(svc < 0 ? "OVERDUE by " + (-svc) + " days" : "due in " + svc + " days").append(" (").append(t.nextService).append(")\n");
            if (ins <= 30)
                s.append("  [!] ").append(t.plate).append(" insurance ").append(ins < 0 ? "EXPIRED " + (-ins) + " days ago" : "expires in " + ins + " days").append(" (").append(t.insuranceExpiry).append(")\n");
        }
        return s.length() == 0 ? "  None. All good.\n" : s.toString();
    }

    // ============================ HELPERS ============================
    static Truck findTruck(String plate) {
        for (Truck t : data.trucks) if (t.plate.equalsIgnoreCase(plate.trim())) return t;
        return null;
    }

    static JComboBox<String> truckCombo(boolean allowGeneral) {
        JComboBox<String> c = new JComboBox<String>();
        if (allowGeneral) c.addItem("(General / whole fleet)");
        for (Truck t : data.trucks) c.addItem(t.plate);
        return c;
    }

    static JTextField tf(String s) { return new JTextField(s, 20); }

    static boolean form(String title, String[] labels, JComponent[] fields) {
        JPanel p = new JPanel(new GridLayout(labels.length, 2, 8, 8));
        for (int i = 0; i < labels.length; i++) { p.add(new JLabel(labels[i])); p.add(fields[i]); }
        return JOptionPane.showConfirmDialog(frame, p, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    static int selected(JTable t) {
        int r = t.getSelectedRow();
        if (r < 0) err("Select a row in the table first.");
        return r;
    }

    static LocalDate date(String s) {
        try { return LocalDate.parse(s.trim()); }
        catch (Exception e) { err("Invalid date '" + s + "'. Use yyyy-MM-dd, e.g. 2026-10-15."); return null; }
    }

    static Double num(String s, String label) {
        try {
            double v = Double.parseDouble(s.trim().replace(",", ""));
            if (v < 0) throw new NumberFormatException();
            return v;
        } catch (Exception e) {
            err(label + " must be a valid number (0 or more).");
            return null;
        }
    }

    static void err(String msg) { JOptionPane.showMessageDialog(frame, msg, "Check your input", JOptionPane.WARNING_MESSAGE); }

    static String m(double v) { return String.format("%,.2f", v); }
    static String plain(double v) { return String.format(Locale.US, "%.2f", v); }
    static String q(String s) { return "\"" + (s == null ? "" : s.replace("\"", "\"\"")) + "\""; }
    static String esc(String s) { return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    static String cut(String s, int n) { return s == null ? "" : (s.length() > n ? s.substring(0, n - 1) + "." : s); }

    // ============================ STORAGE ============================
    static void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE))) {
            out.writeObject(data);
        } catch (IOException e) {
            err("Could not save data: " + e.getMessage());
        }
    }

    static void load() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            data = (Data) in.readObject();
        } catch (Exception e) {
            System.out.println("Could not load saved data, starting fresh: " + e.getMessage());
        }
    }
}