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

/**
 * The CentralServer class represents a central server that handles client requests in a distributed system.
 * It manages user registration, login, task execution, and status queries.
 */
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

    /**
     * Starts the central server and listens for incoming client connections.
     * Once a client connection is established, a new thread is created to handle the client.
     */
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

    /**
     * The ClientHandler class represents a thread that handles communication with a client.
     * It implements the Runnable interface and is responsible for processing client requests
     * and performing the necessary actions based on the request type.
     */
    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private String clientName = null;
        private Lock inputLock = new ReentrantLock();
        private Lock outputLock = new ReentrantLock();

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        /**
         * Runs the server thread, handling client requests.
         */
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
                                clientSocket.close();
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

        /**
         * Handles the registration of a user.
         * 
         * @param in The input stream to read data from.
         * @param out The output stream to write data to.
         * @return True if the registration is successful, false otherwise.
         * @throws IOException If an I/O error occurs.
         */
        private boolean handleRegister(DataInputStream in, DataOutputStream out) throws IOException {
            inputLock.lock();
            outputLock.lock();
            try {
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

            } finally {
                inputLock.unlock();
                outputLock.unlock();
            }
        }

        /**
         * Handles the login process for a client.
         * 
         * @param in The input stream to read data from the client.
         * @param out The output stream to send data to the client.
         * @return true if the login is successful, false otherwise.
         * @throws IOException if an I/O error occurs.
         */
        private boolean handleLogin(DataInputStream in, DataOutputStream out) throws IOException {
            outputLock.lock();
            inputLock.lock();
            try {
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
            } finally {
                outputLock.unlock();
                inputLock.unlock();
            }
        }

        /**
         * Checks if the given username and password match a user in the database.
         */
        private User authenticateUser(String username, String password) {
            User user = userDatabase.get(username);
            if (user != null && user.getPassword().equals(password)) {
                return user;
            }
            return null;
        }

        /**
         * Validates the user by checking if the clientName is null.
         * Sends a response to the client indicating whether the user is valid or not.
         * 
         * @param in The input stream to receive data from the client.
         * @param out The output stream to send data to the client.
         * @return true if the user is valid, false otherwise.
         * @throws IOException if an I/O error occurs while reading or writing data.
         */
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

        /**
         * Reads a task from the client.
         *
         * @param in the DataInputStream to read from
         * @return the byte array representing the task
         * @throws IOException if an I/O error occurs
         */
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

        /**
         * Handles the execution of a task received from a client.
         * 
         * @param in The input stream to read the task from.
         * @param out The output stream to send the result to the client.
         * @throws IOException if an I/O error occurs.
         */
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

        /**
         * Executes a task and returns the result.
         *
         * @param task the task to be executed
         * @return the result of the task execution
         */
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

        /**
         * Sends the result to the client.
         * 
         * @param result The byte array containing the result to be sent.
         * @param out The DataOutputStream used to send the result.
         * @throws IOException if an I/O error occurs while sending the result.
         */
        private void sendResultToClient(byte[] result, DataOutputStream out) throws IOException {
            outputLock.lock();
            try {
                out.writeInt(result.length);
                out.write(result);
                out.flush();
            } finally {
                outputLock.unlock();
            }
        }

        /**
         * Handles the query status request by sending the available memory and pending tasks to the client.
         * 
         * @param out the DataOutputStream used to send the response
         * @throws IOException if an I/O error occurs while sending the response
         */
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