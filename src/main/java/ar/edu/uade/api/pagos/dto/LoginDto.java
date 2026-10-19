package ar.edu.uade.api.pagos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Credenciales de demo")
public class LoginDto {

    @Schema(example = "api-pedidos")
    private String username;

    @Schema(example = "pedidos123")
    private String password;

    public LoginDto() {
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
