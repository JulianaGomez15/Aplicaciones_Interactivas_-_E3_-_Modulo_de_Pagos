package ar.edu.uade.api.pagos.controller;

import ar.edu.uade.api.pagos.dto.LoginDto;
import ar.edu.uade.api.pagos.dto.TokenDto;
import ar.edu.uade.api.pagos.security.CuentasDemo;
import ar.edu.uade.api.pagos.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth (solo demo)",
        description = "Simula al Servicio de Usuarios, que en el ecosistema es el unico emisor de JWT. "
                + "Cuentas: admin/admin123, analista/analista123, cliente/cliente123 (Ana), cliente2/cliente123 "
                + "(Bruno), api-pedidos/pedidos123 (escritura y lectura), api-analitica/analitica123 (lectura).")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Operation(summary = "Login de demo",
            description = "Devuelve un JWT con los roles y, para CLIENTE, el codigoUsuario. Usarlo como "
                    + "`Authorization: Bearer <token>` (boton Authorize).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "JWT emitido"),
                    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: credenciales incorrectas")
            })
    @PostMapping("/login")
    public ResponseEntity<TokenDto> login(@RequestBody LoginDto dto) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .toList();

        CuentasDemo.Cuenta cuenta = CuentasDemo.porUsername(authentication.getName());
        String token = jwtService.generarToken(authentication.getName(), roles,
                cuenta == null ? null : cuenta.codigoUsuario());
        return ResponseEntity.ok(new TokenDto(token, "Bearer", jwtService.getExpirationSeconds(),
                authentication.getName(), roles));
    }
}
