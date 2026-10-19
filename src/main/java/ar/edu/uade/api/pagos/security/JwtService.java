package ar.edu.uade.api.pagos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey clave;
    private final long expirationMillis;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.clave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMinutes * 60 * 1000L;
    }

    public String generarToken(String username, List<String> roles, UUID codigoUsuario) {
        Date creacion = new Date();
        var builder = Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(creacion)
                .expiration(new Date(creacion.getTime() + expirationMillis));
        if (codigoUsuario != null) {
            builder.claim("codigoUsuario", codigoUsuario.toString());
        }
        return builder.signWith(clave).compact();
    }

    // Verifica firma y vencimiento; lanza JwtException si el token no es valido.
    public JwtPrincipal leer(String token) {
        Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();

        List<?> crudos = claims.get("roles", List.class);
        List<String> roles = crudos == null ? List.of() : crudos.stream().map(String::valueOf).toList();

        String codigo = claims.get("codigoUsuario", String.class);
        return new JwtPrincipal(claims.getSubject(), codigo == null ? null : UUID.fromString(codigo), roles);
    }

    public long getExpirationSeconds() {
        return expirationMillis / 1000;
    }
}
