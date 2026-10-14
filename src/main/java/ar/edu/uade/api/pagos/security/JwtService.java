package ar.edu.uade.api.pagos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey clave;
    private final long expirationMillis;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.clave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMinutes * 60 * 1000L;
    }

    public String generarToken(String username, String rol) {
        Date creacion = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("role", rol)
                .issuedAt(creacion)
                .expiration(new Date(creacion.getTime() + expirationMillis))
                .signWith(clave)
                .compact();
    }

    public String obtenerUsername(String token) {
        return obtenerClaims(token).getSubject();
    }

    public String obtenerRol(String token) {
        return obtenerClaims(token).get("role", String.class);
    }

    public long getExpirationSeconds() {
        return expirationMillis / 1000;
    }

    // Verifica firma y vencimiento; lanza JwtException si el token no es valido.
    private Claims obtenerClaims(String token) {
        return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
    }
}
