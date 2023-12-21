import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import sd23.JobFunction;
import sd23.JobFunctionException;

public class CentralServer {
    private ServerSocket serverSocket;
    private ExecutorService executorService;

    private Map<String, User> userDatabase = new HashMap<>();
    private Map<String, DataOutputStream> loggedInUsers = new HashMap<>();

    private int availableMemory = 1024 * 1024 * 1024;
    private int pendingTasks = 0;

    public CentralServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        executorService = Executors.newFixedThreadPool(10);
    }

    public void start() {
        System.out.println("Central Server started.");
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(new ClientHandler(clientSocket));
                
            } catch (IOException e) {
                e.printStackTrace();
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

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private String clientName = null;
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
                boolean exit = false;
                while (!exit) {
                    String requestType = in.readUTF();
                    switch (requestType) {
                        case "REGISTER":
                            handleRegister(in, out);
                            break;
                        case "LOGIN":
                            handleLogin(in, out);
                            break;
                        case "EXECUTE_TASK":
                            if (validateUser(in, out)) handleExecuteTask(in, out);
                            break;
                        case "QUERY_STATUS":
                            if (validateUser(in, out)) handleQueryStatus(out);
                            break;
                        case "LOGOUT":
                            if (validateUser(in, out)) {
                                loggedInUsers.remove(clientName);
                                System.out.println("User " + clientName + " logged out.");
                                exit = true;
                            }
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

        private User authenticateUser(String username, String password) {
            User user = userDatabase.get(username);
            if (user != null && user.getPassword().equals(password)) {
                return user;
            }
            return null;
        }

        private boolean validateUser(DataInputStream in, DataOutputStream out) throws IOException {
            if (clientName == null) {
                outputLock.lock();
                try {
                    out.writeUTF("INVALID");
                    out.flush();
                    return false;
                } finally {
                    outputLock.unlock();
                }
            }

            outputLock.lock();
            try {
                out.writeUTF("VALID");
                out.flush();
                return true;
            } finally {
                outputLock.unlock();
            }
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
}