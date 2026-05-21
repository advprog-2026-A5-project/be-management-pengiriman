package id.ac.ui.cs.advprog.bemanagementpengiriman.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PaymentClientHttpTest {

    @Test
    void requestPayroll_shouldPostPayrollRequestWithAdminServiceRole() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PaymentClientHttp client = new PaymentClientHttp(builder, "http://payment.test");

        server.expect(once(), requestTo("http://payment.test/api/payroll/create"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-Role", "ADMIN"))
                .andExpect(header("X-User-Id", "99"))
                .andExpect(content().json("""
                        {
                          "userId": 5,
                          "role": "MANDOR",
                          "kilogram": 250.5
                        }
                        """))
                .andRespond(withSuccess());

        client.requestPayroll(99L, 5L, "MANDOR", 250.5);

        server.verify();
    }

    @Test
    void requestPayroll_shouldUseZeroActorIdWhenActorIsNull() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PaymentClientHttp client = new PaymentClientHttp(builder, "http://payment.test");

        server.expect(once(), requestTo("http://payment.test/api/payroll/create"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-Role", "ADMIN"))
                .andExpect(header("X-User-Id", "0"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "userId": 2,
                          "role": "SUPIR",
                          "kilogram": 180.0
                        }
                        """))
                .andRespond(withSuccess());

        client.requestPayroll(null, 2L, "SUPIR", 180.0);

        server.verify();
    }
}
