import java.io.IOException;

/**
 * The ClientInterface interface represents the interface for interacting with a client in a distributed system.
 * It provides methods for authentication, registration, executing tasks, querying service status, and logging out.
 */
public interface ClientInterface {
    /**
     * Authenticates the client with the provided username and password.
     *
     * @param username the username of the client
     * @param password the password of the client
     * @return true if the authentication is successful, false otherwise
     * @throws IOException if an I/O error occurs during the authentication process
     */
    public boolean authenticate(String username, String password) throws IOException;

    /**
     * Registers a new client with the provided username and password.
     *
     * @param username the username of the client
     * @param password the password of the client
     * @return true if the registration is successful, false otherwise
     * @throws IOException if an I/O error occurs during the registration process
     */
    public boolean register(String username, String password) throws IOException;

    /**
     * Executes a task specified by the taskFile.
     *
     * @param taskFile the file containing the task to be executed
     * @throws IOException if an I/O error occurs during the task execution
     */
    public void executeTask(String taskFile) throws IOException;

    /**
     * Queries the status of the service.
     *
     * @return the current status of the service
     * @throws IOException if an I/O error occurs during the status query
     */
    public ServiceStatus queryServiceStatus() throws IOException;

    /**
     * Logs out the client from the system.
     *
     * @throws IOException if an I/O error occurs during the logout process
     */
    public void logout() throws IOException;
}