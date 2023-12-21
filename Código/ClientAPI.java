import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientAPI {
    public static void main(String[] args) {
        Client c = new Client();

        try (Socket socket = new Socket("localhost", 8080);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream())) {

            c.auth(in, out);

            boolean exit = false;
            while (!exit) {
                c.printMenu();
                int option = c.scanner.nextInt();
                c.scanner.nextLine();

                switch (option) {
                    case 1:
                        c.executeTask(in, out);
                        break;
                    case 2:
                        c.queryServiceStatus(in, out);
                        break;
                    case 3:
                        c.logout(out);
                        exit = true;
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            }

            c.scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
