package id.ac.ui.cs.advprog.bemanagementpengiriman.client;

import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.KebunDetailResponse;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.MandorKebunAssignmentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KebunClientHttpTest {

    @Test
    void getMandorKebunAssignment_shouldReturnResponseAndSendBearerToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KebunClientHttp client = new KebunClientHttp(builder, "http://kebun.test", "service-token");

        server.expect(once(), requestTo("http://kebun.test/internal/mandors/1/kebun"))
                .andExpect(header("Authorization", "Bearer service-token"))
                .andRespond(withSuccess("""
                        {
                          "mandorId": 1,
                          "kebunId": null,
                          "kebunCode": "KB001",
                          "kebunName": "Kebun A",
                          "active": true
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<MandorKebunAssignmentResponse> result = client.getMandorKebunAssignment(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().mandorId());
        assertEquals("KB001", result.get().kebunCode());
        assertTrue(result.get().active());
        server.verify();
    }

    @Test
    void getMandorKebunAssignment_shouldReturnEmptyWhenKebunServiceFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KebunClientHttp client = new KebunClientHttp(builder, "http://kebun.test", "");

        server.expect(once(), requestTo("http://kebun.test/internal/mandors/1/kebun"))
                .andRespond(withServerError());

        Optional<MandorKebunAssignmentResponse> result = client.getMandorKebunAssignment(1L);

        assertFalse(result.isPresent());
        server.verify();
    }

    @Test
    void getKebunDetail_shouldReturnResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KebunClientHttp client = new KebunClientHttp(builder, "http://kebun.test", "");

        server.expect(once(), requestTo("http://kebun.test/kebun/KB001/detail"))
                .andRespond(withSuccess("""
                        {
                          "code": "KB001",
                          "name": "Kebun A",
                          "luas": 12.5,
                          "coordinates": [],
                          "mandorId": "1",
                          "supirIds": ["2", "3"]
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<KebunDetailResponse> result = client.getKebunDetail("KB001");

        assertTrue(result.isPresent());
        assertEquals("KB001", result.get().code());
        assertEquals("Kebun A", result.get().name());
        assertEquals(12.5, result.get().luas());
        assertEquals("1", result.get().mandorId());
        assertEquals(2, result.get().supirIds().size());
        server.verify();
    }

    @Test
    void getKebunDetail_shouldReturnEmptyWhenKebunServiceFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KebunClientHttp client = new KebunClientHttp(builder, "http://kebun.test", "");

        server.expect(once(), requestTo("http://kebun.test/kebun/KB001/detail"))
                .andRespond(withServerError());

        Optional<KebunDetailResponse> result = client.getKebunDetail("KB001");

        assertFalse(result.isPresent());
        server.verify();
    }
}
