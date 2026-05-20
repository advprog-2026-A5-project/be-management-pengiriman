package id.ac.ui.cs.advprog.bemanagementpengiriman.security;

import lombok.Getter;

@Getter
public class UserPrincipal {
    private final Long id;
    private final String role;

    public UserPrincipal(Long id, String role) {
        this.id = id;
        this.role = role;
    }
}
