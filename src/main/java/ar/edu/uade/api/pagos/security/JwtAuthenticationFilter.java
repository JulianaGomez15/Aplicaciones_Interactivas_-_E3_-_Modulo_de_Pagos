package ar.edu.uade.api.pagos.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // Sin JWT se continua: Spring Security decide despues si el endpoint exige autenticacion.
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            JwtPrincipal principal = jwtService.leer(header.substring(7));

            if (principal.getUsername() != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, null,
                                principal.getRoles().stream()
                                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList()));
            }
        } catch (JwtException | IllegalArgumentException ex) {
            // Token invalido, vencido o con claims mal formados: no se autentica, el entry point responde 401.
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
