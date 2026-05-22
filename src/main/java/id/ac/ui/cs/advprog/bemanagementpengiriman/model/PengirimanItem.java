package id.ac.ui.cs.advprog.bemanagementpengiriman.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "pengiriman_items",
        indexes = {
                @Index(name = "idx_pengiriman_items_harvest_id", columnList = "harvest_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PengirimanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne
    @JoinColumn(name = "pengiriman_id", nullable = false)
    private Pengiriman shipment;

    
    @Column(name = "harvest_id", nullable = false)
    private UUID harvestId;

    @Column(nullable = false)
    private double weightKg;
}
