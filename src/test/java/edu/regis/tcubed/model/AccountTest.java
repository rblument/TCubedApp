package edu.regis.tcubed.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests verifying behavior of the {@link Account} domain model.
 *
 * @author Oscar Castillo Saucedo
 */
class AccountTest {

    @Test
    @DisplayName("getInitials returns uppercase initials when both first and last names are present")
    void getInitialsWithFirstAndLastName() {
        Account account = new Account("test@regis.edu", "pass123", "Oscar", "Castillo", 0, "Denver", true);
        assertEquals("OC", account.getInitials());

        account.setFirstName("testy");
        account.setLastName("mctest");
        assertEquals("TM", account.getInitials());
    }

    @Test
    @DisplayName("getInitials falls back to first letter of first name when last name is null or blank")
    void getInitialsWithFirstNameOnly() {
        Account account = new Account();
        account.setFirstName("Oscar");
        account.setLastName(null);
        assertEquals("O", account.getInitials());

        account.setLastName("   ");
        assertEquals("O", account.getInitials());
    }

    @Test
    @DisplayName("getInitials falls back to first letter of last name when first name is null or blank")
    void getInitialsWithLastNameOnly() {
        Account account = new Account();
        account.setFirstName(null);
        account.setLastName("Castillo");
        assertEquals("C", account.getInitials());

        account.setFirstName("   ");
        assertEquals("C", account.getInitials());
    }

    @Test
    @DisplayName("getInitials returns '?' when both names are null, empty, or whitespace")
    void getInitialsWithNullOrBlankNames() {
        Account account = new Account();
        assertEquals("?", account.getInitials());

        account.setFirstName("");
        account.setLastName("");
        assertEquals("?", account.getInitials());

        account.setFirstName("   ");
        account.setLastName("   ");
        assertEquals("?", account.getInitials());
    }

    @Test
    @DisplayName("getInitials handles leading whitespace properly")
    void getInitialsWithLeadingWhitespace() {
        Account account = new Account();
        account.setFirstName("  Jane ");
        account.setLastName("  Doe ");
        assertEquals("JD", account.getInitials());
    }
}
