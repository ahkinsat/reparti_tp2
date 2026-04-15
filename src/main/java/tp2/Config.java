package tp2;

public class Config {
    // RabbitMQ
    public static String rmqHost() { return env("RABBITMQ_HOST", "localhost"); }
    public static int rmqPort() { return Integer.parseInt(env("RABBITMQ_PORT", "5672")); }
    public static String rmqUser() { return env("RABBITMQ_USER", "guest"); }
    public static String rmqPassword() { return env("RABBITMQ_PASSWORD", "guest"); }
    public static String rmqQueue() { return "sync_queue"; }

    // HO Database
    public static String hoDbUrl() {
        return "jdbc:postgresql://" + env("HO_DB_HOST", "localhost") + ":" +
               env("HO_DB_PORT", "5432") + "/" + env("HO_DB_NAME", "ho_sales");
    }
    public static String hoDbUser() { return env("HO_DB_USER", "ho_user"); }
    public static String hoDbPassword() { return env("HO_DB_PASSWORD", "ho_pass"); }

    // BO Database (bo = 1 or 2)
    public static String boDbUrl(int bo) {
        return "jdbc:postgresql://" + env("BO"+bo+"_DB_HOST", "localhost") + ":" +
               env("BO"+bo+"_DB_PORT", "5432") + "/" + env("BO"+bo+"_DB_NAME", "bo"+bo+"_sales");
    }
    public static String boDbUser(int bo) { return env("BO"+bo+"_DB_USER", "bo"+bo+"_user"); }
    public static String boDbPassword(int bo) { return env("BO"+bo+"_DB_PASSWORD", "bo"+bo+"_pass"); }

    private static String env(String key, String defaultValue) {
        String val = System.getenv(key);
        return val != null ? val : defaultValue;
    }

    public static void printConfig() {
        System.out.println("RabbitMQ: " + rmqHost() + ":" + rmqPort());
        System.out.println("HO DB: " + hoDbUrl());
        System.out.println("BO1 DB: " + boDbUrl(1));
        System.out.println("BO2 DB: " + boDbUrl(2));
    }
}
