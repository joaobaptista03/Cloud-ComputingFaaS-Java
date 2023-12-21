import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client {
    private final Lock inputLock = new ReentrantLock();
    private final Lock outputLock = new ReentrantLock();
    public final Scanner scanner = new Scanner(System.in);
    private final List<String> taskFiles = getFilesInDirectory("TestTaskFiles/Tasks/");
    private String name;

    public List<String> getFilesInDirectory(String directoryPath) {
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
    
    public void printMenu() {
        System.out.println("1. Execute Task");
        System.out.println("2. Query Service Status");
        System.out.println("3. Exit");
        System.out.print("Choose an option: ");
    }

    public void executeTask(DataInputStream in, DataOutputStream out) throws IOException {
        String taskFile = "";

        while (!taskFiles.contains(taskFile)) {
            System.err.println("Type the task file you want to execute:");
            for (String task : taskFiles) {
                System.err.println("- " + task + ";");
            }
            taskFile = scanner.nextLine();
        }

        byte[] task = createTask(taskFile);
        sendTaskToServer(task, out);

        if (!readMemoryAvailability(in)) {
            System.out.println("Not enough memory available to execute task.");
            return;
        }
        
        byte[][] taskAndResult = readResultFromServer(in);

        int intValue = ByteBuffer.wrap(taskAndResult[0]).getInt();

        processResult(taskFile, intValue, taskAndResult[1]);
    }

    public boolean readMemoryAvailability(DataInputStream in) throws IOException {
        inputLock.lock();
        try {
            return in.readBoolean();
        } finally {
            inputLock.unlock();
        }
    }

    public void auth(DataInputStream in, DataOutputStream out) throws IOException {
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

    public boolean hasAccount() {
        String responseString;
    
        do {
            System.out.println("Digite \"s\" se já tiver conta (fazer Login) ou \"n\" se não tiver (Fazer registo)");
            responseString = scanner.nextLine();
        } while (!responseString.equalsIgnoreCase("s") && !responseString.equalsIgnoreCase("n"));
    
        return responseString.equalsIgnoreCase("s");
    }

    public boolean authenticate(DataInputStream in, DataOutputStream out) throws IOException {
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

        if (result.equals("LOGIN_SUCCESS")) {
            name = username;
            return true;
        }
        
        return false;
    }

    public boolean register(DataInputStream in, DataOutputStream out) throws IOException {
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
    
    public byte[] createTask(String taskFile) {
        File file = new File("TestTaskFiles/Tasks/" + taskFile);
        byte[] task = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(task);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return task;
    }

    public void sendTaskToServer(byte[] task, DataOutputStream out) throws IOException {
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

    public byte[][] readResultFromServer(DataInputStream in) throws IOException {
        inputLock.lock();
        try {
            int length = in.readInt();
            int taskNR = in.readInt();

            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
            buffer.putInt(taskNR);
            byte[] intByteArray = buffer.array();

            byte[] result = new byte[length];
            in.readFully(result);

            return new byte[][] { intByteArray, result };
        } finally {
            inputLock.unlock();
        }
    }

    public void processResult(String taskFile, int taskNR, byte[] result) throws IOException {
        File directory = new File("TestTaskFiles/Results/" + name);
        if (!directory.exists()) directory.mkdir();
        File file = new File("TestTaskFiles/Results/" + name + "/" + taskNR + "-" + taskFile + ".zip");
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(result);
        }

        System.err.println("Task result saved to " + file.getAbsolutePath());
    }

    public void queryServiceStatus(DataInputStream in, DataOutputStream out) throws IOException {
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

    public void logout(DataOutputStream out) throws IOException {
        outputLock.lock();
        try {
            out.writeUTF("LOGOUT");
            out.flush();
        } finally {
            outputLock.unlock();
        }
    }
}