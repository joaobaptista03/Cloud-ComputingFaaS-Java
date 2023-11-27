import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final Lock inputLock = new ReentrantLock();
    private static final Lock outputLock = new ReentrantLock();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream())) {

            auth(in, out);

            boolean exit = false;
            while (!exit) {
                System.out.println("1. Execute Task");
                System.out.println("2. Query Service Status");
                System.out.println("3. Exit");
                System.out.print("Choose an option: ");
                
                int option = scanner.nextInt();
                scanner.nextLine();

                switch (option) {
                    case 1:
                        byte[] task = createTask();
                        sendTaskToServer(task, out);
                        byte[] result = readResultFromServer(in);
                        processResult(result);
                        break;
                    case 2:
                        queryServiceStatus(in, out);
                        break;
                    case 3:
                        out.writeUTF("LOGOUT");
                        out.flush();
                        exit = true;
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            }

            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void auth(DataInputStream in, DataOutputStream out) throws IOException {
        if (!hasAccount()) {
            boolean registerSuccess = false;

            outputLock.lock();
            try {
                out.writeUTF("REGISTER");
                out.flush();
            } finally {
                outputLock.unlock();
            }

            while (!registerSuccess) {
                registerSuccess = register(in, out);
                if (!registerSuccess) System.out.println("Register failed (Username in use), try again.");
            }

            System.out.println("Register success!");
        }
        else {
            outputLock.lock();
            try {
                out.writeUTF("LOGIN");
                out.flush();
            } finally {
                outputLock.unlock();
            }
        }

        boolean loginSuccess = false;
        while (!loginSuccess) {
            loginSuccess = authenticate(in, out);
            if (!loginSuccess) System.out.println("Login failed, try again.");
        }
        System.out.println("Login success!");
    }

    private static boolean hasAccount() {
        String responseString = "a";

        while(!responseString.equals("s") && !responseString.equals("n")) {
            System.out.println("Digite \"s\" se já tiver conta (fazer Login) ou \"n\" se não tiver (Fazer registo)");

            responseString = scanner.nextLine();
            
            if (responseString.equalsIgnoreCase("s")) return true;
            else if (responseString.equalsIgnoreCase("n")) return false;
        }

        return false;
    }

    private static boolean authenticate(DataInputStream in, DataOutputStream out) throws IOException {
        String username;
        String password;
        String result = "";
        
        System.out.print("Username: ");
        username = scanner.nextLine();
        System.out.print("Password: ");
        password = scanner.nextLine();

        inputLock.lock();
        try {
            out.writeUTF(username);
            out.writeUTF(password);
            out.flush();

            result = in.readUTF();
        }
        finally {
            inputLock.unlock();
        }

        return result.equals("LOGIN_SUCCESS");
    }

    private static boolean register(DataInputStream in, DataOutputStream out) throws IOException {
        String username;
        String password;
        String result = "";

        System.out.print("New username: ");
        username = scanner.nextLine();
        System.out.print("New password: ");
        password = scanner.nextLine();

        inputLock.lock();
        try {
            out.writeUTF(username);
            out.writeUTF(password);
            out.flush();

            result = in.readUTF();
        }
        finally {
            inputLock.unlock();
        }

        return result.equals("REGISTER_SUCCESS");
    }
    
    private static byte[] createTask() {
        System.out.println("Insere o número de bytes: ");
        int length = scanner.nextInt();

        byte[] task = new byte[length];

        return task;
    }

    private static void sendTaskToServer(byte[] task, DataOutputStream out) throws IOException {
        outputLock.lock();
        try {
            out.writeUTF("EXECUTE_TASK");
            out.flush();

            out.writeInt(task.length);
            out.write(task);
            out.flush();
        } finally {
            outputLock.unlock();
        }
    }

    private static byte[] readResultFromServer(DataInputStream in) throws IOException {
        inputLock.lock();
        try {
            int length = in.readInt();
            byte[] result = new byte[length];
            in.readFully(result);

            return result;
        } finally {
            inputLock.unlock();
        }
    }

    private static void processResult(byte[] result) throws IOException {
        System.out.println(result);
    }

    private static void queryServiceStatus(DataInputStream in, DataOutputStream out) throws IOException {
        outputLock.lock();
        try {
            out.writeUTF("QUERY_STATUS");
            out.flush();
    
            int availableMemory = in.readInt();
            int pendingTasks = in.readInt();
    
            System.out.println("Service Status:");
            System.out.println("Available Memory: " + availableMemory + " bytes");
            System.out.println("Pending Tasks: " + pendingTasks);
        } finally {
            outputLock.unlock();
        }
    }

}