package id.ac.ui.cs.advprog.bemanagementpengiriman.controller;

import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.AdminPartialRejectionRequest;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.AdminRejectionRequest;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.AssignDriverRequest;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.MandorRejectionRequest;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.UpdateStatusPengirimanRequest;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.UserSummary;
import id.ac.ui.cs.advprog.bemanagementpengiriman.model.Pengiriman;
import id.ac.ui.cs.advprog.bemanagementpengiriman.service.PengirimanService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import id.ac.ui.cs.advprog.bemanagementpengiriman.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pengiriman")
@RequiredArgsConstructor
public class PengirimanController {

    private static final String ROLE_MANDOR = "MANDOR";
    private static final String ROLE_SUPIR = "SUPIR";

    private final PengirimanService pengirimanService;

    @PostMapping("/assign")
    @PreAuthorize("hasRole('MANDOR')")
    public ResponseEntity<?> assignDriver(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) AssignDriverRequest request
    ) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body("Request body is required");
            }
            Pengiriman pengiriman = pengirimanService.assignDriver(principal.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(pengiriman);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{pengirimanId}/status")
    @PreAuthorize("hasRole('SUPIR')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long pengirimanId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) UpdateStatusPengirimanRequest request
    ) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body("Request body is required");
            }
            Pengiriman pengiriman = pengirimanService.updateStatusPengiriman(
                    pengirimanId,
                    principal.getId(),
                    request.getNewStatus()
            );
            return ResponseEntity.ok(pengiriman);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasAnyRole('SUPIR','MANDOR')")
    public ResponseEntity<?> getPengirimanByDriver(
            @PathVariable Long driverId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        try {
            if (principal.getRole() != null && principal.getRole().equalsIgnoreCase(ROLE_SUPIR)) {
                if (!driverId.equals(principal.getId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Supir can only access their own shipments");
                }
                return ResponseEntity.ok(pengirimanService.getPengirimanByDriver(driverId));
            }

            if (principal.getRole() != null && principal.getRole().equalsIgnoreCase(ROLE_MANDOR)) {
                return ResponseEntity.ok(pengirimanService.getPengirimanByDriverForMandor(principal.getId(), driverId));
            }

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only MANDOR or SUPIR can access this endpoint");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/driver/{driverId}/history")
    @PreAuthorize("hasRole('SUPIR')")
    public ResponseEntity<?> getPengirimanHistoryByDriver(
            @PathVariable Long driverId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            if (!driverId.equals(principal.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Supir can only access their own shipment history");
            }
            return ResponseEntity.ok(pengirimanService.getPengirimanHistoryByDriver(driverId, startDate, endDate));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/drivers")
    @PreAuthorize("hasRole('MANDOR')")
    public ResponseEntity<?> getDrivers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "searchName", required = false) String searchName
    ) {
        try {
            List<UserSummary> drivers = pengirimanService.getAvailableDriversForMandor(principal.getId(), searchName);
            return ResponseEntity.ok(drivers);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/ongoing")
    @PreAuthorize("hasRole('MANDOR')")
    public ResponseEntity<List<Pengiriman>> getOngoingPengiriman(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(pengirimanService.getOngoingPengiriman(principal.getId()));
    }

    @PatchMapping("/{pengirimanId}/mandor/approve")
    @PreAuthorize("hasRole('MANDOR')")
    public ResponseEntity<?> approveByMandor(
            @PathVariable Long pengirimanId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        try {
            return ResponseEntity.ok(pengirimanService.approveByMandor(pengirimanId, principal.getId()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{pengirimanId}/mandor/reject")
    @PreAuthorize("hasRole('MANDOR')")
    public ResponseEntity<?> rejectByMandor(
            @PathVariable Long pengirimanId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) MandorRejectionRequest request
    ) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body("Request body is required");
            }
            return ResponseEntity.ok(
                    pengirimanService.rejectByMandor(pengirimanId, principal.getId(), request.getRejectionReason()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/admin/approved-mandor")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getApprovedByMandorForAdmin(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "mandorName", required = false) String mandorName,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(pengirimanService.getApprovedPengirimanForAdmin(principal.getId(), mandorName, date));
    }

    @PatchMapping("/{pengirimanId}/admin/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveByAdmin(
            @PathVariable Long pengirimanId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        try {
            return ResponseEntity.ok(pengirimanService.approveByAdmin(pengirimanId, principal.getId()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{pengirimanId}/admin/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectByAdmin(
            @PathVariable Long pengirimanId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) AdminRejectionRequest request
    ) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body("Request body is required");
            }
            return ResponseEntity.ok(
                    pengirimanService.rejectByAdmin(pengirimanId, principal.getId(), request.getRejectionReason()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{pengirimanId}/admin/partial-reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> partialRejectByAdmin(
            @PathVariable Long pengirimanId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) AdminPartialRejectionRequest request
    ) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body("Request body is required");
            }
            return ResponseEntity.ok(
                    pengirimanService.partialRejectByAdmin(
                            pengirimanId,
                            principal.getId(),
                            request.getAcknowledgedWeightKg(),
                            request.getRejectionReason()
                    )
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{pengirimanId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANDOR','SUPIR')")
    public ResponseEntity<?> getPengirimanById(
            @PathVariable Long pengirimanId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        try {
            return ResponseEntity.ok(
                    pengirimanService.getPengirimanByIdForUser(
                            pengirimanId,
                            principal.getId(),
                            principal.getRole()
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}
