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