import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;

public class ServerThread extends Thread{
    private final Socket socket;
    private final Connection con;

    public ServerThread(Socket socket, Connection con) {
        super("ServerThread");
        this.socket = socket;
        this.con = con;
    }

    public void run() {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String inputLine, outputLine;

            // Initiate conversation with client
            Backend backend = new Backend();
            outputLine = backend.processInput(null, con);
            out.println(outputLine);

            while ((inputLine = in.readLine()) != null) {
                outputLine = backend.processInput(inputLine, con);
                out.println(outputLine);
                if (outputLine.equals("Bye."))
                    break;
            }
            socket.close();
        } catch (IOException e) {
            System.out.println("Exception writing/reading to/from server.");
            e.printStackTrace();
        }
    }
}

