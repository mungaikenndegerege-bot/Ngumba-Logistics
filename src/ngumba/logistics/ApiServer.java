package ngumba.logistics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.json.JavalinJackson;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import ngumba.logistics.DataStore.*;

/**
 * NGUMBA LOGISTICS - REST API Server
 *
 * Starts an embedded Javalin/Jetty HTTP server on port 7070.
 * Serves the web frontend from web/ and exposes a JSON REST API at /api/*.
 *
 * Run: java -cp "dist/NgumbaWeb.jar;lib/*" ngumba.logistics.ApiServer
 * Then open: http://localhost:7070
 */
public class ApiServer {

    static final ObjectMapper JSON = new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .findAndRegisterModules();

    public static void main(String[] args) {
        DataStore.load();

        Javalin app = Javalin.create(config -> {
            config.addStaticFiles("/web", Location.CLASSPATH);
            config.enableCorsForAllOrigins();
            config.jsonMapper(new JavalinJackson(JSON));
        }).start(7070);

        // ---- TRUCKS ----
        app.get("/api/trucks", ctx -> {
            ArrayNode arr = JSON.createArrayNode();
            for (Truck t : DataStore.get().trucks) {
                ObjectNode n = JSON.createObjectNode();
                n.put("plate", t.plate);
                n.put("model", t.model);
                n.put("driver", t.driver);
                n.put("nextService", t.nextService.toString());
                n.put("insuranceExpiry", t.insuranceExpiry.toString());
                long svcDays = ChronoUnit.DAYS.between(LocalDate.now(), t.nextService);
                long insDays = ChronoUnit.DAYS.between(LocalDate.now(), t.insuranceExpiry);
                n.put("serviceDaysLeft", svcDays);
                n.put("insuranceDaysLeft", insDays);
                arr.add(n);
            }
            ctx.json(arr.toString());
        });

        app.post("/api/trucks", ctx -> {
            ObjectNode body = ctx.bodyAsClass(ObjectNode.class);
            String plate = body.get("plate").asText().trim().toUpperCase();
            if (plate.isEmpty()) { ctx.status(400).result("Plate is required"); return; }
            if (DataStore.findTruck(plate) != null) { ctx.status(409).result("Truck already exists"); return; }
            Truck t = new Truck();
            t.plate = plate;
            t.model = body.get("model").asText("HOWO T7H 440").trim();
            t.driver = body.get("driver").asText("").trim();
            t.nextService = LocalDate.parse(body.get("nextService").asText());
            t.insuranceExpiry = LocalDate.parse(body.get("insuranceExpiry").asText());
            DataStore.get().trucks.add(t);
            DataStore.save();
            ctx.status(201).result("Created");
        });

        app.put("/api/trucks/{plate}", ctx -> {
            Truck t = DataStore.findTruck(ctx.pathParam("plate"));
            if (t == null) { ctx.status(404).result("Not found"); return; }
            ObjectNode body = ctx.bodyAsClass(ObjectNode.class);
            if (body.has("model"))          t.model = body.get("model").asText().trim();
            if (body.has("driver"))         t.driver = body.get("driver").asText().trim();
            if (body.has("nextService"))    t.nextService = LocalDate.parse(body.get("nextService").asText());
            if (body.has("insuranceExpiry")) t.insuranceExpiry = LocalDate.parse(body.get("insuranceExpiry").asText());
            DataStore.save();
            ctx.result("Updated");
        });

        app.delete("/api/trucks/{plate}", ctx -> {
            String plate = ctx.pathParam("plate");
            boolean removed = DataStore.get().trucks.removeIf(t -> t.plate.equalsIgnoreCase(plate));
            if (!removed) { ctx.status(404).result("Not found"); return; }
            DataStore.save();
            ctx.result("Deleted");
        });

        // ---- TRIPS ----
        app.get("/api/trips", ctx -> {
            ArrayNode arr = JSON.createArrayNode();
            for (Trip t : DataStore.get().trips) {
                ObjectNode n = JSON.createObjectNode();
                n.put("id", t.id);
                n.put("plate", t.plate);
                n.put("route", t.route);
                n.put("cargo", t.cargo);
                n.put("date", t.date.toString());
                n.put("income", t.income);
                n.put("fuel", t.fuel);
                n.put("borderAndTolls", t.borderAndTolls);
                n.put("other", t.other);
                n.put("cost", t.cost());
                n.put("profit", t.profit());
                arr.add(n);
            }
            ctx.json(arr.toString());
        });

        app.post("/api/trips", ctx -> {
            ObjectNode body = ctx.bodyAsClass(ObjectNode.class);
            Trip t = new Trip();
            t.id = DataStore.get().nextTripId++;
            t.plate = body.get("plate").asText().trim().toUpperCase();
            t.route = body.get("route").asText().trim();
            t.cargo = body.get("cargo").asText("").trim();
            t.date = LocalDate.parse(body.get("date").asText());
            t.income = body.get("income").asDouble(0);
            t.fuel = body.get("fuel").asDouble(0);
            t.borderAndTolls = body.get("borderAndTolls").asDouble(0);
            t.other = body.get("other").asDouble(0);
            DataStore.get().trips.add(t);
            DataStore.save();
            ctx.status(201).result("" + t.id);
        });

        app.delete("/api/trips/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            boolean removed = DataStore.get().trips.removeIf(t -> t.id == id);
            if (!removed) { ctx.status(404).result("Not found"); return; }
            DataStore.save();
            ctx.result("Deleted");
        });

