package id.ac.ui.cs.advprog.bemanagementpengiriman.repository;

import id.ac.ui.cs.advprog.bemanagementpengiriman.enums.StatusPengiriman;
import id.ac.ui.cs.advprog.bemanagementpengiriman.model.Pengiriman;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface PengirimanRepository extends JpaRepository<Pengiriman, Long> {
    List<Pengiriman> findByStatus(StatusPengiriman status);
    List<Pengiriman> findByDriverIdAndStatusIn(Long driverId, Collection<StatusPengiriman> statuses);
    List<Pengiriman> findByMandorIdAndStatusIn(Long mandorId, Collection<StatusPengiriman> statuses);
    List<Pengiriman> findByMandorIdAndDriverIdAndStatusIn(Long mandorId, Long driverId, Collection<StatusPengiriman> statuses);

    @Query("""
            SELECT p
            FROM Pengiriman p
            WHERE p.driverId = :driverId
              AND p.status IN :statuses
              AND (:startDate IS NULL OR p.updatedAt >= :startDate)
              AND (:endDate IS NULL OR p.updatedAt <= :endDate)
            ORDER BY p.updatedAt DESC
            """)
    List<Pengiriman> findDriverHistory(
            @Param("driverId") Long driverId,
            @Param("statuses") Collection<StatusPengiriman> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
            SELECT p
            FROM Pengiriman p
            WHERE p.status = :status
              AND (:mandorIds IS NULL OR p.mandorId IN :mandorIds)
              AND (:startDate IS NULL OR p.updatedAt >= :startDate)
              AND (:endDate IS NULL OR p.updatedAt <= :endDate)
            ORDER BY p.updatedAt DESC
            """)
    List<Pengiriman> findForAdminApproval(
            @Param("status") StatusPengiriman status,
            @Param("mandorIds") Collection<Long> mandorIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
                    SELECT COUNT(p)
                    FROM Pengiriman p
                    JOIN p.items i
                    WHERE i.harvestId = :harvestId
                    AND p.status IN :statuses """)
    long countActiveShipmentByHarvestId(
            @Param("harvestId") Long harvestId,
            @Param("statuses") Collection<StatusPengiriman> statuses
    );
}