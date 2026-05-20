package id.ac.ui.cs.advprog.bemanagementpengiriman.security;

import lombok.Getter;

import java.util.Objects;

@Getter
public class UserPrincipal {
    private final Long id;
    private final String role;

    public UserPrincipal(Long id, String role) {
        this.id = id;
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPrincipal that = (UserPrincipal) o;
        return Objects.equals(id, that.id) && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, role);
    }
}
