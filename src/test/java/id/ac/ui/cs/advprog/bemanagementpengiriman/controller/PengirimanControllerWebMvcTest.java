package id.ac.ui.cs.advprog.bemanagementpengiriman.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.AssignDriverRequest;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.MandorRejectionRequest;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.UpdateStatusPengirimanRequest;
import id.ac.ui.cs.advprog.bemanagementpengiriman.enums.StatusPengiriman;
import id.ac.ui.cs.advprog.bemanagementpengiriman.model.Pengiriman;
import id.ac.ui.cs.advprog.bemanagementpengiriman.security.UserPrincipal;
import id.ac.ui.cs.advprog.bemanagementpengiriman.service.PengirimanService;
import id.ac.ui.cs.advprog.bemanagementpengiriman.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PengirimanController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class PengirimanControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PengirimanService pengirimanService;

    @Test
    void assignDriver_returnsCreatedForMandor() throws Exception {
        UserPrincipal principal = new UserPrincipal(1L, "MANDOR");
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MANDOR"))
        );

        AssignDriverRequest request = AssignDriverRequest.builder()
                .driverId(2L)
                .harvestItems(List.of(new AssignDriverRequest.HarvestItemDto(10L, 100.0)))
                .build();

        Pengiriman response = Pengiriman.builder().id(99L).build();
        when(pengirimanService.assignDriver(eq(1L), any(AssignDriverRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/pengiriman/assign")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void updateStatus_returnsOkForDriver() throws Exception {
        UserPrincipal principal = new UserPrincipal(2L, "DRIVER");
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DRIVER"))
        );

        UpdateStatusPengirimanRequest request = new UpdateStatusPengirimanRequest(StatusPengiriman.MENGIRIM);
        Pengiriman response = Pengiriman.builder().id(10L).build();

        when(pengirimanService.updateStatusPengiriman(eq(10L), eq(2L), eq(StatusPengiriman.MENGIRIM)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/pengiriman/10/status")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void rejectByMandor_requiresBody() throws Exception {
        UserPrincipal principal = new UserPrincipal(1L, "MANDOR");
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MANDOR"))
        );

        mockMvc.perform(patch("/api/pengiriman/7/mandor/reject")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectByMandor_acceptsValidBody() throws Exception {
        UserPrincipal principal = new UserPrincipal(1L, "MANDOR");
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MANDOR"))
        );

        MandorRejectionRequest request = new MandorRejectionRequest("Tidak sesuai");
        Pengiriman response = Pengiriman.builder().id(7L).build();
        when(pengirimanService.rejectByMandor(eq(7L), eq(1L), eq("Tidak sesuai")))
                .thenReturn(response);

        mockMvc.perform(patch("/api/pengiriman/7/mandor/reject")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void getById_returnsNotFoundWhenServiceThrows() throws Exception {
        UserPrincipal principal = new UserPrincipal(1L, "ADMIN");
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        when(pengirimanService.getPengirimanById(eq(404L)))
                .thenThrow(new IllegalArgumentException("Pengiriman not found"));

        mockMvc.perform(get("/api/pengiriman/404")
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }
}
