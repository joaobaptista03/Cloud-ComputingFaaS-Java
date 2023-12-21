import java.io.IOException;

public interface ClientInterface {
    public boolean authenticate(String username, String password) throws IOException;
    public boolean register(String username, String password) throws IOException;
    public void executeTask(String taskFile) throws IOException;
    public ServiceStatus queryServiceStatus() throws IOException;
    public void logout() throws IOException;
}