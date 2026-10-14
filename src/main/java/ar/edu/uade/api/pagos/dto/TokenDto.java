package ar.edu.uade.api.pagos.dto;

public class TokenDto {

    private final String token;
    private final String tipo;
    private final long expiresInSeconds;
    private final String username;
    private final String rol;

    public TokenDto(String token, String tipo, long expiresInSeconds, String username, String rol) {
        this.token = token;
        this.tipo = tipo;
        this.expiresInSeconds = expiresInSeconds;
        this.username = username;
        this.rol = rol;
    }

    public String getToken() { return token; }
    public String getTipo() { return tipo; }
    public long getExpiresInSeconds() { return expiresInSeconds; }
    public String getUsername() { return username; }
    public String getRol() { return rol; }
}
