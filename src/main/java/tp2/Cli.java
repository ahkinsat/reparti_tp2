package tp2;

import tp2.bo.BranchOfficeApi;
import tp2.bo.OutboxRecord;
import tp2.ho.HeadOfficeApi;
import tp2.models.Sale;

import java.util.UUID;

public class Cli {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printHelp();
            System.exit(1);
        }

        switch (args[0]) {
            case "init" -> Init.run();
            case "config" -> Config.printConfig();
            case "help" -> printHelp();
            case "bo" -> handleBo(args);
            case "ho" -> handleHo(args);
            case "service" -> handleGlobalService(args);
            default -> { System.err.println("Unknown command: " + args[0]); printHelp(); System.exit(1); }
        }
    }

    private static void handleBo(String[] args) throws Exception {
        if (args.length < 2) { System.err.println("Missing BO number"); System.exit(1); }
        int bo = Integer.parseInt(args[1]);
        if (bo != 1 && bo != 2) { System.err.println("BO must be 1 or 2"); System.exit(1); }
        if (args.length < 3) { System.err.println("Missing subcommand"); System.exit(1); }

        BranchOfficeApi api = new BranchOfficeApi(bo);
        switch (args[2]) {
            case "sales" -> handleBoSales(api, args);
            case "events" -> handleBoEvents(api, args);
            case "service" -> handleBoService(bo, args);
            default -> { System.err.println("Unknown bo subcommand: " + args[2]); System.exit(1); }
        }
    }

    private static void handleBoSales(BranchOfficeApi api, String[] args) throws Exception {
        if (args.length < 4) { System.err.println("Missing sales subcommand"); System.exit(1); }
        switch (args[3]) {
            case "list" -> {
                int limit = (args.length > 4 && "--limit".equals(args[4])) ? Integer.parseInt(args[5]) : 10;
                var sales = api.listSales(limit);
                // Convert to CSV manually (simple)
                System.out.println("sale_id,sale_date,region,product,quantity,cost,amount,tax,total");
                for (Sale s : sales) {
                    System.out.printf("%s,%s,%s,%s,%d,%.2f,%.2f,%.2f,%.2f%n",
                        s.saleId(), s.saleDate(), s.region(), s.product(), s.quantity(),
                        s.cost(), s.amount(), s.tax(), s.total());
                }
            }
            case "get" -> {
                if (args.length < 5) { System.err.println("Missing sale_id"); System.exit(1); }
                var sale = api.getSale(UUID.fromString(args[4]));
                if (sale.isPresent()) {
                    Sale s = sale.get();
                    System.out.printf("%s,%s,%s,%s,%d,%.2f,%.2f,%.2f,%.2f%n",
                        s.saleId(), s.saleDate(), s.region(), s.product(), s.quantity(),
                        s.cost(), s.amount(), s.tax(), s.total());
                } else {
                    System.out.println("NOT FOUND");
                }
            }
            case "write" -> {
                if (args.length < 12) { System.err.println("Need: write <date> <region> <product> <qty> <cost> <amt> <tax> <total>"); System.exit(1); }
                Sale sale = tp2.Util.saleFromArgs(args, 4);
                var result = api.writeSale(sale);
                System.out.println("sale_id=" + result.saleId() + ",event_id=" + result.eventId());
            }
            case "del" -> {
                if (args.length < 5) { System.err.println("Missing sale_id"); System.exit(1); }
                api.deleteSale(UUID.fromString(args[4]));
                System.out.println("DELETE event enqueued");
            }
            default -> { System.err.println("Unknown sales subcommand"); System.exit(1); }
        }
    }

    private static void handleBoEvents(BranchOfficeApi api, String[] args) throws Exception {
        if (args.length < 4) { System.err.println("Missing events subcommand"); System.exit(1); }
        switch (args[3]) {
            case "list" -> {
                int limit = (args.length > 4 && "--limit".equals(args[4])) ? Integer.parseInt(args[5]) : 10;
                var events = api.listEvents(limit);
                System.out.println("event_id,sale_id,event_type,event_time,payload,retry_count");
                for (OutboxRecord e : events) {
                    System.out.printf("%s,%s,%s,%s,%s,%d%n",
                        e.eventId(), e.saleId(), e.eventType(), e.eventTime(), e.payload(), e.retryCount());
                }
            }
            case "get" -> {
                if (args.length < 5) { System.err.println("Missing event_id"); System.exit(1); }
                var ev = api.getEvent(UUID.fromString(args[4]));
                if (ev != null) {
                    System.out.printf("%s,%s,%s,%s,%s,%d%n",
                        ev.eventId(), ev.saleId(), ev.eventType(), ev.eventTime(), ev.payload(), ev.retryCount());
                } else {
                    System.out.println("NOT FOUND");
                }
            }
            default -> { System.err.println("Unknown events subcommand"); System.exit(1); }
        }
    }

    private static void handleBoService(int bo, String[] args) {
        if (args.length < 4) { System.err.println("Missing service command (start|stop|status)"); System.exit(1); }
        String cmd = args[3];
        switch (cmd) {
            case "start" -> { ServiceManager.startBoService(bo); System.out.println("BO"+bo+" outbox processor started"); }
            case "stop" -> { ServiceManager.stopBoService(bo); System.out.println("BO"+bo+" outbox processor stopped"); }
            case "status" -> {
                boolean running = ServiceManager.isBoServiceRunning(bo);
                System.out.println(running ? "running" : "stopped");
                System.exit(running ? 0 : 1);
            }
            default -> { System.err.println("Unknown service command"); System.exit(1); }
        }
    }

    private static void handleHo(String[] args) throws Exception {
        if (args.length < 2) { System.err.println("Missing ho subcommand"); System.exit(1); }
        HeadOfficeApi api = new HeadOfficeApi();
        switch (args[1]) {
            case "sales" -> handleHoSales(api, args);
            case "events" -> handleHoEvents(api, args);
            case "syncs" -> handleHoSyncs(api, args);
            case "service" -> handleHoService(args);
            default -> { System.err.println("Unknown ho subcommand"); System.exit(1); }
        }
    }

    private static void handleHoSales(HeadOfficeApi api, String[] args) throws Exception {
        if (args.length < 3) { System.err.println("Missing sales subcommand"); System.exit(1); }
        switch (args[2]) {
            case "list" -> {
                int limit = (args.length > 3 && "--limit".equals(args[3])) ? Integer.parseInt(args[4]) : 10;
                var sales = api.listSales(limit);
                System.out.println("sale_id,sale_date,region,product,quantity,cost,amount,tax,total");
                for (var s : sales) {
                    System.out.printf("%s,%s,%s,%s,%d,%.2f,%.2f,%.2f,%.2f%n",
                        s.saleId(), s.saleDate(), s.region(), s.product(), s.quantity(),
                        s.cost(), s.amount(), s.tax(), s.total());
                }
            }
            case "get" -> {
                if (args.length < 4) { System.err.println("Missing sale_id"); System.exit(1); }
                var sale = api.getSale(UUID.fromString(args[3]));
                if (sale.isPresent()) {
                    var s = sale.get();
                    System.out.printf("%s,%s,%s,%s,%d,%.2f,%.2f,%.2f,%.2f%n",
                        s.saleId(), s.saleDate(), s.region(), s.product(), s.quantity(),
                        s.cost(), s.amount(), s.tax(), s.total());
                } else {
                    System.out.println("NOT FOUND");
                }
            }
            default -> { System.err.println("Unknown sales subcommand"); System.exit(1); }
        }
    }

    private static void handleHoEvents(HeadOfficeApi api, String[] args) throws Exception {
        if (args.length < 3) { System.err.println("Missing events subcommand"); System.exit(1); }
        switch (args[2]) {
            case "list" -> {
                int limit = (args.length > 3 && "--limit".equals(args[3])) ? Integer.parseInt(args[4]) : 10;
                var events = api.listEvents(limit);
                System.out.println("event_id,sale_id,event_type,event_time,applied_at");
                for (var e : events) {
                    System.out.printf("%s,%s,%s,%s,%s%n",
                        e.eventId(), e.saleId(), e.eventType(), e.eventTime(), e.appliedAt());
                }
            }
            case "get" -> {
                if (args.length < 4) { System.err.println("Missing event_id"); System.exit(1); }
                var ev = api.getEvent(UUID.fromString(args[3]));
                if (ev.isPresent()) {
                    var e = ev.get();
                    System.out.printf("%s,%s,%s,%s,%s%n",
                        e.eventId(), e.saleId(), e.eventType(), e.eventTime(), e.appliedAt());
                } else {
                    System.out.println("NOT FOUND");
                }
            }
            default -> { System.err.println("Unknown events subcommand"); System.exit(1); }
        }
    }

    private static void handleHoSyncs(HeadOfficeApi api, String[] args) throws Exception {
        if (args.length < 3) { System.err.println("Missing syncs subcommand"); System.exit(1); }
        switch (args[2]) {
            case "list" -> {
                int limit = (args.length > 3 && "--limit".equals(args[3])) ? Integer.parseInt(args[4]) : 10;
                var syncs = api.listSyncs(limit);
                System.out.println("sale_id,last_event_time,last_event_id");
                for (var s : syncs) {
                    System.out.printf("%s,%s,%s%n", s.saleId(), s.lastEventTime(), s.lastEventId());
                }
            }
            case "get" -> {
                if (args.length < 4) { System.err.println("Missing sale_id"); System.exit(1); }
                var sync = api.getSync(UUID.fromString(args[3]));
                if (sync.isPresent()) {
                    var s = sync.get();
                    System.out.printf("%s,%s,%s%n", s.saleId(), s.lastEventTime(), s.lastEventId());
                } else {
                    System.out.println("NOT FOUND");
                }
            }
            default -> { System.err.println("Unknown syncs subcommand"); System.exit(1); }
        }
    }

    private static void handleHoService(String[] args) {
        if (args.length < 3) { System.err.println("Missing service command (start|stop|status)"); System.exit(1); }
        String cmd = args[2];
        switch (cmd) {
            case "start" -> { ServiceManager.startHoService(); System.out.println("HO event consumer started"); }
            case "stop" -> { ServiceManager.stopHoService(); System.out.println("HO event consumer stopped"); }
            case "status" -> {
                boolean running = ServiceManager.isHoServiceRunning();
                System.out.println(running ? "running" : "stopped");
                System.exit(running ? 0 : 1);
            }
            default -> { System.err.println("Unknown service command"); System.exit(1); }
        }
    }

    private static void handleGlobalService(String[] args) {
        if (args.length < 2) { System.err.println("Missing service command (start|stop|status)"); System.exit(1); }
        String cmd = args[1];
        switch (cmd) {
            case "start" -> {
                ServiceManager.startBoService(1);
                ServiceManager.startBoService(2);
                ServiceManager.startHoService();
                System.out.println("All services started");
            }
            case "stop" -> {
                ServiceManager.stopBoService(1);
                ServiceManager.stopBoService(2);
                ServiceManager.stopHoService();
                System.out.println("All services stopped");
            }
            case "status" -> {
                boolean b1 = ServiceManager.isBoServiceRunning(1);
                boolean b2 = ServiceManager.isBoServiceRunning(2);
                boolean ho = ServiceManager.isHoServiceRunning();
                System.out.println("BO1: " + (b1 ? "running" : "stopped"));
                System.out.println("BO2: " + (b2 ? "running" : "stopped"));
                System.out.println("HO : " + (ho ? "running" : "stopped"));
                System.exit((b1 && b2 && ho) ? 0 : 1);
            }
            default -> { System.err.println("Unknown service command"); System.exit(1); }
        }
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  init");
        System.out.println("  config");
        System.out.println("  help");
        System.out.println("  bo <1|2> sales list [--limit N]");
        System.out.println("  bo <1|2> sales get <sale_id>");
        System.out.println("  bo <1|2> sales write <date> <region> <product> <qty> <cost> <amt> <tax> <total>");
        System.out.println("  bo <1|2> sales del <sale_id>");
        System.out.println("  bo <1|2> events list [--limit N]");
        System.out.println("  bo <1|2> events get <event_id>");
        System.out.println("  bo <1|2> service start|stop|status");
        System.out.println("  ho sales list [--limit N]");
        System.out.println("  ho sales get <sale_id>");
        System.out.println("  ho events list [--limit N]");
        System.out.println("  ho events get <event_id>");
        System.out.println("  ho syncs list [--limit N]");
        System.out.println("  ho syncs get <sale_id>");
        System.out.println("  ho service start|stop|status");
        System.out.println("  service start|stop|status");
    }
}
