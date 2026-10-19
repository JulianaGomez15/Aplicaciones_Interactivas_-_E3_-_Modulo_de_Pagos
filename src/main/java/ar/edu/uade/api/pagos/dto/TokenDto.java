package ar.edu.uade.api.pagos.dto;

import java.util.List;

public class TokenDto {

    private final String token;
    private final String tipo;
    private final long expiresInSeconds;
    private final String username;
    private final List<String> roles;

    public TokenDto(String token, String tipo, long expiresInSeconds, String username, List<String> roles) {
        this.token = token;
        this.tipo = tipo;
        this.expiresInSeconds = expiresInSeconds;
        this.username = username;
        this.roles = roles;
    }

    public String getToken() { return token; }
    public String getTipo() { return tipo; }
    public long getExpiresInSeconds() { return expiresInSeconds; }
    public String getUsername() { return username; }
    public List<String> getRoles() { return roles; }
}
