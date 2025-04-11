import com.cafe94.enums.UserRole;

/**
 * Represents a Chef user in the Cafe94 system.
 * This is a concrete class inheriting properties from {@link Staff}.
 * @author Adigun Lateef
 * @version 1.0
 */
public class Chef extends Staff {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new Chef object.
     *
     * @param userID            Unique User ID
     * @param firstName         Chef's first name.
     * @param lastName          Chef's last name.
     * @param email             Chef's email address.
     * @param hashedPassword    Chef's hashed password.
     * @param staffId           The unique staff identifier.
     */
    public Chef(int userID, String firstName, String lastName, String email,
    String hashedPassword, String staffId) {
        // Explicitly call the Staff constructor, setting the role to
        // UserRole.CHEF
        super(userID, firstName, lastName, UserRole.CHEF, email,
        hashedPassword, staffId);
    }
}