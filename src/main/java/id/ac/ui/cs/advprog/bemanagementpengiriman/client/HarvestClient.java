package id.ac.ui.cs.advprog.bemanagementpengiriman.client;

import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.HarvestTransportEligibilityResponse;

import java.util.Optional;
import java.util.UUID;

public interface HarvestClient {
    Optional<HarvestTransportEligibilityResponse> getTransportEligibility(UUID harvestId);
}
