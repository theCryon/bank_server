import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class Frontend {
    private final Socket socket;

    Frontend(Socket socket) {
        this.socket = socket;
    }

    public void menu() {
        while(true) {
            readFromServer();
            writeToServer();
        }

    }

    private void writeToServer() {
        PrintWriter printWriter;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            printWriter = new PrintWriter(socket.getOutputStream());
            String str = br.readLine();
            printWriter.println(str);
            printWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readFromServer() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while((line = br.readLine()) != null) {
                System.out.println(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
