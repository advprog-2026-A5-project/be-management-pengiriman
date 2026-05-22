package id.ac.ui.cs.advprog.bemanagementpengiriman.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserSummaryTest {

    @Test
    void shortConstructor_shouldUseUsernameAsNama() {
        UserSummary user = new UserSummary(2L, "supir");

        assertEquals(2L, user.getId());
        assertEquals("supir", user.getUsername());
        assertEquals("supir", user.getNama());
    }
}
