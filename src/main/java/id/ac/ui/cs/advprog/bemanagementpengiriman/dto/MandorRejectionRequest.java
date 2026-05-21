package id.ac.ui.cs.advprog.bemanagementpengiriman.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MandorRejectionRequest {
    @NotBlank
    private String rejectionReason;
}
