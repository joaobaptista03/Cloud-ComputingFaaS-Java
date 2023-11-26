import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import sd23.*;

public class CentralServer {
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private BlockingQueue<Runnable> taskQueue;
    private AtomicInteger taskIdCounter = new AtomicInteger(0);

    private Map<String, User> userDatabase = new HashMap<>();
    private Map<String, DataOutputStream> loggedInUsers = new HashMap<>();

    private static final Lock inputLock = new ReentrantLock();
    private static final Lock outputLock = new ReentrantLock();
/* 
    private static final int MAX_MEMORY = 1024 * 1024 * 1024; // 1 GB em bytes

    private int availableMemory = MAX_MEMORY;
    private int pendingTasks = 0;
*/

    public CentralServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        taskQueue = new LinkedBlockingQueue<>();
        executorService = new ThreadPoolExecutor(2, 10, 30, TimeUnit.SECONDS, taskQueue);
//        startStatusQueryListener();
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
/* 
    private void startStatusQueryListener() {
        new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(10); // Ajuste o intervalo conforme necessário
                    queryServiceStatusToAllClients();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    
    private void queryServiceStatusToAllClients() {
        outputLock.lock();
        try {
            for (DataOutputStream out : loggedInUsers.values()) {
                out.writeUTF("QUERY_STATUS");
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            outputLock.unlock();
        }
    }
*/

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private int clientId;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            this.clientId = taskIdCounter.incrementAndGet();
        }

        @Override
        public void run() {
            try (
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())
            ) {

                String requestType = in.readUTF();
                if ("REGISTER".equals(requestType)) while (!handleRegister(in, out));
                while (!handleLogin(in, out));

                byte[] task = readTaskFromClient(in);
                byte[] result = executeTask(task);
                sendResultToClient(result, out);
/* 
                outputLock.lock();
                try {
                    availableMemory += task.length; // Supondo que a tarefa consome memória
                    pendingTasks--;
                } finally {
                    outputLock.unlock();
                }
*/
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
    
        private byte[] executeTask(byte[] task) {
            byte[] result;

            try {
                result = JobFunction.execute(task);
                return result;
            } catch (JobFunctionException e) {
                e.printStackTrace();
            }

            return null;
        }
    
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
/* 
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
*/
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