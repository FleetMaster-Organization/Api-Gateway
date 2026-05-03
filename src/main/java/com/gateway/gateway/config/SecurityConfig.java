package com.gateway.gateway.config;

import com.gateway.gateway.security.GatewayJwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad de la Gateway.
 *
 * Spring Security por defecto protege todas las rutas y exige autenticación propia.
 * Aquí lo desactivamos completamente (anyRequest().permitAll()) porque toda la
 * lógica de seguridad vive en GatewayJwtFilter, no en el mecanismo de Spring Security.
 * El filtro se registra manualmente antes del filtro estándar de Spring.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayJwtFilter gatewayJwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .addFilterBefore(gatewayJwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}