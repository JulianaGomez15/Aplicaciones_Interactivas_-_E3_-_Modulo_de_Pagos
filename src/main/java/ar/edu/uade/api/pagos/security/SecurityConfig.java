package ar.edu.uade.api.pagos.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private static final String PAGOS = "/api/v1/payments";

    private final JwtAuthenticationFilter jwtFilter;
    private final JsonSecurityErrorHandler errorHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter, JsonSecurityErrorHandler errorHandler) {
        this.jwtFilter = jwtFilter;
        this.errorHandler = errorHandler;
    }

    // Roles: ADMIN, PEDIDOS (Servicio de Pedidos), ANALISTA, CLIENTE (Portal Web).
    // El orden de las reglas importa: gana la primera que coincide.
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> {
                })
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Escrituras sobre pagos: iniciar, aprobar, rechazar, reembolsar.
                        .requestMatchers(HttpMethod.POST, PAGOS, PAGOS + "/*/approve", PAGOS + "/*/reject",
                                PAGOS + "/*/refund").hasAnyRole("ADMIN", "PEDIDOS")

                        // Auditoria / trazabilidad.
                        .requestMatchers(HttpMethod.GET, PAGOS, PAGOS + "/order/*", PAGOS + "/*/history")
                        .hasAnyRole("ADMIN", "ANALISTA")
                        .requestMatchers(HttpMethod.GET, PAGOS + "/*/refunds", "/api/v1/refunds/*")
                        .hasAnyRole("ADMIN", "ANALISTA", "PEDIDOS")

                        // Consulta de un pago: cualquier usuario autenticado.
                        .requestMatchers(HttpMethod.GET, PAGOS + "/*").authenticated()

                        .requestMatchers("/api/v1/payment-methods/**").hasAnyRole("ADMIN", "CLIENTE")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Usuarios de demo (solo academico).
    @Bean
    UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                demo(encoder, "admin", "admin123", "ADMIN"),
                demo(encoder, "pedidos", "pedidos123", "PEDIDOS"),
                demo(encoder, "analista", "analista123", "ANALISTA"),
                demo(encoder, "cliente", "cliente123", "CLIENTE"));
    }

    private UserDetails demo(PasswordEncoder encoder, String username, String password, String rol) {
        return User.withUsername(username).password(encoder.encode(password)).roles(rol).build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origin}") String origin) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of(origin));
        c.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
        c.setExposedHeaders(List.of("X-Correlation-Id"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }
}
