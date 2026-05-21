package id.ac.ui.cs.advprog.bemanagementpengiriman.controller;

import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.AssignDriverRequest;
import id.ac.ui.cs.advprog.bemanagementpengiriman.model.Pengiriman;
import id.ac.ui.cs.advprog.bemanagementpengiriman.security.UserPrincipal;
import id.ac.ui.cs.advprog.bemanagementpengiriman.service.PengirimanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class PengirimanControllerTest {

    private PengirimanService service;
    private PengirimanController controller;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(PengirimanService.class);
        controller = new PengirimanController(service);
    }

    @Test
    void assignDriver_accepted() {
        UserPrincipal principal = new UserPrincipal(1L, "MANDOR");
        AssignDriverRequest req = AssignDriverRequest.builder()
                .driverId(2L)
                .harvestItems(List.of(new AssignDriverRequest.HarvestItemDto(UUID.randomUUID())))
                .build();

        Pengiriman saved = new Pengiriman();
        saved.setId(1L);
        when(service.assignDriver(eq(1L), any(AssignDriverRequest.class))).thenReturn(saved);

        var resp = controller.assignDriver(principal, req);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }
}
