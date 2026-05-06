package id.ac.ui.cs.advprog.bemanagementpengiriman.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "pengiriman_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PengirimanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private UUID harvestId;

    @Column(nullable = false)
    private double weightKg;

    @ManyToOne
    @JoinColumn(name = "shipment_id")
    private Pengiriman shipment;
}
