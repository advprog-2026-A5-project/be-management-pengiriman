package id.ac.ui.cs.advprog.bemanagementpengiriman.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HarvestClientHttp implements HarvestClient {

    private final RestClient restClient;

    public HarvestClientHttp(
            RestClient.Builder builder,
            @Value("${harvest.service.base-url:http://localhost:8082}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public boolean isApprovedHarvest(Long harvestId) {
        if (harvestId == null) {
            return false;
        }
        try {
            HarvestStatusResponse response = restClient.get()
                    .uri("/harvests/{id}", harvestId)
                    .retrieve()
                    .body(HarvestStatusResponse.class);
            return response != null && Boolean.TRUE.equals(response.getApproved());
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw ex;
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class HarvestStatusResponse {
        private Boolean approved;
    }
}
