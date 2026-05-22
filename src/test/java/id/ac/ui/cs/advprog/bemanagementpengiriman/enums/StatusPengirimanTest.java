package id.ac.ui.cs.advprog.bemanagementpengiriman.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusPengirimanTest {

    @Test
    void canDriverTransitionTo_shouldFollowDriverStateMachine() {
        assertEquals("MEMUAT", StatusPengiriman.MEMUAT.getValue());
        assertTrue(StatusPengiriman.MEMUAT.canDriverTransitionTo(StatusPengiriman.MENGIRIM));
        assertFalse(StatusPengiriman.MEMUAT.canDriverTransitionTo(StatusPengiriman.TIBA_DI_TUJUAN));
        assertTrue(StatusPengiriman.MENGIRIM.canDriverTransitionTo(StatusPengiriman.TIBA_DI_TUJUAN));
        assertFalse(StatusPengiriman.MENGIRIM.canDriverTransitionTo(StatusPengiriman.MEMUAT));
        assertFalse(StatusPengiriman.APPROVED_ADMIN.canDriverTransitionTo(StatusPengiriman.MENGIRIM));
    }
}
