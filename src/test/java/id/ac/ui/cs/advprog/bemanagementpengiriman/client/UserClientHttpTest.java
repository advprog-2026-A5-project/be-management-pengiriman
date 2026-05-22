package id.ac.ui.cs.advprog.bemanagementpengiriman.client;

import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.UserSummary;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UserClientHttpTest {

    @Test
    void findById_shouldReturnEmptyWhenIdIsNull() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserClientHttp client = new UserClientHttp(builder, "http://user.test", "token");

        assertFalse(client.findById(null).isPresent());
        server.verify();
    }

    @Test
    void findById_shouldReturnUserAndSendInternalToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserClientHttp client = new UserClientHttp(builder, "http://user.test", "  token  ");

        server.expect(once(), requestTo("http://user.test/internal/users/5/identity"))
                .andExpect(header("X-Internal-Service-Token", "token"))
                .andRespond(withSuccess("""
                        {
                          "id": 5,
                          "username": "supir",
                          "email": "supir@example.com",
                          "nama": "Supir",
                          "role": "SUPIR"
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<UserSummary> result = client.findById(5L);

        assertTrue(result.isPresent());
        assertEquals(5L, result.get().getId());
        assertEquals("SUPIR", result.get().getRole());
        server.verify();
    }

    @Test
    void findById_shouldReturnEmptyForNotFoundAndRethrowOtherErrors() {
        RestClient.Builder notFoundBuilder = RestClient.builder();
        MockRestServiceServer notFoundServer = MockRestServiceServer.bindTo(notFoundBuilder).build();
        UserClientHttp notFoundClient = new UserClientHttp(notFoundBuilder, "http://user.test", "");

        notFoundServer.expect(once(), requestTo("http://user.test/internal/users/5/identity"))
                .andRespond(withResourceNotFound());
        assertFalse(notFoundClient.findById(5L).isPresent());
        notFoundServer.verify();

        RestClient.Builder errorBuilder = RestClient.builder();
        MockRestServiceServer errorServer = MockRestServiceServer.bindTo(errorBuilder).build();
        UserClientHttp errorClient = new UserClientHttp(errorBuilder, "http://user.test", "");

        errorServer.expect(once(), requestTo("http://user.test/internal/users/6/identity"))
                .andRespond(withServerError());
        assertThrows(RestClientResponseException.class, () -> errorClient.findById(6L));
        errorServer.verify();
    }

    @Test
    void findByNameAndRole_shouldReturnEmptyForBlankRole() {
        UserClientHttp client = new UserClientHttp(RestClient.builder(), "http://user.test", "");

        assertTrue(client.findByNameAndRole("su", null).isEmpty());
        assertTrue(client.findByNameAndRole("su", " ").isEmpty());
    }

    @Test
    void findByNameAndRole_shouldQueryUsers() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserClientHttp client = new UserClientHttp(builder, "http://user.test", "token");

        server.expect(once(), requestTo("http://user.test/internal/users?nama=su&role=SUPIR"))
                .andExpect(queryParam("nama", "su"))
                .andExpect(queryParam("role", "SUPIR"))
                .andExpect(header("X-Internal-Service-Token", "token"))
                .andRespond(withSuccess("""
                        [
                          {"id": 2, "username": "supir", "role": "SUPIR"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<UserSummary> result = client.findByNameAndRole("su", "SUPIR");

        assertEquals(1, result.size());
        assertEquals(2L, result.getFirst().getId());
        server.verify();
    }

    @Test
    void findByNameAndRole_shouldReturnEmptyWhenResponseBodyIsEmpty() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserClientHttp client = new UserClientHttp(builder, "http://user.test", "");

        server.expect(once(), requestTo("http://user.test/internal/users?nama=ghost&role=MANDOR"))
                .andExpect(queryParam("nama", "ghost"))
                .andExpect(queryParam("role", "MANDOR"))
                .andRespond(withSuccess());

        List<UserSummary> result = client.findByNameAndRole("ghost", "MANDOR");

        assertTrue(result.isEmpty());
        server.verify();
    }

    @Test
    void findByRole_shouldReturnEmptyForBlankRoleAndQueryUsersForValidRole() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserClientHttp client = new UserClientHttp(builder, "http://user.test", null);

        assertTrue(client.findByRole("").isEmpty());

        server.expect(once(), requestTo("http://user.test/internal/users?role=MANDOR"))
                .andExpect(queryParam("role", "MANDOR"))
                .andRespond(withSuccess("""
                        [
                          {"id": 1, "username": "mandor", "role": "MANDOR"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<UserSummary> result = client.findByRole("MANDOR");

        assertEquals(1, result.size());
        assertEquals("MANDOR", result.getFirst().getRole());
        server.verify();
    }

    @Test
    void findByRole_shouldReturnEmptyWhenResponseBodyIsEmpty() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserClientHttp client = new UserClientHttp(builder, "http://user.test", "");

        server.expect(once(), requestTo("http://user.test/internal/users?role=SUPIR"))
                .andExpect(queryParam("role", "SUPIR"))
                .andRespond(withSuccess());

        List<UserSummary> result = client.findByRole("SUPIR");

        assertTrue(result.isEmpty());
        server.verify();
    }
}
