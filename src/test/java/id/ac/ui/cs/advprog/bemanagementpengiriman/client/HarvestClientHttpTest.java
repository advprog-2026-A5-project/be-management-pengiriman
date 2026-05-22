package id.ac.ui.cs.advprog.bemanagementpengiriman.client;

import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.HarvestTransportEligibilityResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HarvestClientHttpTest {

    @Test
    void getTransportEligibility_shouldReturnEmptyWhenHarvestIdIsNull() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HarvestClientHttp client = new HarvestClientHttp(builder, "http://harvest.test");

        assertFalse(client.getTransportEligibility(null).isPresent());
        server.verify();
    }

    @Test
    void getTransportEligibility_shouldReturnResponse() {
        UUID harvestId = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HarvestClientHttp client = new HarvestClientHttp(builder, "http://harvest.test");

        server.expect(once(), requestTo(
                        "http://harvest.test/internal/harvests/" + harvestId + "/transport-eligibility"))
                .andRespond(withSuccess("""
                        {
                          "harvestId": "%s",
                          "eligible": true,
                          "status": "APPROVED",
                          "kilogram": 125.5
                        }
                        """.formatted(harvestId), MediaType.APPLICATION_JSON));

        Optional<HarvestTransportEligibilityResponse> result = client.getTransportEligibility(harvestId);

        assertTrue(result.isPresent());
        assertEquals(harvestId, result.get().harvestId());
        assertTrue(result.get().eligible());
        server.verify();
    }

    @Test
    void getTransportEligibility_shouldReturnEmptyForNotFoundAndRethrowOtherErrors() {
        UUID missingId = UUID.randomUUID();
        RestClient.Builder missingBuilder = RestClient.builder();
        MockRestServiceServer missingServer = MockRestServiceServer.bindTo(missingBuilder).build();
        HarvestClientHttp missingClient = new HarvestClientHttp(missingBuilder, "http://harvest.test");

        missingServer.expect(once(), requestTo(
                        "http://harvest.test/internal/harvests/" + missingId + "/transport-eligibility"))
                .andRespond(withResourceNotFound());
        assertFalse(missingClient.getTransportEligibility(missingId).isPresent());
        missingServer.verify();

        UUID errorId = UUID.randomUUID();
        RestClient.Builder errorBuilder = RestClient.builder();
        MockRestServiceServer errorServer = MockRestServiceServer.bindTo(errorBuilder).build();
        HarvestClientHttp errorClient = new HarvestClientHttp(errorBuilder, "http://harvest.test");

        errorServer.expect(once(), requestTo(
                        "http://harvest.test/internal/harvests/" + errorId + "/transport-eligibility"))
                .andRespond(withServerError());
        assertThrows(RestClientResponseException.class, () -> errorClient.getTransportEligibility(errorId));
        errorServer.verify();
    }
}
