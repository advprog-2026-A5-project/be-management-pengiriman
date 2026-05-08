package id.ac.ui.cs.advprog.bemanagementpengiriman.client;

import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.UserSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Optional;

@Component
public class UserClientHttp implements UserClient {

    private final RestClient restClient;

    public UserClientHttp(
            RestClient.Builder builder,
            @Value("${user.service.base-url:http://localhost:8081}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Optional<UserSummary> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try {
            UserSummary user = restClient.get()
                    .uri("/users/{id}", id)
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
    public List<UserSummary> findByUsernameContainingIgnoreCase(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }
        List<UserSummary> users = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users")
                        .queryParam("username", username)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<UserSummary>>() {
                });
        return users == null ? List.of() : users;
    }

    @Override
    public List<UserSummary> findAll() {
        List<UserSummary> users = restClient.get()
                .uri("/users")
                .retrieve()
                .body(new ParameterizedTypeReference<List<UserSummary>>() {
                });
        return users == null ? List.of() : users;
    }
}
