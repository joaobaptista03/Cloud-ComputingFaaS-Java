import java.io.IOException;
import java.util.Scanner;

public class ClientUI {
    private static Scanner scanner = new Scanner(System.in);
    private static boolean exit = false;
    private static boolean proceed = true;
    private static ThreadExecutor executorService = new ThreadExecutor(5);
    
    public static void main(String[] args) throws IOException, InterruptedException {
        Client c = new Client();

        String responseString;
        do {
            System.out.println("Type \"y\" if you already have an account (Login) or \"n\" otherwise (Register)");
            responseString = scanner.nextLine();
        } while (!responseString.equalsIgnoreCase("y") && !responseString.equalsIgnoreCase("n"));

        if (responseString.equalsIgnoreCase("n")) {
            register(c);
        }

        login(c);

        while (!exit) {
            while (!proceed) {
                Thread.sleep(1);
            }
            Thread.sleep(1000);
            System.out.println("Choose an option: ");
            System.out.println("1. Execute Task");
            System.out.println("2. Query Service Status");
            System.out.println("3. Exit");

            int option = scanner.nextInt();
            scanner.nextLine();
            executorService.submitTask(new OptionExecutor(option, c));
            Thread.sleep(1);
        }

        scanner.close();
    }

    public static class OptionExecutor implements Runnable {
        private int option;
        private Client c;
        private Scanner threadScanner = new Scanner(System.in);

        public OptionExecutor(int option, Client c) {
            this.option = option;
            this.c = c;
        }

        public void run() {
            try {
                switch (option) {
                    case 1:
                        String taskFile = "";

                        proceed = false;
                        while (!c.taskFiles.contains(taskFile)) {
                            System.err.println("Type the task file you want to execute:");
                            for (String task : c.taskFiles) {
                                System.err.println("- " + task + ";");
                            }
                            taskFile = threadScanner.nextLine();
                        }
                        proceed = true;
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
                        System.out.println("Exiting the program. Thank you!");
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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