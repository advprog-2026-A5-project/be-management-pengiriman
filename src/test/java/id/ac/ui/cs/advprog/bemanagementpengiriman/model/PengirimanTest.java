    package id.ac.ui.cs.advprog.bemanagementpengiriman.model;

    import id.ac.ui.cs.advprog.bemanagementpengiriman.enums.StatusPengiriman;
    import org.junit.jupiter.api.BeforeEach;
    import org.junit.jupiter.api.Test;

    import java.time.LocalDateTime;
    import java.util.List;
    import java.util.UUID;

    import static org.junit.jupiter.api.Assertions.*;

    class PengirimanTest {

        private Pengiriman pengiriman;
        private Long driverId;
        private Long mandorId;

        @BeforeEach
        void setUp() {
            driverId = 2L;
            mandorId = 1L;

            PengirimanItem item = PengirimanItem.builder()
                    .harvestId(UUID.fromString("eb558e9f-1c39-460e-8860-71af6af63bd6"))
                    .weightKg(150.0)
                    .build();

            pengiriman = new Pengiriman();
            pengiriman.setId(100L);
            pengiriman.setDriverId(driverId);
            pengiriman.setMandorId(mandorId);
            pengiriman.setItems(List.of(item));
            pengiriman.setStatus(StatusPengiriman.MEMUAT);
            pengiriman.setTotalWeightKg(150.0);
            pengiriman.setRejectionReason(null);
        }

        @Test
        void testGetPengirimanId() {
            assertEquals(100L, pengiriman.getId());
        }

        @Test
        void testGetPengirimanIdFail() {
            assertNotEquals(101L, pengiriman.getId());
        }

        @Test
        void testGetDriverId() {
            assertEquals(2L, pengiriman.getDriverId());
        }

        @Test
        void testGetMandorId() {
            assertEquals(1L, pengiriman.getMandorId());
        }

        @Test
        void testGetStatus() {
            assertEquals(StatusPengiriman.MEMUAT, pengiriman.getStatus());
        }

        @Test
        void testGetStatusFail() {
            assertNotEquals(StatusPengiriman.MENGIRIM, pengiriman.getStatus());
        }

        @Test
        void testGetTotalWeight() {
            assertEquals(150.0, pengiriman.getTotalWeightKg());
        }

        @Test
        void testGetTotalWeightFail() {
            assertNotEquals(400.0, pengiriman.getTotalWeightKg());
        }

        @Test
        void testOnCreateSetsDefaultStatusAndTimestamps() {
            Pengiriman newPengiriman = new Pengiriman();

            newPengiriman.onCreate();

            assertEquals(StatusPengiriman.MEMUAT, newPengiriman.getStatus());
            assertNotNull(newPengiriman.getCreatedAt());
            assertNotNull(newPengiriman.getUpdatedAt());
        }

        @Test
        void testOnCreateKeepsExistingStatus() {
            Pengiriman newPengiriman = new Pengiriman();
            newPengiriman.setStatus(StatusPengiriman.MENGIRIM);

            newPengiriman.onCreate();

            assertEquals(StatusPengiriman.MENGIRIM, newPengiriman.getStatus());
        }

        @Test
        void testOnUpdateRefreshesUpdatedAt() {
            Pengiriman newPengiriman = new Pengiriman();
            newPengiriman.setUpdatedAt(LocalDateTime.now().minusDays(1));

            LocalDateTime oldUpdatedAt = newPengiriman.getUpdatedAt();
            newPengiriman.onUpdate();

            assertNotNull(newPengiriman.getUpdatedAt());
            assertTrue(newPengiriman.getUpdatedAt().isAfter(oldUpdatedAt));
        }
    }
