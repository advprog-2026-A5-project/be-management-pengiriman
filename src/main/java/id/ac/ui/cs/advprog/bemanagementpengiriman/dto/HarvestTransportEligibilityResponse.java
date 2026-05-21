package id.ac.ui.cs.advprog.bemanagementpengiriman.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record HarvestTransportEligibilityResponse(
        UUID harvestId,
        boolean eligible,
        String status,
        BigDecimal kilogram
) {
}
