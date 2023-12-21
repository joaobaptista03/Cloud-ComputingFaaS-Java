import java.io.IOException;
import java.util.Scanner;

public class ClientAPI {
    private static Scanner scanner = new Scanner(System.in);
    
    public static void main(String[] args) throws IOException {

        Client c = new Client();

        String responseString;
        do {
            System.out.println("Digite \"s\" se já tiver conta (fazer Login) ou \"n\" se não tiver (Fazer registo)");
            responseString = scanner.nextLine();
        } while (!responseString.equalsIgnoreCase("s") && !responseString.equalsIgnoreCase("n"));

        if (responseString.equalsIgnoreCase("n")) {
            register(c);
        }

        login(c);


        boolean exit = false;
        while (!exit) {
            System.out.println("Choose an option:");
            System.out.println("1. Execute Task");
            System.out.println("2. Query Service Status");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            int option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1:
                    String taskFile = "";

                    while (!c.taskFiles.contains(taskFile)) {
                        System.err.println("Type the task file you want to execute:");
                        for (String task : c.taskFiles) {
                            System.err.println("- " + task + ";");
                        }
                        taskFile = scanner.nextLine();
                    }
                    c.executeTask(taskFile);
                    break;
                case 2:
                    ServiceStatus ss = c.queryServiceStatus();

                    System.out.println("Service status:");
                    System.out.println("Available memory: " + ss.availableMemory);
                    System.out.println("Pending tasks: " + ss.pendingTasks);
                    break;
                case 3:
                    c.logout();
                    exit = true;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }

        scanner.close();
    }

    private static void login(Client c) throws IOException {
        boolean loginSuccess = false;
        while (!loginSuccess) {
            System.out.println("Type your username:");
            String username = scanner.nextLine();
            System.out.println("Type your password:");
            String password = scanner.nextLine();
            loginSuccess = c.authenticate(username, password);
            if (!loginSuccess) System.out.println("Login failed, try again.");
        }
        System.out.println("Login success!");
    }

    private static void register (Client c) throws IOException {
        boolean registerSuccess = false;

        while (!registerSuccess) {
            System.out.println("Type the username you want to register:");
            String username = scanner.nextLine();
            System.out.println("Type the password you want to register:");
            String password = scanner.nextLine();
            registerSuccess = c.register(username, password);
            if (!registerSuccess) System.out.println("Register failed (Username in use), try again.");
        }

        System.out.println("Register success!");
        System.out.println("Now, let's login!");
    }
}
