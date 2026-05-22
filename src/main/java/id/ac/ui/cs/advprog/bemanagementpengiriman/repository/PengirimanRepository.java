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
import java.util.UUID;

@Repository
public interface PengirimanRepository extends JpaRepository<Pengiriman, Long> {
    List<Pengiriman> findByStatus(StatusPengiriman status);
    List<Pengiriman> findByDriverIdAndStatusIn(Long driverId, Collection<StatusPengiriman> statuses);
    List<Pengiriman> findByDriverIdAndStatusInOrderByUpdatedAtDesc(Long driverId, Collection<StatusPengiriman> statuses);
    List<Pengiriman> findByDriverIdAndStatusInAndUpdatedAtGreaterThanEqualOrderByUpdatedAtDesc(
            Long driverId,
            Collection<StatusPengiriman> statuses,
            LocalDateTime startDate
    );
    List<Pengiriman> findByDriverIdAndStatusInAndUpdatedAtLessThanEqualOrderByUpdatedAtDesc(
            Long driverId,
            Collection<StatusPengiriman> statuses,
            LocalDateTime endDate
    );
    List<Pengiriman> findByDriverIdAndStatusInAndUpdatedAtBetweenOrderByUpdatedAtDesc(
            Long driverId,
            Collection<StatusPengiriman> statuses,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
    List<Pengiriman> findByMandorIdAndStatusIn(Long mandorId, Collection<StatusPengiriman> statuses);
    List<Pengiriman> findByMandorIdAndDriverIdAndStatusIn(Long mandorId, Long driverId, Collection<StatusPengiriman> statuses);
    List<Pengiriman> findByStatusOrderByUpdatedAtDesc(StatusPengiriman status);
    List<Pengiriman> findByStatusAndMandorIdInOrderByUpdatedAtDesc(
            StatusPengiriman status,
            Collection<Long> mandorIds
    );
    List<Pengiriman> findByStatusAndUpdatedAtBetweenOrderByUpdatedAtDesc(
            StatusPengiriman status,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
    List<Pengiriman> findByStatusAndMandorIdInAndUpdatedAtBetweenOrderByUpdatedAtDesc(
            StatusPengiriman status,
            Collection<Long> mandorIds,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("""
                    SELECT COUNT(p)
                    FROM Pengiriman p
                    JOIN p.items i
                    WHERE i.harvestId = :harvestId
                    AND p.status IN :statuses""")
    long countActiveShipmentByHarvestId(
            @Param("harvestId") UUID harvestId,
            @Param("statuses") Collection<StatusPengiriman> statuses
    );
}
