package edu.regis.tcubed.model;

/**
 * Represents a user account in the TCubed tracking system.
 * This class maps directly to the ACCOUNT table in the database.
 * 
 * @author Thomas Wintenburg
 */
public class Account {
    
    private String userId;     // Primary Key (VARCHAR)
    private String password;   // (VARCHAR)
    private String firstName;  // (VARCHAR)
    private String lastName;   // (VARCHAR)
    private int question;      // Security question ID (INT)
    private String answer;     // Security question answer (VARCHAR)
    private boolean isStudent; // Maps to TINYINT (0 = false, 1 = true)

    /**
     * Default constructor required for Spring and data mapping.
     */
    public Account() {
    }

    /**
     * Constructs a new Account with all the attributes.
     */
    public Account(String userId, String password, String firstName, String lastName, 
                   int question, String answer, boolean isStudent) {
        this.userId = userId;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.question = question;
        this.answer = answer;
        this.isStudent = isStudent;
    }

    // --- Getters and Setters ---

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getQuestion() {
        return question;
    }

    public void setQuestion(int question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isStudent() {
        return isStudent;
    }

    public void setStudent(boolean student) {
        isStudent = student;
    }

    /**
     * Computes uppercase initials from the user's first name and last name.
     * Handles null, empty, or whitespace strings safely by falling back to the
     * first letter of whichever name is present, or "?" if both are blank or null.
     *
     * @return uppercase initials (e.g., "TM", "OC", or "?")
     */
    public String getInitials() {
        String first = (firstName != null) ? firstName.trim() : "";
        String last = (lastName != null) ? lastName.trim() : "";

        StringBuilder sb = new StringBuilder();
        if (!first.isEmpty()) {
            sb.append(Character.toUpperCase(first.charAt(0)));
        }
        if (!last.isEmpty()) {
            sb.append(Character.toUpperCase(last.charAt(0)));
        }

        return sb.length() > 0 ? sb.toString() : "?";
    }

    @Override
    public String toString() {
        return "Account{" +
                "userId='" + userId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", isStudent=" + isStudent +
                '}';
    }
}