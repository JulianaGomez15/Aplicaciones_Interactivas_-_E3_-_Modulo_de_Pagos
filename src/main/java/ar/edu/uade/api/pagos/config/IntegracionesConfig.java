package ar.edu.uade.api.pagos.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

// Arma un RestClient por cada servicio externo. Solo se activa con integraciones.modo=rest;
// en modo simulado no se crea nada de esto y se usan los *ClientSimulado.
@Configuration
@ConditionalOnProperty(name = "integraciones.modo", havingValue = "rest")
public class IntegracionesConfig {

    private final int timeoutConexionMs;
    private final int timeoutLecturaMs;
    private final String tokenServicio;

    public IntegracionesConfig(@Value("${integraciones.timeout-conexion-ms}") int timeoutConexionMs,
                               @Value("${integraciones.timeout-lectura-ms}") int timeoutLecturaMs,
                               @Value("${integraciones.token-servicio:}") String tokenServicio) {
        this.timeoutConexionMs = timeoutConexionMs;
        this.timeoutLecturaMs = timeoutLecturaMs;
        this.tokenServicio = tokenServicio;
    }

    @Bean
    public RestClient pedidosRestClient(@Value("${integraciones.pedidos.url}") String url) {
        return crear(url);
    }

    @Bean
    public RestClient usuariosRestClient(@Value("${integraciones.usuarios.url}") String url) {
        return crear(url);
    }

    @Bean
    public RestClient notificacionesRestClient(@Value("${integraciones.notificaciones.url}") String url) {
        return crear(url);
    }

    // Lo comun a las tres integraciones: URL base, timeouts, JSON, token y correlation id.
    private RestClient crear(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutConexionMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutLecturaMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor((request, body, execution) -> {
                    // JWT de servicio (seccion Seguridad del TPO: las llamadas entre APIs usan un JWT de servicio).
                    if (!tokenServicio.isBlank()) {
                        request.getHeaders().setBearerAuth(tokenServicio);
                    }
                    // Propaga el X-Correlation-Id del request original para seguir una operacion entre modulos.
                    String correlationId = MDC.get(CorrelationIdFilter.ATTRIBUTE);
                    if (correlationId != null) {
                        request.getHeaders().set(CorrelationIdFilter.HEADER, correlationId);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}
