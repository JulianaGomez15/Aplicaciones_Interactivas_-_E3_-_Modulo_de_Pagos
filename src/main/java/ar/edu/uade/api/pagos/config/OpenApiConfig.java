package ar.edu.uade.api.pagos.config;

import ar.edu.uade.api.pagos.dto.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pagosOpenAPI() {
        OpenAPI api = new OpenAPI()
                .info(new Info()
                        .title("DeliveryGo - Modulo de Pagos")
                        .version("1.0")
                        .description("""
                                API REST del **Modulo de Pagos** (Grupo 3, Aplicaciones Interactivas). Registra \
                                intentos de pago de pedidos, los autoriza o rechaza de forma **simulada**, gestiona \
                                reembolsos totales o parciales y guarda el historial de estados. No procesa dinero \
                                real ni guarda datos reales de tarjetas.

                                **Autenticacion:** JWT en `Authorization: Bearer <token>`. En el ecosistema lo emite \
                                el Servicio de Usuarios; aca `POST /auth/login` lo simula con cuentas de demo. \
                                Roles: ADMINISTRADOR, ANALISTA, CLIENTE, SERVICIO_ESCRITURA y SERVICIO_LECTURA \
                                (JWT de servicio entre APIs).

                                **Trazabilidad:** cada operacion indica su ID de API (API-01 a API-08) segun el \
                                documento de la Entrega 1.

                                **Errores:** todos usan el formato `ErrorResponse` (`timestamp`, `status`, `code`, \
                                `message`, `path`, `correlationId`). El `correlationId` se toma del header \
                                `X-Correlation-Id`.

                                **Datos simulados** de otros modulos: pedidos BORRADOR \
                                `00000000-0000-0000-0001-00000000000{1,2,3}` (totales 15000.00, 8500.50 y 32000.00), \
                                usuarios `00000000-0000-0000-0000-00000000000{1,2}`."""))
                .addServersItem(new Server().url("/").description("Servidor actual"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT obtenido en POST /auth/login")));

        // El esquema de error se registra aunque ningun metodo lo declare como tipo de retorno.
        ModelConverters.getInstance().readAll(ErrorResponse.class).forEach(api.getComponents()::addSchemas);
        return api;
    }

    // Toda respuesta 4xx/5xx de la API devuelve ErrorResponse (DTO-10): se documenta una sola vez, aca.
    @Bean
    public OperationCustomizer errorResponseCustomizer() {
        return (Operation operation, org.springframework.web.method.HandlerMethod handlerMethod) -> {
            if (operation.getResponses() != null) {
                operation.getResponses().forEach((codigo, respuesta) -> {
                    if (codigo.startsWith("4") || codigo.startsWith("5")) {
                        conEsquemaDeError(respuesta);
                    }
                });
            }
            return operation;
        };
    }

    private void conEsquemaDeError(ApiResponse respuesta) {
        Schema<?> esquema = new Schema<>().$ref("#/components/schemas/ErrorResponse");
        respuesta.setContent(new Content().addMediaType("application/json", new MediaType().schema(esquema)));
    }
}
