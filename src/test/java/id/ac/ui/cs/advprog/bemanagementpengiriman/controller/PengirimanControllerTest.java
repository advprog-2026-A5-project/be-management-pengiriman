package id.ac.ui.cs.advprog.bemanagementpengiriman.controller;

import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.AssignDriverRequest;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.AdminPartialRejectionRequest;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.AdminRejectionRequest;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.MandorRejectionRequest;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.UpdateStatusPengirimanRequest;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.UserSummary;
import id.ac.ui.cs.advprog.bemanagementpengiriman.enums.StatusPengiriman;
import id.ac.ui.cs.advprog.bemanagementpengiriman.model.Pengiriman;
import id.ac.ui.cs.advprog.bemanagementpengiriman.security.UserPrincipal;
import id.ac.ui.cs.advprog.bemanagementpengiriman.service.PengirimanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

    @Test
    void assignDriver_rejectsMissingBodyAndServiceErrors() {
        UserPrincipal principal = new UserPrincipal(1L, "MANDOR");
        var missingBodyResponse = controller.assignDriver(principal, null);
        assertEquals(HttpStatus.BAD_REQUEST, missingBodyResponse.getStatusCode());
        assertEquals("Request body is required", missingBodyResponse.getBody());

        AssignDriverRequest req = AssignDriverRequest.builder()
                .driverId(2L)
                .harvestItems(List.of(new AssignDriverRequest.HarvestItemDto(UUID.randomUUID())))
                .build();
        when(service.assignDriver(eq(1L), any(AssignDriverRequest.class)))
                .thenThrow(new IllegalStateException("invalid"));

        var serviceErrorResponse = controller.assignDriver(principal, req);
        assertEquals(HttpStatus.BAD_REQUEST, serviceErrorResponse.getStatusCode());
        assertEquals("invalid", serviceErrorResponse.getBody());
    }

    @Test
    void updateStatus_coversSuccessBadRequestAndForbidden() {
        UserPrincipal principal = new UserPrincipal(2L, "SUPIR");
        Pengiriman pengiriman = Pengiriman.builder().id(10L).build();
        when(service.updateStatusPengiriman(10L, 2L, StatusPengiriman.MENGIRIM)).thenReturn(pengiriman);

        var okResponse = controller.updateStatus(
                10L,
                principal,
                new UpdateStatusPengirimanRequest(StatusPengiriman.MENGIRIM)
        );
        assertEquals(HttpStatus.OK, okResponse.getStatusCode());
        assertEquals(pengiriman, okResponse.getBody());

        var missingBodyResponse = controller.updateStatus(10L, principal, null);
        assertEquals(HttpStatus.BAD_REQUEST, missingBodyResponse.getStatusCode());

        when(service.updateStatusPengiriman(11L, 2L, StatusPengiriman.MENGIRIM))
                .thenThrow(new IllegalArgumentException("bad status"));
        var badRequestResponse = controller.updateStatus(
                11L,
                principal,
                new UpdateStatusPengirimanRequest(StatusPengiriman.MENGIRIM)
        );
        assertEquals(HttpStatus.BAD_REQUEST, badRequestResponse.getStatusCode());

        when(service.updateStatusPengiriman(12L, 2L, StatusPengiriman.MENGIRIM))
                .thenThrow(new SecurityException("not assigned"));
        var forbiddenResponse = controller.updateStatus(
                12L,
                principal,
                new UpdateStatusPengirimanRequest(StatusPengiriman.MENGIRIM)
        );
        assertEquals(HttpStatus.FORBIDDEN, forbiddenResponse.getStatusCode());
    }

    @Test
    void getPengirimanByDriver_coversSupirMandorOtherRoleAndServiceError() {
        UserPrincipal supir = new UserPrincipal(2L, "SUPIR");
        UserPrincipal mandor = new UserPrincipal(1L, "MANDOR");
        UserPrincipal admin = new UserPrincipal(99L, "ADMIN");
        List<Pengiriman> shipments = List.of(Pengiriman.builder().id(1L).build());

        when(service.getPengirimanByDriver(2L)).thenReturn(shipments);
        assertEquals(HttpStatus.OK, controller.getPengirimanByDriver(2L, supir).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.getPengirimanByDriver(3L, supir).getStatusCode());

        when(service.getPengirimanByDriverForMandor(1L, 2L)).thenReturn(shipments);
        assertEquals(HttpStatus.OK, controller.getPengirimanByDriver(2L, mandor).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.getPengirimanByDriver(2L, admin).getStatusCode());

        when(service.getPengirimanByDriverForMandor(1L, 4L)).thenThrow(new IllegalStateException("bad"));
        assertEquals(HttpStatus.BAD_REQUEST, controller.getPengirimanByDriver(4L, mandor).getStatusCode());
    }

    @Test
    void getHistory_coversOwnHistoryForbiddenAndBadRequest() {
        UserPrincipal principal = new UserPrincipal(2L, "SUPIR");
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 22);
        List<Pengiriman> history = List.of(Pengiriman.builder().id(1L).build());

        when(service.getPengirimanHistoryByDriver(2L, start, end)).thenReturn(history);
        assertEquals(HttpStatus.OK,
                controller.getPengirimanHistoryByDriver(2L, principal, start, end).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                controller.getPengirimanHistoryByDriver(3L, principal, start, end).getStatusCode());

        when(service.getPengirimanHistoryByDriver(2L, end, start)).thenThrow(new IllegalArgumentException("range"));
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.getPengirimanHistoryByDriver(2L, principal, end, start).getStatusCode());
    }

    @Test
    void mandorEndpoints_coverDriversOngoingApproveAndReject() {
        UserPrincipal mandor = new UserPrincipal(1L, "MANDOR");
        Pengiriman pengiriman = Pengiriman.builder().id(7L).build();
        List<UserSummary> drivers = List.of(new UserSummary(2L, "supir"));

        when(service.getAvailableDriversForMandor(1L, "su")).thenReturn(drivers);
        assertEquals(HttpStatus.OK, controller.getDrivers(mandor, "su").getStatusCode());

        when(service.getAvailableDriversForMandor(1L, "bad")).thenThrow(new IllegalArgumentException("bad"));
        assertEquals(HttpStatus.BAD_REQUEST, controller.getDrivers(mandor, "bad").getStatusCode());

        when(service.getOngoingPengiriman(1L)).thenReturn(List.of(pengiriman));
        assertEquals(HttpStatus.OK, controller.getOngoingPengiriman(mandor).getStatusCode());

        when(service.approveByMandor(7L, 1L)).thenReturn(pengiriman);
        assertEquals(HttpStatus.OK, controller.approveByMandor(7L, mandor).getStatusCode());

        when(service.approveByMandor(8L, 1L)).thenThrow(new IllegalStateException("bad"));
        assertEquals(HttpStatus.BAD_REQUEST, controller.approveByMandor(8L, mandor).getStatusCode());

        assertEquals(HttpStatus.BAD_REQUEST, controller.rejectByMandor(7L, mandor, null).getStatusCode());

        when(service.rejectByMandor(7L, 1L, "rusak")).thenReturn(pengiriman);
        assertEquals(HttpStatus.OK,
                controller.rejectByMandor(7L, mandor, new MandorRejectionRequest("rusak")).getStatusCode());

        when(service.rejectByMandor(9L, 1L, "rusak")).thenThrow(new IllegalArgumentException("bad"));
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.rejectByMandor(9L, mandor, new MandorRejectionRequest("rusak")).getStatusCode());
    }

    @Test
    void adminEndpoints_coverApprovedListApproveRejectAndPartialReject() {
        UserPrincipal admin = new UserPrincipal(99L, "ADMIN");
        Pengiriman pengiriman = Pengiriman.builder().id(20L).build();
        LocalDate date = LocalDate.of(2026, 5, 22);

        when(service.getApprovedPengirimanForAdmin(99L, "man", date)).thenReturn(List.of(pengiriman));
        assertEquals(HttpStatus.OK, controller.getApprovedByMandorForAdmin(admin, "man", date).getStatusCode());

        when(service.approveByAdmin(20L, 99L)).thenReturn(pengiriman);
        assertEquals(HttpStatus.OK, controller.approveByAdmin(20L, admin).getStatusCode());

        when(service.approveByAdmin(21L, 99L)).thenThrow(new IllegalArgumentException("bad"));
        assertEquals(HttpStatus.BAD_REQUEST, controller.approveByAdmin(21L, admin).getStatusCode());

        assertEquals(HttpStatus.BAD_REQUEST, controller.rejectByAdmin(20L, admin, null).getStatusCode());

        when(service.rejectByAdmin(20L, 99L, "tidak sesuai")).thenReturn(pengiriman);
        assertEquals(HttpStatus.OK,
                controller.rejectByAdmin(20L, admin, new AdminRejectionRequest("tidak sesuai")).getStatusCode());

        when(service.rejectByAdmin(21L, 99L, "tidak sesuai")).thenThrow(new IllegalStateException("bad"));
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.rejectByAdmin(21L, admin, new AdminRejectionRequest("tidak sesuai")).getStatusCode());

        assertEquals(HttpStatus.BAD_REQUEST, controller.partialRejectByAdmin(20L, admin, null).getStatusCode());

        when(service.partialRejectByAdmin(20L, 99L, 100.0, "sebagian rusak")).thenReturn(pengiriman);
        assertEquals(HttpStatus.OK, controller.partialRejectByAdmin(
                20L,
                admin,
                new AdminPartialRejectionRequest(100.0, "sebagian rusak")
        ).getStatusCode());

        when(service.partialRejectByAdmin(21L, 99L, 100.0, "sebagian rusak"))
                .thenThrow(new IllegalArgumentException("bad"));
        assertEquals(HttpStatus.BAD_REQUEST, controller.partialRejectByAdmin(
                21L,
                admin,
                new AdminPartialRejectionRequest(100.0, "sebagian rusak")
        ).getStatusCode());
    }

    @Test
    void getPengirimanById_coversSuccessNotFoundAndForbidden() {
        UserPrincipal principal = new UserPrincipal(99L, "ADMIN");
        Pengiriman pengiriman = Pengiriman.builder().id(5L).build();
        when(service.getPengirimanByIdForUser(5L, 99L, "ADMIN")).thenReturn(pengiriman);

        var okResponse = controller.getPengirimanById(5L, principal);
        assertEquals(HttpStatus.OK, okResponse.getStatusCode());
        assertEquals(pengiriman, okResponse.getBody());
        verify(service).getPengirimanByIdForUser(5L, 99L, "ADMIN");

        when(service.getPengirimanByIdForUser(404L, 99L, "ADMIN"))
                .thenThrow(new IllegalArgumentException("missing"));
        assertEquals(HttpStatus.NOT_FOUND, controller.getPengirimanById(404L, principal).getStatusCode());

        when(service.getPengirimanByIdForUser(403L, 99L, "ADMIN"))
                .thenThrow(new SecurityException("forbidden"));
        assertEquals(HttpStatus.FORBIDDEN, controller.getPengirimanById(403L, principal).getStatusCode());
    }
}
