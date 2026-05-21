package id.ac.ui.cs.advprog.bemanagementpengiriman.client;

import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.PayrollRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PaymentClientHttp implements PaymentClient {

    private static final String SERVICE_ROLE = "ADMIN";

    private final RestClient restClient;

    public PaymentClientHttp(
            RestClient.Builder builder,
            @Value("${payment.service.base-url:http://localhost:8084}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public void requestPayroll(Long actorId, Long userId, String role, Double kilogram) {
        restClient.post()
                .uri("/api/payroll/create")
                .header("X-User-Role", SERVICE_ROLE)
                .header("X-User-Id", String.valueOf(actorId == null ? 0L : actorId))
                .body(new PayrollRequest(userId, role, kilogram))
                .retrieve()
                .toBodilessEntity();
    }
}
