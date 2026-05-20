package id.ac.ui.cs.advprog.bemanagementpengiriman.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminPartialRejectionRequest {
    @NotNull
    @Positive
    private Double acknowledgedWeightKg;

    @NotBlank
    private String rejectionReason;
}
