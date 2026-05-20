package id.ac.ui.cs.advprog.bemanagementpengiriman.dto;

import id.ac.ui.cs.advprog.bemanagementpengiriman.enums.StatusPengiriman;
import lombok.*;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusPengirimanRequest {
    @NotNull
    private StatusPengiriman newStatus;
}