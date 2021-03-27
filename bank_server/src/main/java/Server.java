import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.SQLException;

class Server {
    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Usage: java Server <port number>");
            System.exit(1);
        }

        int portNumber = Integer.parseInt(args[0]);
        boolean listening = true;
        String url = "jdbc:sqlite:cards.db";
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);
        try {
            Connection con = dataSource.getConnection();
            con.setAutoCommit(false);
            ServerSocket serverSocket = new ServerSocket(portNumber);
            System.out.println("Server is running...");
            while (listening) {
                new ServerThread(serverSocket.accept(), con).start();
            }
        } catch (IOException e) {
            System.err.println("Exception caught when trying to listen on port "
                    + portNumber + " or listening for a connection");
            System.err.println(e.getMessage());
            System.exit(-1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

