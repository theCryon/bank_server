import org.sqlite.SQLiteDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        Operation logic = new Operation();
        if (true) {
            String url = "jdbc:sqlite:" + "server.db";
            SQLiteDataSource dataSource = new SQLiteDataSource();
            dataSource.setUrl(url);
            try (Connection con = dataSource.getConnection()) {
                con.setAutoCommit(false);
                logic.menu(con);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No database file specified.");
        }
    }
}
