import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface ClientInterface {
    public boolean authenticate(String username, String password, DataInputStream in, DataOutputStream out) throws IOException;
    public boolean register(String username, String password, DataInputStream in, DataOutputStream out) throws IOException;
    public void executeTask(String taskFile, DataInputStream in, DataOutputStream out) throws IOException;
    public ServiceStatus queryServiceStatus(DataInputStream in, DataOutputStream out) throws IOException;
    public void logout(DataOutputStream out) throws IOException;
}