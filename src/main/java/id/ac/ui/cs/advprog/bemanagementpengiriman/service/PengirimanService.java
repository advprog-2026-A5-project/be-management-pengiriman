package id.ac.ui.cs.advprog.bemanagementpengiriman.service;

import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.AssignDriverRequest;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.UserSummary;
import id.ac.ui.cs.advprog.bemanagementpengiriman.model.Pengiriman;
import id.ac.ui.cs.advprog.bemanagementpengiriman.enums.StatusPengiriman;

import java.time.LocalDate;
import java.util.List;

public interface PengirimanService {
    Pengiriman assignDriver(Long mandorId, AssignDriverRequest request);
    Pengiriman updateStatusPengiriman(Long pengirimanId, Long driverId, StatusPengiriman newStatus);
    List<Pengiriman> getPengirimanByDriver(Long driverId);
    List<Pengiriman> getPengirimanByDriverForMandor(Long mandorId, Long driverId);
    List<Pengiriman> getPengirimanHistoryByDriver(Long driverId, LocalDate startDate, LocalDate endDate);
    List<Pengiriman> getOngoingPengiriman(Long mandorId);
    List<Pengiriman> getPengirimanByStatus(StatusPengiriman status);
    List<Pengiriman> getApprovedPengirimanForAdmin(String mandorName, LocalDate date);
    List<UserSummary> getAvailableDriversForMandor(Long mandorId, String searchName);
    Pengiriman approveByMandor(Long pengirimanId, Long mandorId);
    Pengiriman rejectByMandor(Long pengirimanId, Long mandorId, String rejectionReason);
    Pengiriman approveByAdmin(Long pengirimanId, Long adminId);
    Pengiriman rejectByAdmin(Long pengirimanId, Long adminId, String rejectionReason);
    Pengiriman partialRejectByAdmin(Long pengirimanId, Long adminId, Double acknowledgedWeightKg, String rejectionReason);
    Pengiriman getPengirimanById(Long pengirimanId);
}