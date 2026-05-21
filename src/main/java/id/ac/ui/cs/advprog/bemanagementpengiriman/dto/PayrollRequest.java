package id.ac.ui.cs.advprog.bemanagementpengiriman.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PayrollRequest {
    private Long userId;
    private String role;
    private Double kilogram;
}
