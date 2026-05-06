package id.ac.ui.cs.advprog.bemanagementpengiriman.model;

import id.ac.ui.cs.advprog.bemanagementpengiriman.model.Pengiriman;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shipment_items")
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
    @JoinColumn(name = "shipment_id", nullable = false)
    private Pengiriman shipment;

    // Reference to approved harvest ID from another module
    @Column(nullable = false)
    private long harvestId;

    @Column(nullable = false)
    private double weightKg;
}