import org.sqlite.SQLiteDataSource;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;

class Main {
    public static final int PORT = 50001;
    public static void main(String[] args) throws IOException {
        //DB initialisation
        String url = "jdbc:sqlite:server.db";
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        //Creating server socket
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println();

        //Waiting for connection
        System.out.println("Listening... : " + serverSocket);
        Socket socket = serverSocket.accept();
        System.out.println("Connected to: " + socket);

        //Accessing server backend using socket and DB connection
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            Backend logic = new Backend(socket, con);
            logic.menu();
            //Closing connection
            //input
            socket.close();
            serverSocket.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }
}