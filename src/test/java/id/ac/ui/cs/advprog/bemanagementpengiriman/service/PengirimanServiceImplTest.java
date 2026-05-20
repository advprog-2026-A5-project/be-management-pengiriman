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
import id.ac.ui.cs.advprog.bemanagementpengiriman.events.EventPublisher;
import id.ac.ui.cs.advprog.bemanagementpengiriman.events.PayrollEvent;
import id.ac.ui.cs.advprog.bemanagementpengiriman.model.Pengiriman;
import id.ac.ui.cs.advprog.bemanagementpengiriman.repository.PengirimanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PengirimanServiceImplTest {

    @Mock
    private PengirimanRepository pengirimanRepository;

    @Mock
    private UserClient userClient;

        @Mock
        private HarvestClient harvestClient;

        @Mock
        private KebunClient kebunClient;

        @Mock
        private EventPublisher eventPublisher;

        @Mock
        private PaymentClient paymentClient;

    @InjectMocks
    private PengirimanServiceImpl pengirimanService;

    private static UserSummary user(Long id, String name, String role) {
        return new UserSummary(id, name, name + "@example.com", name, role);
    }

    @BeforeEach
    void setUpKebunDefaults() {
        lenient().when(kebunClient.getMandorKebunAssignment(1L))
                .thenReturn(Optional.of(assignment(1L, "KB001", true)));
        lenient().when(kebunClient.getKebunDetail("KB001"))
                .thenReturn(Optional.of(kebunDetail("KB001", "1", List.of("2"))));
    }

    private static MandorKebunAssignmentResponse assignment(Long mandorId, String kebunCode, boolean active) {
        return new MandorKebunAssignmentResponse(mandorId, null, kebunCode, "Kebun A", active);
    }

    private static KebunDetailResponse kebunDetail(String code, String mandorId, List<String> supirIds) {
        return new KebunDetailResponse(code, "Kebun A", 10.0, List.of(), mandorId, supirIds);
    }

    private static HarvestTransportEligibilityResponse eligible(UUID harvestId, double kilogram) {
        return new HarvestTransportEligibilityResponse(
                harvestId,
                true,
                "APPROVED",
                BigDecimal.valueOf(kilogram)
        );
    }

    private static HarvestTransportEligibilityResponse ineligible(UUID harvestId) {
        return new HarvestTransportEligibilityResponse(
                harvestId,
                false,
                "REJECTED",
                BigDecimal.valueOf(100.0)
        );
    }

    @Test
    void assignDriver_shouldRejectEmptyHarvestItems() {
        AssignDriverRequest request = AssignDriverRequest.builder()
                .driverId(2L)
                .harvestItems(List.of())
                .build();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pengirimanService.assignDriver(1L, request)
        );

        assertTrue(ex.getMessage().contains("At least one harvest item is required"));
        verifyNoInteractions(userClient);
        verifyNoInteractions(pengirimanRepository);
    }

    @Test
    void assignDriver_shouldRejectDuplicateHarvestItemInRequest() {
        UUID duplicateHarvestId = UUID.randomUUID();

        when(userClient.findById(1L)).thenReturn(Optional.of(user(1L, "mandor", "MANDOR")));
        when(userClient.findById(2L)).thenReturn(Optional.of(user(2L, "supir", "SUPIR")));
        when(harvestClient.getTransportEligibility(duplicateHarvestId))
                .thenReturn(Optional.of(eligible(duplicateHarvestId, 100.0)));
        when(pengirimanRepository.countActiveShipmentByHarvestId(eq(duplicateHarvestId), anyCollection()))
                .thenReturn(0L);

        AssignDriverRequest request = AssignDriverRequest.builder()
                .driverId(2L)
                .harvestItems(List.of(
                        new AssignDriverRequest.HarvestItemDto(duplicateHarvestId),
                        new AssignDriverRequest.HarvestItemDto(duplicateHarvestId)
                ))
                .build();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pengirimanService.assignDriver(1L, request)
        );

        assertTrue(ex.getMessage().contains("Duplicate harvest item"));
    }

    @Test
    void assignDriver_shouldRejectAlreadyAssignedHarvestItem() {
        UUID harvestId = UUID.randomUUID();

        when(userClient.findById(1L)).thenReturn(Optional.of(user(1L, "mandor", "MANDOR")));
        when(userClient.findById(2L)).thenReturn(Optional.of(user(2L, "supir", "SUPIR")));
        when(harvestClient.getTransportEligibility(harvestId))
                .thenReturn(Optional.of(eligible(harvestId, 120.0)));
        when(pengirimanRepository.countActiveShipmentByHarvestId(eq(harvestId), anyCollection()))
                .thenReturn(1L);

        AssignDriverRequest request = AssignDriverRequest.builder()
                .driverId(2L)
                .harvestItems(List.of(new AssignDriverRequest.HarvestItemDto(harvestId)))
                .build();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pengirimanService.assignDriver(1L, request)
        );

        assertTrue(ex.getMessage().contains("already assigned to an active shipment"));
    }

    @Test
    void assignDriver_shouldRejectWhenMandorHasNoActiveKebun() {
        UUID harvestId = UUID.randomUUID();
        AssignDriverRequest request = AssignDriverRequest.builder()
                .driverId(2L)
                .harvestItems(List.of(new AssignDriverRequest.HarvestItemDto(harvestId)))
                .build();

        when(userClient.findById(1L)).thenReturn(Optional.of(user(1L, "mandor", "MANDOR")));
        when(userClient.findById(2L)).thenReturn(Optional.of(user(2L, "supir", "SUPIR")));
        when(kebunClient.getMandorKebunAssignment(1L))
                .thenReturn(Optional.of(assignment(1L, null, false)));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> pengirimanService.assignDriver(1L, request)
        );

        assertEquals("Mandor is not assigned to a kebun", ex.getMessage());
        verifyNoInteractions(harvestClient);
        verify(pengirimanRepository, never()).save(any(Pengiriman.class));
    }

    @Test
    void assignDriver_shouldRejectWhenDriverIsNotInSameKebun() {
        UUID harvestId = UUID.randomUUID();
        AssignDriverRequest request = AssignDriverRequest.builder()
                .driverId(2L)
                .harvestItems(List.of(new AssignDriverRequest.HarvestItemDto(harvestId)))
                .build();

        when(userClient.findById(1L)).thenReturn(Optional.of(user(1L, "mandor", "MANDOR")));
        when(userClient.findById(2L)).thenReturn(Optional.of(user(2L, "supir", "SUPIR")));
        when(kebunClient.getKebunDetail("KB001"))
                .thenReturn(Optional.of(kebunDetail("KB001", "1", List.of("3"))));

        SecurityException ex = assertThrows(
                SecurityException.class,
                () -> pengirimanService.assignDriver(1L, request)
        );

        assertEquals("Driver is not assigned to the same kebun as mandor", ex.getMessage());
        verifyNoInteractions(harvestClient);
        verify(pengirimanRepository, never()).save(any(Pengiriman.class));
    }

    @Test
    void getPengirimanByDriver_shouldUseActiveStatusesOnly() {
        List<Pengiriman> expected = List.of(Pengiriman.builder().id(1L).build());
        when(userClient.findById(2L)).thenReturn(Optional.of(user(2L, "supir", "SUPIR")));
        when(pengirimanRepository.findByDriverIdAndStatusIn(eq(2L), anyCollection())).thenReturn(expected);

        List<Pengiriman> result = pengirimanService.getPengirimanByDriver(2L);

        assertEquals(expected, result);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<StatusPengiriman>> statusesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(pengirimanRepository).findByDriverIdAndStatusIn(eq(2L), statusesCaptor.capture());

        Collection<StatusPengiriman> statuses = statusesCaptor.getValue();
        assertTrue(statuses.contains(StatusPengiriman.MEMUAT));
        assertTrue(statuses.contains(StatusPengiriman.MENGIRIM));
        assertTrue(statuses.contains(StatusPengiriman.TIBA_DI_TUJUAN));
    }

    @Test
    void getAvailableDriversForMandor_shouldFilterByNameAndExcludeMandorSelf() {
        UserSummary mandor = user(1L, "mandor.satu", "MANDOR");
        UserSummary driverMatch = user(2L, "supir.rifky", "SUPIR");
        UserSummary sameMandorFromResult = user(1L, "mandor.satu", "MANDOR");

        when(userClient.findById(1L)).thenReturn(Optional.of(mandor));
        when(userClient.findByNameAndRole("ri", "SUPIR"))
                .thenReturn(List.of(driverMatch, sameMandorFromResult));

        List<UserSummary> result = pengirimanService.getAvailableDriversForMandor(1L, "ri");

        assertEquals(1, result.size());
        assertEquals(2L, result.getFirst().getId());
        assertEquals("supir.rifky", result.getFirst().getUsername());
    }

    @Test
    void approveByMandor_shouldSetApprovedStatusAndAcknowledgedWeight() {
        UserSummary mandor = user(1L, "mandor.satu", "MANDOR");
        Pengiriman pengiriman = Pengiriman.builder()
                .id(10L)
                                .driverId(2L)
                .mandorId(1L)
                .status(StatusPengiriman.TIBA_DI_TUJUAN)
                .totalWeightKg(250.0)
                .build();

        when(userClient.findById(1L)).thenReturn(Optional.of(mandor));
        when(pengirimanRepository.findById(10L)).thenReturn(Optional.of(pengiriman));
        when(pengirimanRepository.save(any(Pengiriman.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pengiriman result = pengirimanService.approveByMandor(10L, 1L);

        assertEquals(StatusPengiriman.APPROVED_MANDOR, result.getStatus());
        assertEquals(250.0, result.getAcknowledgedWeightKg());
        assertNull(result.getRejectionReason());

                ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
                verify(eventPublisher, times(2)).publish(any(), eventCaptor.capture());
                assertTrue(eventCaptor.getAllValues().stream().anyMatch(ev -> ev instanceof PayrollEvent));
                verify(paymentClient).requestPayroll(1L, 2L, "SUPIR", 250.0);
    }

    @Test
    void rejectByMandor_shouldRequireReason() {
        UserSummary mandor = user(1L, "mandor.satu", "MANDOR");
        Pengiriman pengiriman = Pengiriman.builder()
                .id(10L)
                .mandorId(1L)
                .status(StatusPengiriman.TIBA_DI_TUJUAN)
                .totalWeightKg(250.0)
                .build();

        when(userClient.findById(1L)).thenReturn(Optional.of(mandor));
        when(pengirimanRepository.findById(10L)).thenReturn(Optional.of(pengiriman));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pengirimanService.rejectByMandor(10L, 1L, "  ")
        );

        assertEquals("Rejection reason is required", ex.getMessage());
        verify(pengirimanRepository, never()).save(any());
    }

    @Test
    void getPengirimanHistoryByDriver_shouldRejectInvalidDateRange() {
        when(userClient.findById(2L)).thenReturn(Optional.of(user(2L, "supir", "SUPIR")));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pengirimanService.getPengirimanHistoryByDriver(
                        2L,
                        LocalDate.of(2026, 4, 20),
                        LocalDate.of(2026, 4, 19)
                )
        );

        assertEquals("End date must be after or equal to start date", ex.getMessage());
        verify(pengirimanRepository, never()).findDriverHistory(any(), any(), any(), any());
    }

    @Test
    void partialRejectByAdmin_shouldRejectWhenAcknowledgedWeightIsNotPartial() {
        UserSummary admin = user(99L, "admin", "ADMIN");
        Pengiriman pengiriman = Pengiriman.builder()
                .id(20L)
                .status(StatusPengiriman.APPROVED_MANDOR)
                .totalWeightKg(300.0)
                .build();

        when(userClient.findById(99L)).thenReturn(Optional.of(admin));
        when(pengirimanRepository.findById(20L)).thenReturn(Optional.of(pengiriman));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pengirimanService.partialRejectByAdmin(20L, 99L, 300.0, "Sebagian rusak")
        );

        assertEquals("Acknowledged weight for partial rejection must be less than total shipment weight", ex.getMessage());
        verify(pengirimanRepository, never()).save(any());
    }

    @Test
    void assignDriver_shouldCreateShipmentAndItemsWhenValid() {
        UserSummary mandor = user(1L, "mandor", "MANDOR");
        UserSummary driver = user(2L, "supir", "SUPIR");
        UUID harvestId1 = UUID.randomUUID();
        UUID harvestId2 = UUID.randomUUID();

        when(userClient.findById(1L)).thenReturn(Optional.of(mandor));
        when(userClient.findById(2L)).thenReturn(Optional.of(driver));
        when(harvestClient.getTransportEligibility(harvestId1))
                .thenReturn(Optional.of(eligible(harvestId1, 100.0)));
        when(harvestClient.getTransportEligibility(harvestId2))
                .thenReturn(Optional.of(eligible(harvestId2, 120.0)));
        when(pengirimanRepository.countActiveShipmentByHarvestId(eq(harvestId1), anyCollection())).thenReturn(0L);
        when(pengirimanRepository.countActiveShipmentByHarvestId(eq(harvestId2), anyCollection())).thenReturn(0L);
        when(pengirimanRepository.save(any(Pengiriman.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssignDriverRequest request = AssignDriverRequest.builder()
                .driverId(2L)
                .harvestItems(List.of(
                        new AssignDriverRequest.HarvestItemDto(harvestId1),
                        new AssignDriverRequest.HarvestItemDto(harvestId2)
                ))
                .build();

        Pengiriman result = pengirimanService.assignDriver(1L, request);

        assertEquals(StatusPengiriman.MEMUAT, result.getStatus());
        assertEquals("KB001", result.getKebunCode());
        assertEquals(220.0, result.getTotalWeightKg());
        assertEquals(2, result.getItems().size());
        assertTrue(result.getItems().stream().allMatch(item -> item.getShipment() == result));
        verify(pengirimanRepository).save(any(Pengiriman.class));
    }

    @Test
    void assignDriver_shouldRejectNullRequest() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pengirimanService.assignDriver(1L, null)
        );

        assertEquals("Request body is required", ex.getMessage());
        verifyNoInteractions(userClient);
        verifyNoInteractions(pengirimanRepository);
    }

    @Test
    void assignDriver_shouldRejectWhenDriverNotFound() {
        UUID harvestId = UUID.randomUUID();
        AssignDriverRequest request = AssignDriverRequest.builder()
                .driverId(2L)
                .harvestItems(List.of(new AssignDriverRequest.HarvestItemDto(harvestId)))
                .build();

        when(userClient.findById(1L)).thenReturn(Optional.of(user(1L, "mandor", "MANDOR")));
        when(userClient.findById(2L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pengirimanService.assignDriver(1L, request)
        );

        assertEquals("Driver not found", ex.getMessage());
        verify(pengirimanRepository, never()).save(any(Pengiriman.class));
    }

    @Test
    void assignDriver_shouldRejectItemWeightLessThanOrEqualZero() {
        UUID harvestId = UUID.randomUUID();
        AssignDriverRequest request = AssignDriverRequest.builder()
                .driverId(2L)
                .harvestItems(List.of(new AssignDriverRequest.HarvestItemDto(harvestId)))
                .build();

        when(userClient.findById(1L)).thenReturn(Optional.of(user(1L, "mandor", "MANDOR")));
        when(userClient.findById(2L)).thenReturn(Optional.of(user(2L, "supir", "SUPIR")));
        when(harvestClient.getTransportEligibility(harvestId))
                .thenReturn(Optional.of(new HarvestTransportEligibilityResponse(
                        harvestId,
                        true,
                        "APPROVED",
                        BigDecimal.ZERO
                )));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pengirimanService.assignDriver(1L, request)
        );

        assertEquals("Harvest item weight must be greater than 0", ex.getMessage());
        verify(pengirimanRepository, never()).save(any(Pengiriman.class));
    }

        @Test
        void assignDriver_shouldRejectWhenHarvestNotApproved() {
                UUID harvestId = UUID.randomUUID();
                AssignDriverRequest request = AssignDriverRequest.builder()
                                .driverId(2L)
                                .harvestItems(List.of(new AssignDriverRequest.HarvestItemDto(harvestId)))
                                .build();

                when(userClient.findById(1L)).thenReturn(Optional.of(user(1L, "mandor", "MANDOR")));
                when(userClient.findById(2L)).thenReturn(Optional.of(user(2L, "supir", "SUPIR")));
                when(harvestClient.getTransportEligibility(harvestId))
                        .thenReturn(Optional.of(ineligible(harvestId)));

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> pengirimanService.assignDriver(1L, request)
                );

                assertEquals("Harvest item is not approved", ex.getMessage());
                verify(pengirimanRepository, never()).save(any(Pengiriman.class));
        }

    @Test
    void updateStatusPengiriman_shouldUpdateWhenTransitionIsValid() {
        Pengiriman pengiriman = Pengiriman.builder()
                .id(10L)
                .driverId(2L)
                .status(StatusPengiriman.MEMUAT)
                .build();

        when(userClient.findById(2L)).thenReturn(Optional.of(user(2L, "supir", "SUPIR")));
        when(pengirimanRepository.findById(10L)).thenReturn(Optional.of(pengiriman));
        when(pengirimanRepository.save(any(Pengiriman.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pengiriman result = pengirimanService.updateStatusPengiriman(10L, 2L, StatusPengiriman.MENGIRIM);

        assertEquals(StatusPengiriman.MENGIRIM, result.getStatus());
        verify(pengirimanRepository).save(pengiriman);
    }

    @Test
    void updateStatusPengiriman_shouldRejectNullStatus() {
        when(userClient.findById(2L)).thenReturn(Optional.of(user(2L, "supir", "SUPIR")));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pengirimanService.updateStatusPengiriman(10L, 2L, null)
        );

        assertEquals("New status is required", ex.getMessage());
        verifyNoInteractions(pengirimanRepository);
    }

    @Test
    void updateStatusPengiriman_shouldRejectWhenDriverNotAssigned() {
        Pengiriman pengiriman = Pengiriman.builder()
                .id(10L)
                .driverId(3L)
                .status(StatusPengiriman.MEMUAT)
                .build();

        when(userClient.findById(2L)).thenReturn(Optional.of(user(2L, "supir", "SUPIR")));
        when(pengirimanRepository.findById(10L)).thenReturn(Optional.of(pengiriman));

        SecurityException ex = assertThrows(
                SecurityException.class,
                () -> pengirimanService.updateStatusPengiriman(10L, 2L, StatusPengiriman.MENGIRIM)
        );

        assertEquals("Driver is not assigned to this pengiriman", ex.getMessage());
        verify(pengirimanRepository, never()).save(any(Pengiriman.class));
    }

    @Test
    void updateStatusPengiriman_shouldRejectInvalidTransition() {
        Pengiriman pengiriman = Pengiriman.builder()
                .id(10L)
                .driverId(2L)
                .status(StatusPengiriman.MEMUAT)
                .build();

        when(userClient.findById(2L)).thenReturn(Optional.of(user(2L, "supir", "SUPIR")));
        when(pengirimanRepository.findById(10L)).thenReturn(Optional.of(pengiriman));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> pengirimanService.updateStatusPengiriman(10L, 2L, StatusPengiriman.TIBA_DI_TUJUAN)
        );

        assertEquals("Cannot transition from MEMUAT to TIBA_DI_TUJUAN", ex.getMessage());
        verify(pengirimanRepository, never()).save(any(Pengiriman.class));
    }

    @Test
    void getPengirimanByDriverForMandor_shouldReturnDriverShipmentsInActiveStatuses() {
        when(userClient.findById(1L)).thenReturn(Optional.of(user(1L, "mandor", "MANDOR")));

        List<Pengiriman> expected = List.of(Pengiriman.builder().id(30L).build());
        when(pengirimanRepository.findByMandorIdAndDriverIdAndStatusIn(eq(1L), eq(2L), anyCollection()))
                .thenReturn(expected);

        List<Pengiriman> result = pengirimanService.getPengirimanByDriverForMandor(1L, 2L);

        assertEquals(expected, result);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<StatusPengiriman>> statusesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(pengirimanRepository).findByMandorIdAndDriverIdAndStatusIn(eq(1L), eq(2L), statusesCaptor.capture());
        assertTrue(statusesCaptor.getValue().contains(StatusPengiriman.MEMUAT));
        assertTrue(statusesCaptor.getValue().contains(StatusPengiriman.MENGIRIM));
        assertTrue(statusesCaptor.getValue().contains(StatusPengiriman.TIBA_DI_TUJUAN));
    }

    @Test
    void getPengirimanByDriverForMandor_shouldRejectUnknownMandor() {
        when(userClient.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pengirimanService.getPengirimanByDriverForMandor(1L, 2L)
        );

        assertEquals("Mandor not found", ex.getMessage());
        verifyNoInteractions(pengirimanRepository);
    }

    @Test
    void getPengirimanHistoryByDriver_shouldConvertDateRangeToDateTimeBounds() {
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 30);

        when(userClient.findById(2L)).thenReturn(Optional.of(user(2L, "supir", "SUPIR")));
        when(pengirimanRepository.findDriverHistory(eq(2L), anyCollection(), any(), any()))
                .thenReturn(List.of(Pengiriman.builder().id(100L).build()));

        List<Pengiriman> result = pengirimanService.getPengirimanHistoryByDriver(2L, startDate, endDate);

        assertEquals(1, result.size());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<StatusPengiriman>> statusesCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<LocalDateTime> startDateTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endDateTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(pengirimanRepository).findDriverHistory(
                eq(2L),
                statusesCaptor.capture(),
                startDateTimeCaptor.capture(),
                endDateTimeCaptor.capture()
        );

        assertEquals(startDate.atStartOfDay(), startDateTimeCaptor.getValue());
        assertEquals(endDate.atTime(LocalTime.MAX), endDateTimeCaptor.getValue());
        assertTrue(statusesCaptor.getValue().contains(StatusPengiriman.APPROVED_MANDOR));
        assertTrue(statusesCaptor.getValue().contains(StatusPengiriman.PARTIALLY_REJECTED_ADMIN));
    }

    @Test
    void getPengirimanHistoryByDriver_shouldRejectUnknownDriver() {
        when(userClient.findById(2L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pengirimanService.getPengirimanHistoryByDriver(2L, null, null)
        );

        assertEquals("Driver not found", ex.getMessage());
        verify(pengirimanRepository, never()).findDriverHistory(any(), anyCollection(), any(), any());
    }

    @Test
    void getOngoingPengiriman_shouldQueryUsingActiveStatuses() {
        List<Pengiriman> expected = List.of(Pengiriman.builder().id(200L).build());
        when(userClient.findById(1L)).thenReturn(Optional.of(user(1L, "mandor", "MANDOR")));
        when(pengirimanRepository.findByMandorIdAndStatusIn(eq(1L), anyCollection())).thenReturn(expected);

        List<Pengiriman> result = pengirimanService.getOngoingPengiriman(1L);

        assertEquals(expected, result);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<StatusPengiriman>> statusesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(pengirimanRepository).findByMandorIdAndStatusIn(eq(1L), statusesCaptor.capture());
        assertTrue(statusesCaptor.getValue().contains(StatusPengiriman.MEMUAT));
        assertTrue(statusesCaptor.getValue().contains(StatusPengiriman.MENGIRIM));
        assertTrue(statusesCaptor.getValue().contains(StatusPengiriman.TIBA_DI_TUJUAN));
    }

    @Test
    void getPengirimanByStatus_shouldDelegateToRepository() {
        List<Pengiriman> expected = List.of(Pengiriman.builder().id(201L).build());
        when(pengirimanRepository.findByStatus(StatusPengiriman.MENGIRIM)).thenReturn(expected);

        List<Pengiriman> result = pengirimanService.getPengirimanByStatus(StatusPengiriman.MENGIRIM);

        assertEquals(expected, result);
        verify(pengirimanRepository).findByStatus(StatusPengiriman.MENGIRIM);
    }

    @Test
    void getApprovedPengirimanForAdmin_shouldNormalizeMandorNameAndDate() {
        LocalDate date = LocalDate.of(2026, 4, 17);
        UserSummary admin = user(99L, "admin", "ADMIN");
        UserSummary mandor = user(10L, "mandor.satu", "MANDOR");

        when(userClient.findById(99L)).thenReturn(Optional.of(admin));
        when(userClient.findByNameAndRole("man", "MANDOR"))
                .thenReturn(List.of(mandor));
        when(pengirimanRepository.findForAdminApproval(eq(StatusPengiriman.APPROVED_MANDOR), anyCollection(), any(), any()))
                .thenReturn(List.of(Pengiriman.builder().id(300L).build()));

        List<Pengiriman> result = pengirimanService.getApprovedPengirimanForAdmin(99L, "  man  ", date);

        assertEquals(1, result.size());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> mandorIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<LocalDateTime> startDateTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endDateTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(pengirimanRepository).findForAdminApproval(
                eq(StatusPengiriman.APPROVED_MANDOR),
                mandorIdsCaptor.capture(),
                startDateTimeCaptor.capture(),
                endDateTimeCaptor.capture()
        );

        assertTrue(mandorIdsCaptor.getValue().contains(10L));
        assertEquals(date.atStartOfDay(), startDateTimeCaptor.getValue());
        assertEquals(date.atTime(LocalTime.MAX), endDateTimeCaptor.getValue());
    }

    @Test
    void getApprovedPengirimanForAdmin_shouldPassNullFiltersWhenEmpty() {
        when(userClient.findById(99L)).thenReturn(Optional.of(user(99L, "admin", "ADMIN")));
        when(pengirimanRepository.findForAdminApproval(eq(StatusPengiriman.APPROVED_MANDOR), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        List<Pengiriman> result = pengirimanService.getApprovedPengirimanForAdmin(99L, "   ", null);

        assertTrue(result.isEmpty());
        verify(pengirimanRepository).findForAdminApproval(eq(StatusPengiriman.APPROVED_MANDOR), isNull(), isNull(), isNull());
    }

    @Test
    void getAvailableDriversForMandor_shouldUseFindAllWhenSearchNameBlank() {
        UserSummary mandor = user(1L, "mandor", "MANDOR");
        UserSummary driver = user(2L, "supir", "SUPIR");
        UserSummary otherKebunDriver = user(3L, "supir.lain", "SUPIR");

        when(userClient.findById(1L)).thenReturn(Optional.of(mandor));
        when(userClient.findByRole("SUPIR")).thenReturn(List.of(mandor, driver, otherKebunDriver));

        List<UserSummary> result = pengirimanService.getAvailableDriversForMandor(1L, "   ");

        assertEquals(1, result.size());
        assertEquals(2L, result.getFirst().getId());
    }

    @Test
    void approveByAdmin_shouldSetApprovedStatusAndAcknowledgedWeight() {
        UserSummary admin = user(99L, "admin", "ADMIN");
        Pengiriman pengiriman = Pengiriman.builder()
                .id(400L)
                                .mandorId(1L)
                .status(StatusPengiriman.APPROVED_MANDOR)
                .totalWeightKg(350.0)
                .rejectionReason("old")
                .build();

        when(userClient.findById(99L)).thenReturn(Optional.of(admin));
        when(pengirimanRepository.findById(400L)).thenReturn(Optional.of(pengiriman));
        when(pengirimanRepository.save(any(Pengiriman.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pengiriman result = pengirimanService.approveByAdmin(400L, 99L);

        assertEquals(StatusPengiriman.APPROVED_ADMIN, result.getStatus());
        assertEquals(350.0, result.getAcknowledgedWeightKg());
        assertNull(result.getRejectionReason());

                ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
                verify(eventPublisher, times(2)).publish(any(), eventCaptor.capture());
                assertTrue(eventCaptor.getAllValues().stream().anyMatch(ev -> ev instanceof PayrollEvent));
                verify(paymentClient).requestPayroll(99L, 1L, "MANDOR", 350.0);
    }

    @Test
    void rejectByAdmin_shouldSetRejectedStatusAndTrimReason() {
        UserSummary admin = user(99L, "admin", "ADMIN");
        Pengiriman pengiriman = Pengiriman.builder()
                .id(401L)
                .status(StatusPengiriman.APPROVED_MANDOR)
                .totalWeightKg(350.0)
                .build();

        when(userClient.findById(99L)).thenReturn(Optional.of(admin));
        when(pengirimanRepository.findById(401L)).thenReturn(Optional.of(pengiriman));
        when(pengirimanRepository.save(any(Pengiriman.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pengiriman result = pengirimanService.rejectByAdmin(401L, 99L, "  Muatan tidak sesuai  ");

        assertEquals(StatusPengiriman.REJECTED_ADMIN, result.getStatus());
        assertEquals("Muatan tidak sesuai", result.getRejectionReason());
        assertNull(result.getAcknowledgedWeightKg());
    }

    @Test
    void partialRejectByAdmin_shouldSetPartialRejectedStatusWhenValid() {
        UserSummary admin = user(99L, "admin", "ADMIN");
        Pengiriman pengiriman = Pengiriman.builder()
                .id(402L)
                                .mandorId(5L)
                .status(StatusPengiriman.APPROVED_MANDOR)
                .totalWeightKg(300.0)
                .build();

        when(userClient.findById(99L)).thenReturn(Optional.of(admin));
        when(pengirimanRepository.findById(402L)).thenReturn(Optional.of(pengiriman));
        when(pengirimanRepository.save(any(Pengiriman.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pengiriman result = pengirimanService.partialRejectByAdmin(402L, 99L, 250.0, "  Sebagian rusak ");

        assertEquals(StatusPengiriman.PARTIALLY_REJECTED_ADMIN, result.getStatus());
        assertEquals(250.0, result.getAcknowledgedWeightKg());
        assertEquals("Sebagian rusak", result.getRejectionReason());

                ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
                verify(eventPublisher, times(2)).publish(any(), eventCaptor.capture());
                assertTrue(eventCaptor.getAllValues().stream().anyMatch(ev -> ev instanceof PayrollEvent));
                verify(paymentClient).requestPayroll(99L, 5L, "MANDOR", 250.0);
    }

    @Test
    void getPengirimanById_shouldReturnDataWhenExists() {
        Pengiriman pengiriman = Pengiriman.builder().id(500L).build();
        when(pengirimanRepository.findById(500L)).thenReturn(Optional.of(pengiriman));

        Pengiriman result = pengirimanService.getPengirimanById(500L);

        assertEquals(pengiriman, result);
    }

    @Test
    void getPengirimanById_shouldThrowWhenNotFound() {
        when(pengirimanRepository.findById(501L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pengirimanService.getPengirimanById(501L)
        );

        assertEquals("Pengiriman not found", ex.getMessage());
    }

    @Test
    void getPengirimanByIdForUser_shouldAllowAssignedMandor() {
        Pengiriman pengiriman = Pengiriman.builder()
                .id(600L)
                .mandorId(1L)
                .driverId(2L)
                .build();

        when(pengirimanRepository.findById(600L)).thenReturn(Optional.of(pengiriman));
        when(userClient.findById(1L)).thenReturn(Optional.of(user(1L, "mandor", "MANDOR")));

        Pengiriman result = pengirimanService.getPengirimanByIdForUser(600L, 1L, "MANDOR");

        assertEquals(pengiriman, result);
    }

    @Test
    void getPengirimanByIdForUser_shouldRejectUnassignedSupir() {
        Pengiriman pengiriman = Pengiriman.builder()
                .id(601L)
                .mandorId(1L)
                .driverId(2L)
                .build();

        when(pengirimanRepository.findById(601L)).thenReturn(Optional.of(pengiriman));
        when(userClient.findById(3L)).thenReturn(Optional.of(user(3L, "supir.lain", "SUPIR")));

        SecurityException ex = assertThrows(
                SecurityException.class,
                () -> pengirimanService.getPengirimanByIdForUser(601L, 3L, "SUPIR")
        );

        assertEquals("Supir is not assigned to this pengiriman", ex.getMessage());
    }
}
