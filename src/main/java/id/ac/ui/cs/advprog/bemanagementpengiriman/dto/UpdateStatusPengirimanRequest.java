package id.ac.ui.cs.advprog.bemanagementpengiriman.dto;

import id.ac.ui.cs.advprog.bemanagementpengiriman.enums.StatusPengiriman;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusPengirimanRequest {
    private StatusPengiriman newStatus;
}