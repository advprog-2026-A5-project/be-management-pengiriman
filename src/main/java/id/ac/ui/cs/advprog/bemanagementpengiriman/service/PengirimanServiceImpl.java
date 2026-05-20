package id.ac.ui.cs.advprog.bemanagementpengiriman.service;

import id.ac.ui.cs.advprog.bemanagementpengiriman.client.HarvestClient;
import id.ac.ui.cs.advprog.bemanagementpengiriman.client.UserClient;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.AssignDriverRequest;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.UserSummary;
import id.ac.ui.cs.advprog.bemanagementpengiriman.enums.StatusPengiriman;
import id.ac.ui.cs.advprog.bemanagementpengiriman.model.Pengiriman;
import id.ac.ui.cs.advprog.bemanagementpengiriman.model.PengirimanItem;
import id.ac.ui.cs.advprog.bemanagementpengiriman.repository.PengirimanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PengirimanServiceImpl implements PengirimanService {

    private static final double MAX_WEIGHT_KG = 400.0;
    private static final List<StatusPengiriman> ACTIVE_SHIPMENT_STATUSES = List.of(
            StatusPengiriman.MEMUAT,
            StatusPengiriman.MENGIRIM,
            StatusPengiriman.TIBA_DI_TUJUAN
    );
        private static final List<StatusPengiriman> DRIVER_HISTORY_STATUSES = List.of(
            StatusPengiriman.APPROVED_MANDOR,
            StatusPengiriman.REJECTED_MANDOR,
            StatusPengiriman.APPROVED_ADMIN,
            StatusPengiriman.REJECTED_ADMIN,
            StatusPengiriman.PARTIALLY_REJECTED_ADMIN
        );

    private final PengirimanRepository pengirimanRepository;
    private final UserClient userClient;
    private final HarvestClient harvestClient;
    private final id.ac.ui.cs.advprog.bemanagementpengiriman.events.EventPublisher eventPublisher;

    @Override
    @Transactional
    @Retryable(
            retryFor = org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )    public Pengiriman assignDriver(Long mandorId, AssignDriverRequest request) {
        validateAssignDriverRequest(request);

        ensureUserExists(mandorId, "Mandor not found");
        ensureUserExists(request.getDriverId(), "Driver not found");

        double totalWeight = 0.0;
        Set<Long> harvestIdsInRequest = new HashSet<>();

        for (AssignDriverRequest.HarvestItemDto item : request.getHarvestItems()) {
            if (item == null) {
                throw new IllegalArgumentException("Harvest item cannot be null");
            }
            if (item.getHarvestId() == null) {
                throw new IllegalArgumentException("Harvest ID is required for each item");
            }
            if (item.getWeightKg() <= 0) {
                throw new IllegalArgumentException("Each harvest item weight must be greater than 0");
            }
            if (!harvestIdsInRequest.add(item.getHarvestId())) {
                throw new IllegalArgumentException("Duplicate harvest item in request");
            }

            if (!harvestClient.isApprovedHarvest(item.getHarvestId())) {
                throw new IllegalArgumentException("Harvest item is not approved");
            }

            long activeShipmentCount = pengirimanRepository.countActiveShipmentByHarvestId(
                    item.getHarvestId(), ACTIVE_SHIPMENT_STATUSES);
            if (activeShipmentCount > 0) {
                throw new IllegalArgumentException(
                    "Harvest item already assigned to an active shipment");
            }

            totalWeight += item.getWeightKg();
        }

        if (totalWeight > MAX_WEIGHT_KG) {
            throw new IllegalArgumentException(
                    String.format("Total weight %.2f Kg exceeds maximum capacity of %.0f Kg",
                            totalWeight, MAX_WEIGHT_KG));
        }

        if (totalWeight <= 0) {
            throw new IllegalArgumentException("Total weight must be greater than 0");
        }

        Pengiriman pengiriman = Pengiriman.builder()
            .driverId(request.getDriverId())
            .mandorId(mandorId)
                .status(StatusPengiriman.MEMUAT)
                .totalWeightKg(totalWeight)
                .items(new ArrayList<>())
                .build();

        // Create pengiriman items
        for (AssignDriverRequest.HarvestItemDto item : request.getHarvestItems()) {
            PengirimanItem pengirimanItem = PengirimanItem.builder()
                    .shipment(pengiriman)
                    .harvestId(item.getHarvestId())
                    .weightKg(item.getWeightKg())
                    .build();
            pengiriman.getItems().add(pengirimanItem);
        }

        Pengiriman saved = pengirimanRepository.save(pengiriman);
        try {
            eventPublisher.publish("pengiriman-assigned", saved);
        } catch (Exception ignored) {
            // best-effort publish
        }
        return saved;
    }

    @Override
    @Transactional
    public Pengiriman updateStatusPengiriman(Long pengirimanId, Long driverId, StatusPengiriman newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status is required");
        }

        Pengiriman pengiriman = pengirimanRepository.findById(pengirimanId)
                .orElseThrow(() -> new IllegalArgumentException("Pengiriman not found"));

        // Verify the driver is assigned to this pengiriman
        if (!pengiriman.getDriverId().equals(driverId)) {
            throw new SecurityException("Driver is not assigned to this pengiriman");
        }

        // Validate state machine transition
        StatusPengiriman currentStatus = pengiriman.getStatus();
        if (!currentStatus.canDriverTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s", currentStatus, newStatus));
        }

        pengiriman.setStatus(newStatus);
        return pengirimanRepository.save(pengiriman);
    }

    @Override
    public List<Pengiriman> getPengirimanByDriver(Long driverId) {
        return pengirimanRepository.findByDriverIdAndStatusIn(driverId, ACTIVE_SHIPMENT_STATUSES);
    }

    @Override
    public List<Pengiriman> getPengirimanByDriverForMandor(Long mandorId, Long driverId) {
        ensureUserExists(mandorId, "Mandor not found");

        return pengirimanRepository.findByMandorIdAndDriverIdAndStatusIn(
                mandorId,
                driverId,
                ACTIVE_SHIPMENT_STATUSES
        );
    }

    @Override
    public List<Pengiriman> getPengirimanHistoryByDriver(Long driverId, LocalDate startDate, LocalDate endDate) {
        ensureUserExists(driverId, "Driver not found");

            validateDateRange(startDate, endDate);

            return pengirimanRepository.findDriverHistory(
                driverId,
                DRIVER_HISTORY_STATUSES,
                toStartDateTime(startDate),
                toEndDateTime(endDate)
            );
            }

    @Override
    public List<Pengiriman> getOngoingPengiriman(Long mandorId) {
        return pengirimanRepository.findByMandorIdAndStatusIn(mandorId, ACTIVE_SHIPMENT_STATUSES);
    }

    @Override
    public List<Pengiriman> getPengirimanByStatus(StatusPengiriman status) {
        return pengirimanRepository.findByStatus(status);
    }

    @Override
    public List<Pengiriman> getApprovedPengirimanForAdmin(String mandorName, LocalDate date) {
        String normalizedMandorName = normalizeName(mandorName);
        List<Long> mandorIds = null;
        if (normalizedMandorName != null) {
            List<UserSummary> matches = userClient.findByUsernameContainingIgnoreCase(normalizedMandorName);
            if (matches.isEmpty()) {
                return List.of();
            }
            mandorIds = matches.stream().map(UserSummary::getId).toList();
        }

        LocalDateTime startDate = date == null ? null : date.atStartOfDay();
        LocalDateTime endDate = date == null ? null : date.atTime(LocalTime.MAX);

        return pengirimanRepository.findForAdminApproval(
                StatusPengiriman.APPROVED_MANDOR,
            mandorIds,
                startDate,
                endDate
        );
    }

    @Override
    public List<UserSummary> getAvailableDriversForMandor(Long mandorId, String searchName) {
        UserSummary mandor = ensureUserExists(mandorId, "Mandor not found");

        List<UserSummary> users;
        if (searchName == null || searchName.isBlank()) {
            users = userClient.findAll();
        } else {
            users = userClient.findByUsernameContainingIgnoreCase(searchName.trim());
        }

        // Temporary filter nunggu role dan perkebunun diimplementasi.
        return users.stream()
            .filter(user -> !user.getId().equals(mandor.getId()))
                .toList();
    }

    @Override
    @Transactional
    public Pengiriman approveByMandor(Long pengirimanId, Long mandorId) {
        ensureUserExists(mandorId, "Mandor not found");

        Pengiriman pengiriman = pengirimanRepository.findById(pengirimanId)
                .orElseThrow(() -> new IllegalArgumentException("Pengiriman not found"));

        if (!pengiriman.getMandorId().equals(mandorId)) {
            throw new SecurityException("Mandor is not assigned to this pengiriman");
        }
        if (pengiriman.getStatus() != StatusPengiriman.TIBA_DI_TUJUAN) {
            throw new IllegalStateException("Mandor can only approve/reject shipment after it reaches destination");
        }

        pengiriman.setStatus(StatusPengiriman.APPROVED_MANDOR);
        pengiriman.setRejectionReason(null);
        pengiriman.setAcknowledgedWeightKg(pengiriman.getTotalWeightKg());
        Pengiriman saved = pengirimanRepository.save(pengiriman);
        try {
            eventPublisher.publish("pengiriman-approved-mandor", saved);
            eventPublisher.publish("payroll-driver-requested",
                new id.ac.ui.cs.advprog.bemanagementpengiriman.events.PayrollEvent(
                    saved.getDriverId(),
                    "DRIVER",
                    saved.getId(),
                    saved.getTotalWeightKg(),
                    null));
        } catch (Exception ignored) {}
        return saved;
    }

    @Override
    @Transactional
    public Pengiriman rejectByMandor(Long pengirimanId, Long mandorId, String rejectionReason) {
        ensureUserExists(mandorId, "Mandor not found");

        Pengiriman pengiriman = pengirimanRepository.findById(pengirimanId)
                .orElseThrow(() -> new IllegalArgumentException("Pengiriman not found"));

        if (!pengiriman.getMandorId().equals(mandorId)) {
            throw new SecurityException("Mandor is not assigned to this pengiriman");
        }
        if (pengiriman.getStatus() != StatusPengiriman.TIBA_DI_TUJUAN) {
            throw new IllegalStateException("Mandor can only approve/reject shipment after it reaches destination");
        }

        String validatedReason = requireReason(rejectionReason);

        pengiriman.setStatus(StatusPengiriman.REJECTED_MANDOR);
        pengiriman.setRejectionReason(validatedReason);
        pengiriman.setAcknowledgedWeightKg(null);
        Pengiriman saved = pengirimanRepository.save(pengiriman);
        try { eventPublisher.publish("pengiriman-rejected-mandor", saved); } catch (Exception ignored) {}
        return saved;
    }

    @Override
    @Transactional
    public Pengiriman approveByAdmin(Long pengirimanId, Long adminId) {
        ensureUserExists(adminId, "Admin not found");

        Pengiriman pengiriman = pengirimanRepository.findById(pengirimanId)
                .orElseThrow(() -> new IllegalArgumentException("Pengiriman not found"));

        if (pengiriman.getStatus() != StatusPengiriman.APPROVED_MANDOR) {
            throw new IllegalStateException("Admin can only process shipments approved by mandor");
        }

        pengiriman.setStatus(StatusPengiriman.APPROVED_ADMIN);
        pengiriman.setRejectionReason(null);
        pengiriman.setAcknowledgedWeightKg(pengiriman.getTotalWeightKg());
        Pengiriman saved = pengirimanRepository.save(pengiriman);
        try {
            eventPublisher.publish("pengiriman-approved-admin", saved);
            eventPublisher.publish("payroll-mandor-requested",
                new id.ac.ui.cs.advprog.bemanagementpengiriman.events.PayrollEvent(
                    saved.getMandorId(),
                    "MANDOR",
                    saved.getId(),
                    saved.getAcknowledgedWeightKg(),
                    null));
        } catch (Exception ignored) {}
        return saved;
    }

    @Override
    @Transactional
    public Pengiriman rejectByAdmin(Long pengirimanId, Long adminId, String rejectionReason) {
        ensureUserExists(adminId, "Admin not found");

        Pengiriman pengiriman = pengirimanRepository.findById(pengirimanId)
                .orElseThrow(() -> new IllegalArgumentException("Pengiriman not found"));

        if (pengiriman.getStatus() != StatusPengiriman.APPROVED_MANDOR) {
            throw new IllegalStateException("Admin can only process shipments approved by mandor");
        }

        String validatedReason = requireReason(rejectionReason);

        pengiriman.setStatus(StatusPengiriman.REJECTED_ADMIN);
        pengiriman.setRejectionReason(validatedReason);
        pengiriman.setAcknowledgedWeightKg(null);
        Pengiriman saved = pengirimanRepository.save(pengiriman);
        try { eventPublisher.publish("pengiriman-rejected-admin", saved); } catch (Exception ignored) {}
        return saved;
    }

    @Override
    @Transactional
    public Pengiriman partialRejectByAdmin(Long pengirimanId,
                                           Long adminId,
                                           Double acknowledgedWeightKg,
                                           String rejectionReason) {
        ensureUserExists(adminId, "Admin not found");

        Pengiriman pengiriman = pengirimanRepository.findById(pengirimanId)
                .orElseThrow(() -> new IllegalArgumentException("Pengiriman not found"));

        if (pengiriman.getStatus() != StatusPengiriman.APPROVED_MANDOR) {
            throw new IllegalStateException("Admin can only process shipments approved by mandor");
        }
        if (acknowledgedWeightKg == null) {
            throw new IllegalArgumentException("Acknowledged weight is required for partial rejection");
        }
        if (acknowledgedWeightKg <= 0) {
            throw new IllegalArgumentException("Acknowledged weight must be greater than 0");
        }
        if (acknowledgedWeightKg >= pengiriman.getTotalWeightKg()) {
            throw new IllegalArgumentException("Acknowledged weight for partial rejection must be less than total shipment weight");
        }

        String validatedReason = requireReason(rejectionReason);

        pengiriman.setStatus(StatusPengiriman.PARTIALLY_REJECTED_ADMIN);
        pengiriman.setAcknowledgedWeightKg(acknowledgedWeightKg);
        pengiriman.setRejectionReason(validatedReason);
        Pengiriman saved = pengirimanRepository.save(pengiriman);
        try {
            eventPublisher.publish("pengiriman-partial-rejected-admin", saved);
            eventPublisher.publish("payroll-mandor-requested",
                new id.ac.ui.cs.advprog.bemanagementpengiriman.events.PayrollEvent(
                    saved.getMandorId(),
                    "MANDOR",
                    saved.getId(),
                    saved.getAcknowledgedWeightKg(),
                    null));
        } catch (Exception ignored) {}
        return saved;
    }

    @Override
    public Pengiriman getPengirimanById(Long pengirimanId) {
        return pengirimanRepository.findById(pengirimanId)
                .orElseThrow(() -> new IllegalArgumentException("Pengiriman not found"));
    }

    private void validateAssignDriverRequest(AssignDriverRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getDriverId() == null) {
            throw new IllegalArgumentException("Driver ID is required");
        }
        if (request.getHarvestItems() == null || request.getHarvestItems().isEmpty()) {
            throw new IllegalArgumentException("At least one harvest item is required");
        }
    }

    private UserSummary ensureUserExists(Long userId, String message) {
        if (userId == null) {
            throw new IllegalArgumentException(message);
        }
        return userClient.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requireReason(String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        return rejectionReason.trim();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after or equal to start date");
        }
    }

    private LocalDateTime toStartDateTime(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime toEndDateTime(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.MAX);
    }
}