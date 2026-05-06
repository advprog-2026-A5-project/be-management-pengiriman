package id.ac.ui.cs.advprog.bemanagementpengiriman.model;

// import id.ac.ui.cs.advprog.bemanagementpengiriman.enums.StatusPengiriman;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pengiriman")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pengiriman {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "mandor_id", nullable = false)
    private Long mandorId;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PengirimanItem> items;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPengiriman status;

    @Column(nullable = false)
    private double totalWeightKg;

    private String rejectionReason;

    private Double acknowledgedWeightKg;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = StatusPengiriman.MEMUAT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

enum StatusPengiriman {
    MEMUAT,
    MENGIRIM,
    SELESAI,
    DITOLAK
}
