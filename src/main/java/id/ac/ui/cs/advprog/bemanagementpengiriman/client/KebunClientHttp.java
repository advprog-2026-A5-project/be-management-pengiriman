package id.ac.ui.cs.advprog.bemanagementpengiriman.client;

import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.KebunDetailResponse;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.MandorKebunAssignmentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
public class KebunClientHttp implements KebunClient {

    private final RestClient restClient;
    private final String serviceToken;

    public KebunClientHttp(RestClient.Builder builder,
            @Value("${mysawit.services.kebun.base-url:http://localhost:8081}") String baseUrl,
            @Value("${mysawit.services.kebun.auth-token:}") String serviceToken) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.serviceToken = serviceToken;
    }

    @Override
    @Cacheable(
            cacheNames = "mandorKebunAssignment",
            key = "#mandorId",
            condition = "#mandorId != null",
            unless = "#result == null || #result.isEmpty()"
    )
    public Optional<MandorKebunAssignmentResponse> getMandorKebunAssignment(Long mandorId) {
        try {
            MandorKebunAssignmentResponse response = restClient.get()
                    .uri("/internal/mandors/{mandorId}/kebun", mandorId)
                    .headers(this::addAuthorizationIfConfigured)
                    .retrieve()
                    .body(MandorKebunAssignmentResponse.class);
            return Optional.ofNullable(response);
        } catch (RestClientException ex) {
            return Optional.empty();
        }
    }

    @Override
    @Cacheable(
            cacheNames = "kebunDetail",
            key = "#kebunCode.trim().toUpperCase()",
            condition = "#kebunCode != null && !#kebunCode.isBlank()",
            unless = "#result == null || #result.isEmpty()"
    )
    public Optional<KebunDetailResponse> getKebunDetail(String kebunCode) {
        try {
            KebunDetailResponse response = restClient.get()
                    .uri("/kebun/{code}/detail", kebunCode)
                    .headers(this::addAuthorizationIfConfigured)
                    .retrieve()
                    .body(KebunDetailResponse.class);
            return Optional.ofNullable(response);
        } catch (RestClientException ex) {
            return Optional.empty();
        }
    }

    private void addAuthorizationIfConfigured(HttpHeaders headers) {
        if (serviceToken != null && !serviceToken.isBlank()) {
            headers.setBearerAuth(serviceToken);
        }
    }
}
