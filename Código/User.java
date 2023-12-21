import java.util.Objects;

/**
 * Represents a user with a username and password.
 */
public class User {
    private String username;
    private String password;

    /**
     * Constructs a User object with the specified username and password.
     *
     * @param username the username of the user
     * @param password the password of the user
     */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Returns the username of the user.
     *
     * @return the username of the user
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the password of the user.
     *
     * @return the password of the user
     */
    public String getPassword() {
        return password;
    }

    /**
     * Checks if this User object is equal to the specified object.
     *
     * @param obj the object to compare
     * @return true if the objects are equal, false otherwise
     */
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User otherUser = (User) obj;
        return Objects.equals(username, otherUser.username);
    }

    /**
     * Returns the hash code value for this User object.
     *
     * @return the hash code value for this User object
     */
    public int hashCode() {
        return Objects.hash(username);
    }
}