package id.ac.ui.cs.advprog.bemanagementpengiriman.client;

import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.UserSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Optional;

@Component
public class UserClientHttp implements UserClient {

    private final RestClient restClient;
    private final String internalServiceToken;

    public UserClientHttp(
            RestClient.Builder builder,
            @Value("${mysawit.services.user.base-url:http://localhost:8080}") String baseUrl,
            @Value("${mysawit.services.user.auth-token:}") String serviceToken
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalServiceToken = serviceToken == null ? "" : serviceToken.trim();
    }

    @Override
    @Cacheable(
            cacheNames = "userById",
            key = "#id",
            condition = "#id != null",
            unless = "#result == null || #result.isEmpty()"
    )
    public Optional<UserSummary> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try {
            UserSummary user = restClient.get()
                    .uri("/internal/users/{id}/identity", id)
                    .headers(this::addInternalTokenIfConfigured)
                    .retrieve()
                    .body(UserSummary.class);
            return Optional.ofNullable(user);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    @Override
    @Cacheable(
            cacheNames = "usersByNameAndRole",
            key = "(#name == null ? '' : #name.trim().toLowerCase()) + ':' + #role.trim().toUpperCase()",
            condition = "#role != null && !#role.isBlank()",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<UserSummary> findByNameAndRole(String name, String role) {
        if (role == null || role.isBlank()) {
            return List.of();
        }
        List<UserSummary> users = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/users")
                        .queryParam("nama", name)
                        .queryParam("role", role)
                        .build())
                .headers(this::addInternalTokenIfConfigured)
                .retrieve()
                .body(new ParameterizedTypeReference<List<UserSummary>>() {
                });
        return users == null ? List.of() : users;
    }

    @Override
    @Cacheable(
            cacheNames = "usersByRole",
            key = "#role.trim().toUpperCase()",
            condition = "#role != null && !#role.isBlank()",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<UserSummary> findByRole(String role) {
        if (role == null || role.isBlank()) {
            return List.of();
        }
        List<UserSummary> users = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/users")
                        .queryParam("role", role)
                        .build())
                .headers(this::addInternalTokenIfConfigured)
                .retrieve()
                .body(new ParameterizedTypeReference<List<UserSummary>>() {
                });
        return users == null ? List.of() : users;
    }

    private void addInternalTokenIfConfigured(HttpHeaders headers) {
        if (!internalServiceToken.isBlank()) {
            headers.set("X-Internal-Service-Token", internalServiceToken);
        }
    }
}
