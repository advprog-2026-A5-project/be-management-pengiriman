package id.ac.ui.cs.advprog.bemanagementpengiriman.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class HeaderAuthenticationFilter extends HttpFilter {

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        String userIdHeader = req.getHeader("X-User-Id");
        String roleHeader = req.getHeader("X-User-Role");

        if (userIdHeader != null) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                var authorities = roleHeader == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + roleHeader.toUpperCase()));
                var principal = new UserPrincipal(userId, roleHeader);
                var auth = new UsernamePasswordAuthenticationToken( principal,  null, (Collection<? extends GrantedAuthority>) authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (NumberFormatException ignored) {
                // ignore invalid header
            }
        }

        chain.doFilter(req, res);
    }
}
