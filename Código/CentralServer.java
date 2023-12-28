import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
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
    private ThreadExecutor executorService;

    private Map<String, User> userDatabase = new HashMap<>();
    private Map<String, DataOutputStream> loggedInUsers = new HashMap<>();

    private int availableMemory = 1024 * 1024 * 1024;
    private int pendingTasks = 0;

    public CentralServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        executorService = new ThreadExecutor(10);
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
                executorService.submitTask(new ClientHandler(clientSocket));
                
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
        private ThreadExecutor clienThreadExecutor = new ThreadExecutor(5);
        private boolean exit = false;
        private Lock inputLock = new ReentrantLock();
        private Lock outputLock = new ReentrantLock();

        private DataInputStream in = null;
        private DataOutputStream out = null;


        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                in = new DataInputStream(clientSocket.getInputStream());
                out = new DataOutputStream(clientSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Runs the server thread, handling client requests.
         */
        @Override
        public void run() {
            try {
                while (!exit) {
                    String requestType = "FAIL";
                    inputLock.lock();
                    try {
                        requestType = in.readUTF();
                    } finally {
                        inputLock.unlock();
                    }
                    FuncExecutor funcExecutor = new FuncExecutor(requestType);
                    clienThreadExecutor.submitTask(funcExecutor);
                    Thread.sleep(1);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        class FuncExecutor implements Runnable {
            private String requestType;

            public FuncExecutor(String requestType) {
                this.requestType = requestType;
            }

            @Override
            public void run() {
                try {
                    switch (requestType) {
                        case "REGISTER":
                            handleRegister();
                            break;
                        case "LOGIN":
                            handleLogin();
                            break;
                        case "EXECUTE_TASK":
                            if (validateUser()) handleExecuteTask();
                            break;
                        case "QUERY_STATUS":
                            if (validateUser()) handleQueryStatus();
                            break;
                        case "LOGOUT":
                            if (validateUser()) {
                                exit = true;
                                loggedInUsers.remove(clientName);
                                clientSocket.close();
                                System.out.println("User " + clientName + " logged out.");
                            }
                            break;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
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
        private boolean handleRegister() throws IOException {
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
        private boolean handleLogin() throws IOException {
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
        private boolean validateUser() throws IOException {
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
        private byte[] readTaskFromClient() throws IOException {
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
        private void handleExecuteTask() throws IOException {
            byte[] task = readTaskFromClient();

            outputLock.lock();
            try {
                if (task.length > availableMemory) {
                    out.writeBoolean(false);
                    out.flush();
                } else {
                    out.writeBoolean(true);
                    out.flush();
                    byte[] result = executeTask(task);
                    availableMemory += task.length;
                    pendingTasks--;
                    sendResultToClient(result);
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
            pendingTasks++;
            availableMemory -= task.length;
                
            try {
                result = JobFunction.execute(task);
                return result;
            } catch (JobFunctionException e) {
                System.err.println("Job failed! Code = " + e.getCode() + " message=" + e.getMessage());
                return null;
            }
        }

        /**
         * Sends the result to the client.
         * 
         * @param result The byte array containing the result to be sent.
         * @param out The DataOutputStream used to send the result.
         * @throws IOException if an I/O error occurs while sending the result.
         */
        private void sendResultToClient(byte[] result) throws IOException {
            outputLock.lock();
            try {
                if (result == null) {
                    out.writeInt(0);
                    out.flush();
                    return;
                }
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
        private void handleQueryStatus() throws IOException {
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