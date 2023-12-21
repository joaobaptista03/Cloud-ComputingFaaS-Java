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

    private boolean readMemoryAvailability() throws IOException {
        inputLock.lock();
        try {
            return in.readBoolean();
        } finally {
            inputLock.unlock();
        }
    }

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

    private void processResult(String taskFile, byte[] result) throws IOException {
        File directory = new File("TestTaskFiles/Results/" + name);
        if (!directory.exists()) directory.mkdir();
        File file = new File("TestTaskFiles/Results/" + name + "/" + "-" + taskFile + ".zip");
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(result);
        }

        System.err.println("Task result saved to " + file.getAbsolutePath());
    }

    public ServiceStatus queryServiceStatus() throws IOException {
        outputLock.lock();
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
            outputLock.unlock();
        }
    }

    public void logout() throws IOException {
        outputLock.lock();
        try {
            out.writeUTF("LOGOUT");
            out.flush();
            
        } finally {
            outputLock.unlock();
        }
    }
}