package id.ac.ui.cs.advprog.bemanagementpengiriman.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {

    private Long id;
    private String username;
    private String email;
    private String nama;
    private String role;

    public UserSummary(Long id, String username) {
        this.id = id;
        this.username = username;
        this.nama = username;
    }
}
