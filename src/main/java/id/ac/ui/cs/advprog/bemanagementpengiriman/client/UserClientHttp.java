package id.ac.ui.cs.advprog.bemanagementpengiriman.client;

import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.UserSummary;
import org.springframework.beans.factory.annotation.Value;
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
    private final String serviceToken;

    public UserClientHttp(
            RestClient.Builder builder,
            @Value("${user.service.base-url:http://localhost:8080}") String baseUrl,
            @Value("${user.service.auth-token:}") String serviceToken
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.serviceToken = serviceToken == null ? "" : serviceToken.trim();
    }

    @Override
    public Optional<UserSummary> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try {
            UserSummary user = restClient.get()
                    .uri("/internal/users/{id}/identity", id)
                    .headers(this::addAuthorizationIfConfigured)
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
    public List<UserSummary> findByNameAndRole(String name, String role) {
        if (role == null || role.isBlank()) {
            return List.of();
        }
        List<UserSummary> users = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/users")
                        .queryParam("nama", name)
                        .queryParam("role", role)
                        .build())
                .headers(this::addAuthorizationIfConfigured)
                .retrieve()
                .body(new ParameterizedTypeReference<List<UserSummary>>() {
                });
        return users == null ? List.of() : users;
    }

    @Override
    public List<UserSummary> findByRole(String role) {
        if (role == null || role.isBlank()) {
            return List.of();
        }
        List<UserSummary> users = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/users")
                        .queryParam("role", role)
                        .build())
                .headers(this::addAuthorizationIfConfigured)
                .retrieve()
                .body(new ParameterizedTypeReference<List<UserSummary>>() {
                });
        return users == null ? List.of() : users;
    }

    private void addAuthorizationIfConfigured(HttpHeaders headers) {
        if (!serviceToken.isBlank()) {
            headers.setBearerAuth(serviceToken);
        }
    }
}
