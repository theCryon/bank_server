import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

class Client {
    public static final int PORT = 50001;
    public static final String HOST = "127.0.0.1";

    public static void main(String[] args) {
        //Connecting to server
        Socket socket;
        try {
            socket = new Socket(HOST, PORT);
            Frontend frontend = new Frontend(socket);
            frontend.menu();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
