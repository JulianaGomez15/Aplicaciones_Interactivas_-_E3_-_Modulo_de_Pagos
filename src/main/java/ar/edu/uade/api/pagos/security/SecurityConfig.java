package ar.edu.uade.api.pagos.security;

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

import java.util.List;

import static ar.edu.uade.api.pagos.security.CuentasDemo.*;

@Configuration
public class SecurityConfig {

    private static final String PAGOS = "/api/v1/payments";

    private final JwtAuthenticationFilter jwtFilter;
    private final JsonSecurityErrorHandler errorHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter, JsonSecurityErrorHandler errorHandler) {
        this.jwtFilter = jwtFilter;
        this.errorHandler = errorHandler;
    }

    // Autorizacion por rol segun la tabla "Seguridad y autorizacion por rol" del TPO.
    // El orden importa: gana la primera regla que coincide.
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/v3/api-docs.yaml", "/h2-console/**").permitAll()

                        // API-01, API-02, API-03
                        .requestMatchers(HttpMethod.POST, PAGOS, PAGOS + "/*/approve", PAGOS + "/*/reject")
                        .hasRole(SERVICIO_ESCRITURA)
                        // API-05
                        .requestMatchers(HttpMethod.POST, PAGOS + "/*/refund")
                        .hasAnyRole(SERVICIO_ESCRITURA, ADMINISTRADOR)
                        // API-06 (tambien CU-07, que filtra por codigoPedido)
                        .requestMatchers(HttpMethod.GET, PAGOS)
                        .hasAnyRole(ADMINISTRADOR, ANALISTA, SERVICIO_LECTURA)
                        // API-08
                        .requestMatchers(HttpMethod.GET, PAGOS + "/*/history").hasRole(ADMINISTRADOR)
                        // API-04 (un CLIENTE solo ve sus pagos: lo controla el Service)
                        .requestMatchers(HttpMethod.GET, PAGOS + "/*")
                        .hasAnyRole(CLIENTE, ADMINISTRADOR, SERVICIO_LECTURA)
                        // API-07
                        .requestMatchers(HttpMethod.POST, "/api/v1/payment-methods").hasRole(CLIENTE)

                        .anyRequest().denyAll())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Solo para el login de demo (/auth/login). La validacion real de cada request es por firma del JWT.
    @Bean
    UserDetailsService users(PasswordEncoder encoder) {
        List<UserDetails> usuarios = CUENTAS.stream()
                .map(c -> User.withUsername(c.username()).password(encoder.encode(c.password()))
                        .roles(c.roles().toArray(new String[0])).build())
                .toList();
        return new InMemoryUserDetailsManager(usuarios);
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }
}
