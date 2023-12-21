import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The Client class represents a client that interacts with a server.
 * It implements the ClientInterface interface.
 */
public class Client implements ClientInterface {
    private Lock inputLock = new ReentrantLock();
    private Lock outputLock = new ReentrantLock();
    public List<String> taskFiles = getFilesInDirectory("TestTaskFiles/Tasks/");
    private String name;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    
    public Client() throws UnknownHostException, IOException {
        this.socket = new Socket("localhost", 8080);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    /**
     * Retrieves a list of file names in the specified directory.
     *
     * @param directoryPath the path of the directory
     * @return a list of file names in the directory
     */
    private List<String> getFilesInDirectory(String directoryPath) {
        List<String> fileList = new ArrayList<>();
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file.getName());
                }
            }
        }
        return fileList;
    }

    /**
     * Executes a task by sending it to the server, checking memory availability, 
     * and processing the result.
     * 
     * @param taskFile the file containing the task to be executed
     * @throws IOException if an I/O error occurs while executing the task
     */
    public void executeTask(String taskFile) throws IOException {
        byte[] task = createTask(taskFile);
        sendTaskToServer(task);

        if (!readMemoryAvailability()) {
            System.out.println("Not enough memory available to execute task.");
            return;
        }
        
        byte[] result = readResultFromServer();

        processResult(taskFile, result);
    }

    /**
     * Reads the availability of memory from the input stream.
     * 
     * @return true if memory is available, false otherwise.
     * @throws IOException if an I/O error occurs.
     */
    private boolean readMemoryAvailability() throws IOException {
        inputLock.lock();
        try {
            return in.readBoolean();
        } finally {
            inputLock.unlock();
        }
    }

    /**
     * Authenticates the user with the provided username and password.
     * 
     * @param username The username of the user.
     * @param password The password of the user.
     * @return true if the authentication is successful, false otherwise.
     * @throws IOException if an I/O error occurs.
     */
    public boolean authenticate(String username, String password) throws IOException {
        String result = "";

        outputLock.lock();
        inputLock.lock();
        try {
            out.writeUTF("LOGIN");
            out.writeUTF(username);
            out.writeUTF(password);
            out.flush();

            result = in.readUTF();
        }
        finally {
            outputLock.unlock();
            inputLock.unlock();
        }

        if (result.equals("LOGIN_SUCCESS")) {
            name = username;
            return true;
        }
        
        return false;
    }

    /**
     * Registers a user with the given username and password.
     * 
     * @param username the username of the user to be registered
     * @param password the password of the user to be registered
     * @return true if the registration is successful, false otherwise
     * @throws IOException if an I/O error occurs while communicating with the server
     */
    public boolean register(String username, String password) throws IOException {
        outputLock.lock();
        inputLock.lock();
        try {
            out.writeUTF("REGISTER");
            out.writeUTF(username);
            out.writeUTF(password);
            out.flush();

            return in.readUTF().equals("REGISTER_SUCCESS");
        }
        finally {
            outputLock.unlock();
            inputLock.unlock();
        }
    }
    
    /**
     * Creates a byte array representation of a task by reading the contents of a task file.
     *
     * @param taskFile the path of the task file
     * @return the byte array representation of the task
     */
    private byte[] createTask(String taskFile) {
        File file = new File("TestTaskFiles/Tasks/" + taskFile);
        byte[] task = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(task);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return task;
    }

    /**
     * Sends a task to the server for execution.
     * 
     * @param task the task to be sent as a byte array
     * @return true if the task was sent successfully, false otherwise
     * @throws IOException if an I/O error occurs while sending the task
     */
    private boolean sendTaskToServer(byte[] task) throws IOException {
        outputLock.lock();
        inputLock.lock();
        try {
            out.writeUTF("EXECUTE_TASK");
            out.flush();

            if (in.readUTF().equals("INVALID")) {
                System.out.println("Invalid user.");
                return false;
            }

            out.writeInt(task.length);
            out.write(task);
            out.flush();

            return true;
        } finally {
            inputLock.unlock();
            outputLock.unlock();
        }
    }

    /**
     * Reads the result from the server.
     * 
     * @return the result received from the server as a byte array
     * @throws IOException if an I/O error occurs while reading the result
     */
    private byte[] readResultFromServer() throws IOException {
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
     * Processes the result of a task by saving it to a file.
     * 
     * @param taskFile The name of the task file.
     * @param result The byte array containing the result data.
     * @throws IOException If an I/O error occurs while saving the result.
     */
    private void processResult(String taskFile, byte[] result) throws IOException {
        File directory = new File("TestTaskFiles/Results/" + name);
        if (!directory.exists()) directory.mkdir();
        File file = new File("TestTaskFiles/Results/" + name + "/" + "-" + taskFile + ".zip");
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(result);
        }

        System.err.println("Task result saved to " + file.getAbsolutePath());
    }

    /**
        * Queries the service status and returns the current status of the service.
        * 
        * @return The ServiceStatus object containing the available memory and pending tasks.
        * @throws IOException if an I/O error occurs while communicating with the service.
        */
    public ServiceStatus queryServiceStatus() throws IOException {
        outputLock.lock();
        inputLock.lock();
        try {
            out.writeUTF("QUERY_STATUS");
            out.flush();

            if (in.readUTF().equals("INVALID")) {
                System.out.println("Invalid user.");
                return null;
            }
    
            int availableMemory = in.readInt();
            int pendingTasks = in.readInt();
    
            return new ServiceStatus(availableMemory, pendingTasks);
        } finally {
            inputLock.unlock();
            outputLock.unlock();
        }
    }

    /**
     * Logs out the user by sending a "LOGOUT" message to the server and closing the connection.
     * This method should be called when the user wants to end the session and disconnect from the server.
     * 
     * @throws IOException if an I/O error occurs while sending the "LOGOUT" message or closing the connection.
     */
    public void logout() throws IOException {
        outputLock.lock();
        try {
            out.writeUTF("LOGOUT");
            out.flush();
            
        } finally {
            outputLock.unlock();
            in.close();
            out.close();
            socket.close();
        }
    }
}