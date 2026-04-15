package tp2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    public static Connection getBoConnection(int bo) throws SQLException {
        return DriverManager.getConnection(
            Config.boDbUrl(bo),
            Config.boDbUser(bo),
            Config.boDbPassword(bo)
        );
    }

    public static Connection getHoConnection() throws SQLException {
        return DriverManager.getConnection(
            Config.hoDbUrl(),
            Config.hoDbUser(),
            Config.hoDbPassword()
        );
    }
}
