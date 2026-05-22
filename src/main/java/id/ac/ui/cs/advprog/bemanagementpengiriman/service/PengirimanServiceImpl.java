package id.ac.ui.cs.advprog.bemanagementpengiriman.service;

import id.ac.ui.cs.advprog.bemanagementpengiriman.client.HarvestClient;
import id.ac.ui.cs.advprog.bemanagementpengiriman.client.KebunClient;
import id.ac.ui.cs.advprog.bemanagementpengiriman.client.PaymentClient;
import id.ac.ui.cs.advprog.bemanagementpengiriman.client.UserClient;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.AssignDriverRequest;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.HarvestTransportEligibilityResponse;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.KebunDetailResponse;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.MandorKebunAssignmentResponse;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PengirimanServiceImpl implements PengirimanService {

    private static final double MAX_WEIGHT_KG = 400.0;
    private static final String ROLE_MANDOR = "MANDOR";
    private static final String ROLE_SUPIR = "SUPIR";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final List<StatusPengiriman> ACTIVE_SHIPMENT_STATUSES = List.of(
            StatusPengiriman.MEMUAT,
            StatusPengiriman.MENGIRIM,
            StatusPengiriman.TIBA_DI_TUJUAN
    );
        private static final List<StatusPengiriman> SUPIR_HISTORY_STATUSES = List.of(
            StatusPengiriman.APPROVED_MANDOR,
            StatusPengiriman.REJECTED_MANDOR,
            StatusPengiriman.APPROVED_ADMIN,
            StatusPengiriman.REJECTED_ADMIN,
            StatusPengiriman.PARTIALLY_REJECTED_ADMIN
        );

    private final PengirimanRepository pengirimanRepository;
    private final UserClient userClient;
    private final HarvestClient harvestClient;
    private final KebunClient kebunClient;
    private final PaymentClient paymentClient;
    private final id.ac.ui.cs.advprog.bemanagementpengiriman.events.EventPublisher eventPublisher;

    @Override
    @Transactional
    @Retryable(
            retryFor = org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )    public Pengiriman assignDriver(Long mandorId, AssignDriverRequest request) {
        validateAssignDriverRequest(request);

        ensureUserHasRole(mandorId, ROLE_MANDOR, "Mandor not found");
        ensureUserHasRole(request.getDriverId(), ROLE_SUPIR, "Driver not found");
        String kebunCode = ensureDriverAssignedToMandorKebun(mandorId, request.getDriverId());

        double totalWeight = 0.0;
        Set<UUID> harvestIdsInRequest = new HashSet<>();
        Map<UUID, HarvestTransportEligibilityResponse> eligibilityByHarvestId = new LinkedHashMap<>();

        for (AssignDriverRequest.HarvestItemDto item : request.getHarvestItems()) {
            if (item == null) {
                throw new IllegalArgumentException("Harvest item cannot be null");
            }
            if (item.getHarvestId() == null) {
                throw new IllegalArgumentException("Harvest ID is required for each item");
            }
            if (!harvestIdsInRequest.add(item.getHarvestId())) {
                throw new IllegalArgumentException("Duplicate harvest item in request");
            }

            HarvestTransportEligibilityResponse eligibility = harvestClient.getTransportEligibility(item.getHarvestId())
                    .orElseThrow(() -> new IllegalArgumentException("Harvest item is not found"));
            if (!eligibility.eligible()) {
                throw new IllegalArgumentException("Harvest item is not approved");
            }
            if (eligibility.kilogram() == null || eligibility.kilogram().doubleValue() <= 0) {
                throw new IllegalArgumentException("Harvest item weight must be greater than 0");
            }
            eligibilityByHarvestId.put(item.getHarvestId(), eligibility);

            long activeShipmentCount = pengirimanRepository.countActiveShipmentByHarvestId(
                    item.getHarvestId(), ACTIVE_SHIPMENT_STATUSES);
            if (activeShipmentCount > 0) {
                throw new IllegalArgumentException(
                    "Harvest item already assigned to an active shipment");
            }

            totalWeight += eligibility.kilogram().doubleValue();
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
                .kebunCode(kebunCode)
                .status(StatusPengiriman.MEMUAT)
                .totalWeightKg(totalWeight)
                .items(new ArrayList<>())
                .build();

        // Create pengiriman items
        for (AssignDriverRequest.HarvestItemDto item : request.getHarvestItems()) {
            HarvestTransportEligibilityResponse eligibility = eligibilityByHarvestId.get(item.getHarvestId());
            PengirimanItem pengirimanItem = PengirimanItem.builder()
                    .shipment(pengiriman)
                    .harvestId(item.getHarvestId())
                    .weightKg(eligibility.kilogram().doubleValue())
                    .build();
            pengiriman.getItems().add(pengirimanItem);
        }

        Pengiriman saved = pengirimanRepository.save(pengiriman);
        publishEvent("pengiriman-assigned", saved);
        return saved;
    }

    @Override
    @Transactional
    public Pengiriman updateStatusPengiriman(Long pengirimanId, Long driverId, StatusPengiriman newStatus) {
        ensureUserHasRole(driverId, ROLE_SUPIR, "Driver not found");
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
        ensureUserHasRole(driverId, ROLE_SUPIR, "Driver not found");
        return pengirimanRepository.findByDriverIdAndStatusIn(driverId, ACTIVE_SHIPMENT_STATUSES);
    }

    @Override
    public List<Pengiriman> getPengirimanByDriverForMandor(Long mandorId, Long driverId) {
        ensureUserHasRole(mandorId, ROLE_MANDOR, "Mandor not found");

        return pengirimanRepository.findByMandorIdAndDriverIdAndStatusIn(
                mandorId,
                driverId,
                ACTIVE_SHIPMENT_STATUSES
        );
    }

    @Override
    public List<Pengiriman> getPengirimanHistoryByDriver(Long driverId, LocalDate startDate, LocalDate endDate) {
        ensureUserHasRole(driverId, ROLE_SUPIR, "Driver not found");

        validateDateRange(startDate, endDate);

        return findDriverHistory(driverId, toStartDateTime(startDate), toEndDateTime(endDate));
    }

    @Override
    public List<Pengiriman> getOngoingPengiriman(Long mandorId) {
        ensureUserHasRole(mandorId, ROLE_MANDOR, "Mandor not found");
        return pengirimanRepository.findByMandorIdAndStatusIn(mandorId, ACTIVE_SHIPMENT_STATUSES);
    }

    @Override
    public List<Pengiriman> getPengirimanByStatus(StatusPengiriman status) {
        return pengirimanRepository.findByStatus(status);
    }

    @Override
    public List<Pengiriman> getApprovedPengirimanForAdmin(Long adminId, String mandorName, LocalDate date) {
        ensureUserHasRole(adminId, ROLE_ADMIN, "Admin not found");

        String normalizedMandorName = normalizeName(mandorName);
        List<Long> mandorIds = null;
        if (normalizedMandorName != null) {
            List<UserSummary> matches = userClient.findByNameAndRole(normalizedMandorName, ROLE_MANDOR);
            if (matches.isEmpty()) {
                return List.of();
            }
            mandorIds = matches.stream().map(UserSummary::getId).toList();
        }

        LocalDateTime startDate = date == null ? null : date.atStartOfDay();
        LocalDateTime endDate = date == null ? null : date.atTime(LocalTime.MAX);

        return findApprovedMandorShipments(mandorIds, startDate, endDate);
    }

    @Override
    public List<UserSummary> getAvailableDriversForMandor(Long mandorId, String searchName) {
        UserSummary mandor = ensureUserHasRole(mandorId, ROLE_MANDOR, "Mandor not found");
        KebunDetailResponse kebunDetail = getActiveMandorKebunDetail(mandorId);
        Set<String> assignedSupirIds = new HashSet<>(kebunDetail.supirIds() == null
                ? List.of()
                : kebunDetail.supirIds());

        List<UserSummary> users;
        if (searchName == null || searchName.isBlank()) {
            users = userClient.findByRole(ROLE_SUPIR);
        } else {
            users = userClient.findByNameAndRole(searchName.trim(), ROLE_SUPIR);
        }

        return users.stream()
            .filter(user -> !user.getId().equals(mandor.getId()))
                .filter(user -> assignedSupirIds.contains(String.valueOf(user.getId())))
                .toList();
    }

    @Override
    @Transactional
    public Pengiriman approveByMandor(Long pengirimanId, Long mandorId) {
        ensureUserHasRole(mandorId, ROLE_MANDOR, "Mandor not found");

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
        publishEvent("pengiriman-approved-mandor", saved);
        publishEvent("payroll-driver-requested",
                new id.ac.ui.cs.advprog.bemanagementpengiriman.events.PayrollEvent(
                    saved.getDriverId(),
                    ROLE_SUPIR,
                    saved.getTotalWeightKg()));
        requestPayroll(mandorId, saved.getDriverId(), ROLE_SUPIR, saved.getTotalWeightKg());
        return saved;
    }

    @Override
    @Transactional
    public Pengiriman rejectByMandor(Long pengirimanId, Long mandorId, String rejectionReason) {
        ensureUserHasRole(mandorId, ROLE_MANDOR, "Mandor not found");

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
        publishEvent("pengiriman-rejected-mandor", saved);
        return saved;
    }

    @Override
    @Transactional
    public Pengiriman approveByAdmin(Long pengirimanId, Long adminId) {
        ensureUserHasRole(adminId, ROLE_ADMIN, "Admin not found");

        Pengiriman pengiriman = pengirimanRepository.findById(pengirimanId)
                .orElseThrow(() -> new IllegalArgumentException("Pengiriman not found"));

        if (pengiriman.getStatus() != StatusPengiriman.APPROVED_MANDOR) {
            throw new IllegalStateException("Admin can only process shipments approved by mandor");
        }

        pengiriman.setStatus(StatusPengiriman.APPROVED_ADMIN);
        pengiriman.setRejectionReason(null);
        pengiriman.setAcknowledgedWeightKg(pengiriman.getTotalWeightKg());
        Pengiriman saved = pengirimanRepository.save(pengiriman);
        publishEvent("pengiriman-approved-admin", saved);
        publishEvent("payroll-mandor-requested",
                new id.ac.ui.cs.advprog.bemanagementpengiriman.events.PayrollEvent(
                    saved.getMandorId(),
                    ROLE_MANDOR,
                    saved.getAcknowledgedWeightKg()));
        requestPayroll(adminId, saved.getMandorId(), ROLE_MANDOR, saved.getAcknowledgedWeightKg());
        return saved;
    }

    @Override
    @Transactional
    public Pengiriman rejectByAdmin(Long pengirimanId, Long adminId, String rejectionReason) {
        ensureUserHasRole(adminId, ROLE_ADMIN, "Admin not found");

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
        publishEvent("pengiriman-rejected-admin", saved);
        return saved;
    }

    @Override
    @Transactional
    public Pengiriman partialRejectByAdmin(Long pengirimanId,
                                           Long adminId,
                                           Double acknowledgedWeightKg,
                                           String rejectionReason) {
        ensureUserHasRole(adminId, ROLE_ADMIN, "Admin not found");

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
        publishEvent("pengiriman-partial-rejected-admin", saved);
        publishEvent("payroll-mandor-requested",
                new id.ac.ui.cs.advprog.bemanagementpengiriman.events.PayrollEvent(
                    saved.getMandorId(),
                    ROLE_MANDOR,
                    saved.getAcknowledgedWeightKg()));
        requestPayroll(adminId, saved.getMandorId(), ROLE_MANDOR, saved.getAcknowledgedWeightKg());
        return saved;
    }

    @Override
    public Pengiriman getPengirimanById(Long pengirimanId) {
        return pengirimanRepository.findById(pengirimanId)
                .orElseThrow(() -> new IllegalArgumentException("Pengiriman not found"));
    }

    @Override
    public Pengiriman getPengirimanByIdForUser(Long pengirimanId, Long userId, String role) {
        Pengiriman pengiriman = getPengirimanById(pengirimanId);
        if (isRole(role, ROLE_ADMIN)) {
            ensureUserHasRole(userId, ROLE_ADMIN, "Admin not found");
            return pengiriman;
        }
        if (isRole(role, ROLE_MANDOR)) {
            ensureUserHasRole(userId, ROLE_MANDOR, "Mandor not found");
            if (!pengiriman.getMandorId().equals(userId)) {
                throw new SecurityException("Mandor is not assigned to this pengiriman");
            }
            return pengiriman;
        }
        if (isRole(role, ROLE_SUPIR)) {
            ensureUserHasRole(userId, ROLE_SUPIR, "Driver not found");
            if (!pengiriman.getDriverId().equals(userId)) {
                throw new SecurityException("Supir is not assigned to this pengiriman");
            }
            return pengiriman;
        }
        throw new SecurityException("Only ADMIN, MANDOR, or SUPIR can access this endpoint");
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

    private UserSummary ensureUserHasRole(Long userId, String expectedRole, String message) {
        UserSummary user = ensureUserExists(userId, message);
        if (user.getRole() == null || !user.getRole().equalsIgnoreCase(expectedRole)) {
            throw new IllegalArgumentException(message);
        }
        return user;
    }

    private boolean isRole(String actualRole, String expectedRole) {
        return actualRole != null && actualRole.equalsIgnoreCase(expectedRole);
    }

    private String ensureDriverAssignedToMandorKebun(Long mandorId, Long driverId) {
        KebunDetailResponse kebunDetail = getActiveMandorKebunDetail(mandorId);
        boolean driverAssignedToSameKebun = kebunDetail.supirIds() != null
                && kebunDetail.supirIds().contains(String.valueOf(driverId));
        if (!driverAssignedToSameKebun) {
            throw new SecurityException("Driver is not assigned to the same kebun as mandor");
        }
        return kebunDetail.code();
    }

    private KebunDetailResponse getActiveMandorKebunDetail(Long mandorId) {
        MandorKebunAssignmentResponse assignment = kebunClient.getMandorKebunAssignment(mandorId)
                .orElseThrow(() -> new IllegalArgumentException("Mandor kebun assignment not found"));
        if (!assignment.active() || assignment.kebunCode() == null || assignment.kebunCode().isBlank()) {
            throw new IllegalStateException("Mandor is not assigned to a kebun");
        }
        return kebunClient.getKebunDetail(assignment.kebunCode())
                .orElseThrow(() -> new IllegalArgumentException("Kebun detail not found"));
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

    private void publishEvent(String topic, Object event) {
        try {
            eventPublisher.publish(topic, event);
        } catch (Exception ignored) {
            // Best-effort integration event; domain state has already been persisted.
        }
    }

    private void requestPayroll(Long actorId, Long userId, String role, Double kilogram) {
        try {
            paymentClient.requestPayroll(actorId, userId, role, kilogram);
        } catch (Exception ignored) {
            // Payroll creation is an integration side effect and should not roll back shipment approval.
        }
    }

    private LocalDateTime toStartDateTime(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime toEndDateTime(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.MAX);
    }

    private List<Pengiriman> findDriverHistory(Long driverId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null) {
            return pengirimanRepository.findByDriverIdAndStatusInAndUpdatedAtBetweenOrderByUpdatedAtDesc(
                    driverId,
                    SUPIR_HISTORY_STATUSES,
                    startDate,
                    endDate
            );
        }
        if (startDate != null) {
            return pengirimanRepository.findByDriverIdAndStatusInAndUpdatedAtGreaterThanEqualOrderByUpdatedAtDesc(
                    driverId,
                    SUPIR_HISTORY_STATUSES,
                    startDate
            );
        }
        if (endDate != null) {
            return pengirimanRepository.findByDriverIdAndStatusInAndUpdatedAtLessThanEqualOrderByUpdatedAtDesc(
                    driverId,
                    SUPIR_HISTORY_STATUSES,
                    endDate
            );
        }
        return pengirimanRepository.findByDriverIdAndStatusInOrderByUpdatedAtDesc(driverId, SUPIR_HISTORY_STATUSES);
    }

    private List<Pengiriman> findApprovedMandorShipments(
            List<Long> mandorIds,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        boolean hasMandorFilter = mandorIds != null && !mandorIds.isEmpty();
        boolean hasDateFilter = startDate != null && endDate != null;

        if (hasMandorFilter && hasDateFilter) {
            return pengirimanRepository.findByStatusAndMandorIdInAndUpdatedAtBetweenOrderByUpdatedAtDesc(
                    StatusPengiriman.APPROVED_MANDOR,
                    mandorIds,
                    startDate,
                    endDate
            );
        }
        if (hasMandorFilter) {
            return pengirimanRepository.findByStatusAndMandorIdInOrderByUpdatedAtDesc(
                    StatusPengiriman.APPROVED_MANDOR,
                    mandorIds
            );
        }
        if (hasDateFilter) {
            return pengirimanRepository.findByStatusAndUpdatedAtBetweenOrderByUpdatedAtDesc(
                    StatusPengiriman.APPROVED_MANDOR,
                    startDate,
                    endDate
            );
        }
        return pengirimanRepository.findByStatusOrderByUpdatedAtDesc(StatusPengiriman.APPROVED_MANDOR);
    }
}
