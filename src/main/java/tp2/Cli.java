package tp2;

import tp2.bo.BranchOfficeApi;
import tp2.ho.HeadOfficeApi;

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
            default -> {
                System.err.println("Unknown command: " + args[0]);
                printHelp();
                System.exit(1);
            }
        }
    }

    private static void handleBo(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Missing BO number");
            System.exit(1);
        }
        int bo = Integer.parseInt(args[1]);
        if (bo != 1 && bo != 2) {
            System.err.println("BO must be 1 or 2");
            System.exit(1);
        }
        if (args.length < 3) {
            System.err.println("Missing subcommand");
            System.exit(1);
        }

        BranchOfficeApi api = new BranchOfficeApi(bo);
        switch (args[2]) {
            case "sales" -> handleBoSales(api, args);
            case "events" -> handleBoEvents(api, args);
            default -> {
                System.err.println("Unknown bo subcommand: " + args[2]);
                System.exit(1);
            }
        }
    }

    private static void handleBoSales(BranchOfficeApi api, String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Missing sales subcommand");
            System.exit(1);
        }
        switch (args[3]) {
            case "list" -> {
                int limit = (args.length > 4 && "--limit".equals(args[4])) ? Integer.parseInt(args[5]) : 10;
                System.out.print(api.listSalesToCsv(limit));
            }
            case "get" -> {
                if (args.length < 5) {
                    System.err.println("Missing sale_id");
                    System.exit(1);
                }
                System.out.print(api.getSaleToCsv(UUID.fromString(args[4])));
            }
            case "write" -> {
                if (args.length < 12) {
                    System.err.println("Need: write <date> <region> <product> <qty> <cost> <amt> <tax> <total>");
                    System.exit(1);
                }
                var sale = tp2.Util.saleFromArgs(args, 4);
                var result = api.writeSale(sale);
                System.out.println("sale_id=" + result.saleId() + ",event_id=" + result.eventId());
            }
            case "del" -> {
                if (args.length < 5) {
                    System.err.println("Missing sale_id");
                    System.exit(1);
                }
                api.deleteSale(UUID.fromString(args[4]));
                System.out.println("DELETE event enqueued");
            }
            default -> {
                System.err.println("Unknown sales subcommand");
                System.exit(1);
            }
        }
    }

    private static void handleBoEvents(BranchOfficeApi api, String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Missing events subcommand");
            System.exit(1);
        }
        switch (args[3]) {
            case "list" -> {
                int limit = (args.length > 4 && "--limit".equals(args[4])) ? Integer.parseInt(args[5]) : 10;
                System.out.print(api.listEventsToCsv(limit));
            }
            case "get" -> {
                if (args.length < 5) {
                    System.err.println("Missing event_id");
                    System.exit(1);
                }
                System.out.print(api.getEventToCsv(UUID.fromString(args[4])));
            }
            default -> {
                System.err.println("Unknown events subcommand");
                System.exit(1);
            }
        }
    }

    private static void handleHo(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Missing ho subcommand");
            System.exit(1);
        }
        HeadOfficeApi api = new HeadOfficeApi();
        switch (args[1]) {
            case "sales" -> handleHoSales(api, args);
            case "events" -> handleHoEvents(api, args);
            case "syncs" -> handleHoSyncs(api, args);
            default -> {
                System.err.println("Unknown ho subcommand");
                System.exit(1);
            }
        }
    }

    private static void handleHoSales(HeadOfficeApi api, String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Missing sales subcommand");
            System.exit(1);
        }
        switch (args[2]) {
            case "list" -> {
                int limit = (args.length > 3 && "--limit".equals(args[3])) ? Integer.parseInt(args[4]) : 10;
                System.out.print(api.listSalesToCsv(limit));
            }
            case "get" -> {
                if (args.length < 4) {
                    System.err.println("Missing sale_id");
                    System.exit(1);
                }
                System.out.print(api.getSaleToCsv(UUID.fromString(args[3])));
            }
            default -> {
                System.err.println("Unknown sales subcommand");
                System.exit(1);
            }
        }
    }

    private static void handleHoEvents(HeadOfficeApi api, String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Missing events subcommand");
            System.exit(1);
        }
        switch (args[2]) {
            case "list" -> {
                int limit = (args.length > 3 && "--limit".equals(args[3])) ? Integer.parseInt(args[4]) : 10;
                System.out.print(api.listEventsToCsv(limit));
            }
            case "get" -> {
                if (args.length < 4) {
                    System.err.println("Missing event_id");
                    System.exit(1);
                }
                System.out.print(api.getEventToCsv(UUID.fromString(args[3])));
            }
            default -> {
                System.err.println("Unknown events subcommand");
                System.exit(1);
            }
        }
    }

    private static void handleHoSyncs(HeadOfficeApi api, String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Missing syncs subcommand");
            System.exit(1);
        }
        switch (args[2]) {
            case "list" -> {
                int limit = (args.length > 3 && "--limit".equals(args[3])) ? Integer.parseInt(args[4]) : 10;
                System.out.print(api.listSyncsToCsv(limit));
            }
            case "get" -> {
                if (args.length < 4) {
                    System.err.println("Missing sale_id");
                    System.exit(1);
                }
                System.out.print(api.getSyncToCsv(UUID.fromString(args[3])));
            }
            default -> {
                System.err.println("Unknown syncs subcommand");
                System.exit(1);
            }
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
        System.out.println("  ho sales list [--limit N]");
        System.out.println("  ho sales get <sale_id>");
        System.out.println("  ho events list [--limit N]");
        System.out.println("  ho events get <event_id>");
        System.out.println("  ho syncs list [--limit N]");
        System.out.println("  ho syncs get <sale_id>");
    }
}
