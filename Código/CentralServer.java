import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import sd23.*;

public class CentralServer {
    private ServerSocket serverSocket;
    private BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private ExecutorService executorService = new ThreadPoolExecutor(50, 51, 30, TimeUnit.SECONDS, taskQueue);

    private Map<String, User> userDatabase = new HashMap<>();
    private Map<String, DataOutputStream> loggedInUsers = new HashMap<>();

    private int availableMemory = 1024 * 1024 * 1024;
    private int pendingTasks = 0;

    public CentralServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public void start() {
        System.out.println("Central Server started.");
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                executorService.execute(clientHandler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private String clientName;
        private Lock inputLock = new ReentrantLock();
        private Lock outputLock = new ReentrantLock();

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())
            ) {
                String authType = in.readUTF();
                if ("REGISTER".equals(authType)) {
                    while (!handleRegister(in, out));
                }
                while (!handleLogin(in, out));

                while (true) {
                    String requestType = in.readUTF();
                    switch (requestType) {
                        case "EXECUTE_TASK":
                            handleExecuteTask(in, out);
                            break;
                        case "QUERY_STATUS":
                            handleQueryStatus(out);
                            break;
                        case "LOGOUT":
                            loggedInUsers.remove(clientName);
                            System.out.println("User " + clientName + " logged out.");
                            break;
                        default:
                            System.out.println("Invalid request type: " + requestType);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private boolean handleRegister(DataInputStream in, DataOutputStream out) throws IOException {
            String username = in.readUTF();
            String password = in.readUTF();

            if (!userDatabase.containsKey(username)) {
                userDatabase.put(username, new User(username, password));
                loggedInUsers.put(username, out);
                out.writeUTF("REGISTER_SUCCESS");
                System.out.println("User registered: " + username);
                return true;
            } else {
                out.writeUTF("REGISTER_FAILURE");
            }
            return false;
        }

        private boolean handleLogin(DataInputStream in, DataOutputStream out) throws IOException {
            String username = in.readUTF();
            String password = in.readUTF();

            User authenticatedUser = this.authenticateUser(username, password);
            if (authenticatedUser != null) {
                loggedInUsers.put(username, out);
                this.clientName = username;
                out.writeUTF("LOGIN_SUCCESS");
                System.out.println("User logged in: " + username);
                return true;
            } else {
                out.writeUTF("LOGIN_FAILURE");
                System.out.println("Server response: " + "LOGIN_FAILURE");
            }

            return false;
        }

        public User authenticateUser(String username, String password) {
            User user = userDatabase.get(username);
            if (user != null && user.getPassword().equals(password)) {
                return user;
            }
            return null;
        }

        private byte[] readTaskFromClient(DataInputStream in) throws IOException {
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

        private void handleExecuteTask(DataInputStream in, DataOutputStream out) throws IOException {
            byte[] task = readTaskFromClient(in);

            outputLock.lock();
            try {
                if (task.length > availableMemory) {
                    out.writeBoolean(false);
                    out.flush();
                } else {
                    out.writeBoolean(true);
                    out.flush();

                    byte[] result = executeTask(task);
                    sendResultToClient(result, out);
                    availableMemory += task.length;
                    pendingTasks--;
                }
            } finally {
                outputLock.unlock();
            }
        }

        private byte[] executeTask(byte[] task) {
            byte[] result;

            try {
                pendingTasks++;
                availableMemory -= task.length;
                result = JobFunction.execute(task);
                return result;
            } catch (JobFunctionException e) {
                e.printStackTrace();
            }

            return null;
        }

        private void sendResultToClient(byte[] result, DataOutputStream out) throws IOException {
            out.writeInt(result.length);
            out.write(result);
            out.flush();
        }

        private void handleQueryStatus(DataOutputStream out) throws IOException {
            outputLock.lock();
            try {
                out.writeInt(availableMemory);
                out.writeInt(pendingTasks);
                out.flush();
            } finally {
                outputLock.unlock();
            }
        }
    }

    public static void main(String[] args) {
        try {
            CentralServer server = new CentralServer(8080);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}