        // ---- SPARE PARTS ----
        app.get("/api/parts", ctx -> {
            ArrayNode arr = JSON.createArrayNode();
            for (Part p : DataStore.get().parts) {
                ObjectNode n = JSON.createObjectNode();
                n.put("id", p.id);
                n.put("date", p.date.toString());
                n.put("plate", p.plate);
                n.put("name", p.name);
                n.put("qty", p.qty);
                n.put("cost", p.cost);
                arr.add(n);
            }
            ctx.json(arr.toString());
        });

        app.post("/api/parts", ctx -> {
            ObjectNode body = ctx.bodyAsClass(ObjectNode.class);
            Part p = new Part();
            p.id = DataStore.get().nextPartId++;
            p.plate = body.get("plate").asText().trim().toUpperCase();
            p.name = body.get("name").asText().trim();
            p.qty = body.get("qty").asInt(1);
            p.cost = body.get("cost").asDouble(0);
            p.date = LocalDate.parse(body.get("date").asText());
            DataStore.get().parts.add(p);
            DataStore.save();
            ctx.status(201).result("" + p.id);
        });

        app.delete("/api/parts/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            boolean removed = DataStore.get().parts.removeIf(p -> p.id == id);
            if (!removed) { ctx.status(404).result("Not found"); return; }
            DataStore.save();
            ctx.result("Deleted");
        });

        // ---- STAFF ----
        app.get("/api/staff", ctx -> {
            ArrayNode arr = JSON.createArrayNode();
            for (Staff s : DataStore.get().staff) {
                ObjectNode n = JSON.createObjectNode();
                n.put("id", s.id);
                n.put("name", s.name);
                n.put("role", s.role);
                n.put("monthlySalary", s.monthlySalary);
                arr.add(n);
            }
            ctx.json(arr.toString());
        });

        app.post("/api/staff", ctx -> {
            ObjectNode body = ctx.bodyAsClass(ObjectNode.class);
            Staff s = new Staff();
            s.id = DataStore.get().nextStaffId++;
            s.name = body.get("name").asText().trim();
            s.role = body.get("role").asText("").trim();
            s.monthlySalary = body.get("monthlySalary").asDouble(0);
            DataStore.get().staff.add(s);
            DataStore.save();
            ctx.status(201).result("" + s.id);
        });

        app.delete("/api/staff/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            boolean removed = DataStore.get().staff.removeIf(s -> s.id == id);
            if (!removed) { ctx.status(404).result("Not found"); return; }
            DataStore.save();
            ctx.result("Deleted");
        });

        // ---- MISC EXPENSES ----
        app.get("/api/misc", ctx -> {
            ArrayNode arr = JSON.createArrayNode();
            for (Misc x : DataStore.get().misc) {
                ObjectNode n = JSON.createObjectNode();
                n.put("id", x.id);
                n.put("date", x.date.toString());
                n.put("category", x.category);
                n.put("description", x.description);
                n.put("plate", x.plate == null ? "" : x.plate);
                n.put("amount", x.amount);
                arr.add(n);
            }
            ctx.json(arr.toString());
        });

        app.post("/api/misc", ctx -> {
            ObjectNode body = ctx.bodyAsClass(ObjectNode.class);
            Misc x = new Misc();
            x.id = DataStore.get().nextMiscId++;
            x.date = LocalDate.parse(body.get("date").asText());
            x.category = body.get("category").asText("Other").trim();
            x.description = body.get("description").asText("").trim();
            x.plate = body.has("plate") ? body.get("plate").asText("").trim() : "";
            x.amount = body.get("amount").asDouble(0);
            DataStore.get().misc.add(x);
            DataStore.save();
            ctx.status(201).result("" + x.id);
        });

        app.delete("/api/misc/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            boolean removed = DataStore.get().misc.removeIf(x -> x.id == id);
            if (!removed) { ctx.status(404).result("Not found"); return; }
            DataStore.save();
            ctx.result("Deleted");
        });

        // ---- DASHBOARD ----
        app.get("/api/dashboard", ctx -> {
            YearMonth now = YearMonth.now();
            AppData d = DataStore.get();
            LocalDate today = LocalDate.now();

            // This-month totals
            double income = 0, tripCost = 0, parts = 0, misc = 0, payroll = 0;
            for (Trip t : d.trips) if (inMonth(t.date, now)) { income += t.income; tripCost += t.cost(); }
            for (Part p : d.parts) if (inMonth(p.date, now)) parts += p.cost;
            for (Misc x : d.misc)  if (inMonth(x.date, now)) misc += x.amount;
            for (Staff s : d.staff) payroll += s.monthlySalary;

            // Alerts
            ArrayNode alerts = JSON.createArrayNode();
            for (Truck t : d.trucks) {
                long svc = ChronoUnit.DAYS.between(today, t.nextService);
                long ins = ChronoUnit.DAYS.between(today, t.insuranceExpiry);
                if (svc <= 30) {
                    ObjectNode a = JSON.createObjectNode();
                    a.put("plate", t.plate);
                    a.put("type", "service");
                    a.put("days", svc);
                    a.put("date", t.nextService.toString());
                    alerts.add(a);
                }
                if (ins <= 30) {
                    ObjectNode a = JSON.createObjectNode();
                    a.put("plate", t.plate);
                    a.put("type", "insurance");
                    a.put("days", ins);
                    a.put("date", t.insuranceExpiry.toString());
                    alerts.add(a);
                }
            }

            ObjectNode result = JSON.createObjectNode();
            result.put("truckCount", d.trucks.size());
            result.put("staffCount", d.staff.size());
            result.put("tripCount", d.trips.size());
            result.put("monthIncome", income);
            result.put("monthTripCost", tripCost);
            result.put("monthParts", parts);
            result.put("monthMisc", misc);
            result.put("monthPayroll", payroll);
            result.put("monthNet", income - tripCost - parts - misc - payroll);
            result.put("currentMonth", now.toString());
            result.set("alerts", alerts);
            ctx.json(result.toString());
        });

        // ---- MONTHLY REPORT ----
        app.get("/api/report", ctx -> {
            String yearParam  = ctx.queryParam("year");
            String monthParam = ctx.queryParam("month");
            int year  = (yearParam  != null && !yearParam.isEmpty())  ? Integer.parseInt(yearParam)  : LocalDate.now().getYear();
            int month = (monthParam != null && !monthParam.isEmpty()) ? Integer.parseInt(monthParam) : LocalDate.now().getMonthValue();
            YearMonth ym = YearMonth.of(year, month);
            AppData d = DataStore.get();

            double income = 0, tripCost = 0, partsTotal = 0, miscTotal = 0, payroll = 0;
            for (Trip t : d.trips) if (inMonth(t.date, ym)) { income += t.income; tripCost += t.cost(); }
            for (Part p : d.parts) if (inMonth(p.date, ym)) partsTotal += p.cost;
            for (Misc x : d.misc)  if (inMonth(x.date, ym)) miscTotal += x.amount;
            for (Staff s : d.staff) payroll += s.monthlySalary;

            // Per-truck breakdown
            ArrayNode truckRows = JSON.createArrayNode();
            for (Truck tr : d.trucks) {
                double inc = 0, cost = 0; int n = 0;
                for (Trip t : d.trips) if (t.plate.equals(tr.plate) && inMonth(t.date, ym)) { inc += t.income; cost += t.cost(); n++; }
                for (Part p : d.parts) if (p.plate.equals(tr.plate) && inMonth(p.date, ym)) cost += p.cost;
                for (Misc x : d.misc)  if (tr.plate.equals(x.plate) && inMonth(x.date, ym)) cost += x.amount;
                ObjectNode row = JSON.createObjectNode();
                row.put("plate", tr.plate); row.put("trips", n);
                row.put("income", inc); row.put("cost", cost); row.put("profit", inc - cost);
                truckRows.add(row);
            }

            // Trip detail
            ArrayNode tripRows = JSON.createArrayNode();
            for (Trip t : d.trips) if (inMonth(t.date, ym)) {
                ObjectNode row = JSON.createObjectNode();
                row.put("id", t.id); row.put("date", t.date.toString());
                row.put("plate", t.plate); row.put("route", t.route);
                row.put("cargo", t.cargo); row.put("income", t.income);
                row.put("cost", t.cost()); row.put("profit", t.profit());
                tripRows.add(row);
            }

            // Parts detail
            ArrayNode partRows = JSON.createArrayNode();
            for (Part p : d.parts) if (inMonth(p.date, ym)) {
                ObjectNode row = JSON.createObjectNode();
                row.put("id", p.id); row.put("date", p.date.toString());
                row.put("plate", p.plate); row.put("name", p.name);
                row.put("qty", p.qty); row.put("cost", p.cost);
                partRows.add(row);
            }

            // Misc detail
            ArrayNode miscRows = JSON.createArrayNode();
            for (Misc x : d.misc) if (inMonth(x.date, ym)) {
                ObjectNode row = JSON.createObjectNode();
                row.put("id", x.id); row.put("date", x.date.toString());
                row.put("category", x.category); row.put("description", x.description);
                row.put("plate", x.plate == null ? "" : x.plate); row.put("amount", x.amount);
                miscRows.add(row);
            }

            // Staff / payroll
            ArrayNode staffRows = JSON.createArrayNode();
            for (Staff s : d.staff) {
                ObjectNode row = JSON.createObjectNode();
                row.put("id", s.id); row.put("name", s.name);
                row.put("role", s.role); row.put("monthlySalary", s.monthlySalary);
                staffRows.add(row);
            }

            ObjectNode result = JSON.createObjectNode();
            result.put("year", year); result.put("month", month);
            result.put("income", income); result.put("tripCost", tripCost);
            result.put("parts", partsTotal); result.put("misc", miscTotal);
            result.put("payroll", payroll);
            result.put("net", income - tripCost - partsTotal - miscTotal - payroll);
            result.set("trucks", truckRows);
            result.set("trips", tripRows);
            result.set("parts", partRows);
            result.set("misc", miscRows);
            result.set("staff", staffRows);
            ctx.json(result.toString());
        });

        // ---- CONSTANTS (for dropdowns) ----
        app.get("/api/constants", ctx -> {
            ObjectNode result = JSON.createObjectNode();
            ArrayNode routes = JSON.createArrayNode();
            for (String r : DataStore.ROUTES) routes.add(r);
            ArrayNode cats = JSON.createArrayNode();
            for (String c : DataStore.MISC_CATEGORIES) cats.add(c);
            result.set("routes", routes);
            result.set("miscCategories", cats);
            ctx.json(result.toString());
        });

        System.out.println("===========================================");
        System.out.println("  Ngumba Logistics Web App running on:    ");
        System.out.println("  http://localhost:7070                    ");
        System.out.println("===========================================");
    }

    static boolean inMonth(LocalDate d, YearMonth ym) {
        return YearMonth.from(d).equals(ym);
    }
}
