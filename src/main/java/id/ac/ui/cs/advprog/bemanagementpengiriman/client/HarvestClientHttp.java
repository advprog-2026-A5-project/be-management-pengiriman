package id.ac.ui.cs.advprog.bemanagementpengiriman.client;

import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.HarvestTransportEligibilityResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;
import java.util.UUID;

@Component
public class HarvestClientHttp implements HarvestClient {

    private final RestClient restClient;

    public HarvestClientHttp(
            RestClient.Builder builder,
            @Value("${mysawit.services.hasil-panen.base-url:http://localhost:8082}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Optional<HarvestTransportEligibilityResponse> getTransportEligibility(UUID harvestId) {
        if (harvestId == null) {
            return Optional.empty();
        }
        try {
            HarvestTransportEligibilityResponse response = restClient.get()
                    .uri("/internal/harvests/{id}/transport-eligibility", harvestId)
                    .retrieve()
                    .body(HarvestTransportEligibilityResponse.class);
            return Optional.ofNullable(response);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw ex;
        }
    }
}
