package id.ac.ui.cs.advprog.bemanagementpengiriman.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UserPrincipalTest {

    @Test
    void equalsAndHashCode_shouldUseIdAndRole() {
        UserPrincipal principal = new UserPrincipal(1L, "ADMIN");
        UserPrincipal samePrincipal = new UserPrincipal(1L, "ADMIN");
        UserPrincipal differentPrincipal = new UserPrincipal(2L, "SUPIR");

        assertEquals(1L, principal.getId());
        assertEquals("ADMIN", principal.getRole());
        assertEquals(principal, samePrincipal);
        assertEquals(principal.hashCode(), samePrincipal.hashCode());
        assertNotEquals(principal, differentPrincipal);
        assertNotEquals(principal, null);
        assertNotEquals(principal, "ADMIN");
    }
}
