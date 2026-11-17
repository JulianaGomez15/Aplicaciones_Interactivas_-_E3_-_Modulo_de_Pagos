package ar.edu.uade.api.pagos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Status", description = "Healthcheck del modulo para el resto del ecosistema.")
public class StatusController {

    @Operation(summary = "Healthcheck",
            description = "Siempre responde 200 con `{\"status\": \"OK\"}` mientras el servicio este levantado. "
                    + "Publico, no requiere JWT.",
            responses = @ApiResponse(responseCode = "200", description = "Servicio levantado"))
    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }
}
