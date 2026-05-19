package com.gateway.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro JWT de la API Gateway — única capa del sistema que valida firma y expiración.
 *
 * Responsabilidades:
 *  1. Dejar pasar rutas públicas sin token.
 *  2. Exigir JWT válido en todas las demás rutas.
 *  3. Aplicar autorización gruesa por roles según el patrón de ruta.
 *  4. Reenviar la petición al microservicio conservando el header Authorization
 *     para que el JwtParsingFilter de cada servicio pueda extraer los claims.
 *
 * Rutas públicas (sin token):
 *  - POST /auth/login
 *  - POST /auth/refresh
 *  - Documentación Swagger / OpenAPI (/swagger-ui, /v3/api-docs, ...)
 *  - Endpoints de Actuator (/actuator/**) para health checks y Prometheus
 *
 * Rutas autenticadas (cualquier rol válido):
 *  - POST /auth/logout
 *
 * Rutas por rol:
 *  - /admin/**           → ROLE_ADMINISTRADOR
 *  - /vehicles/**        → ROLE_ADMINISTRADOR, ROLE_COORDINADOR, ROLE_DESPACHADOR
 *  - /drivers/**         → ROLE_ADMINISTRADOR, ROLE_COORDINADOR, ROLE_DESPACHADOR
 *  - /maintenance/**     → ROLE_ADMINISTRADOR, ROLE_COORDINADOR, ROLE_MECANICO
 *  - /alerts/**          → ROLE_ADMINISTRADOR, ROLE_COORDINADOR, ROLE_DESPACHADOR, ROLE_MECANICO
 *  - /assignments/**     → ROLE_ADMINISTRADOR, ROLE_COORDINADOR, ROLE_DESPACHADOR
 */
@Component
@RequiredArgsConstructor
public class GatewayJwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path   = request.getRequestURI();
        String method = request.getMethod();

        if (isPublicRoute(path, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        if (token == null) {
            sendError(response, HttpStatus.UNAUTHORIZED, "Token requerido");
            return;
        }

        if (!jwtUtil.validateToken(token)) {
            sendError(response, HttpStatus.UNAUTHORIZED, "Token inválido o expirado");
            return;
        }

        List<String> roles = jwtUtil.extractRoles(token);

        if (!isAuthorized(path, roles)) {
            sendError(response, HttpStatus.FORBIDDEN, "No tienes permisos para acceder a este recurso");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicRoute(String path, String method) {
        return (path.equals("/auth/login")   && method.equals("POST")) ||
                (path.equals("/auth/refresh") && method.equals("POST")) ||
                isDocsOrMonitoringRoute(path);
    }

    /**
     * Rutas de documentación y monitoreo que deben ser accesibles sin token,
     * tanto en la propia Gateway como en cualquier microservicio enrutado
     * (ej: /alert-services/actuator/health vía discovery locator).
     *
     * Usamos contains() porque al reenviar vía Eureka el path puede llevar
     * el prefijo del servicio antes del segmento real.
     */
    private boolean isDocsOrMonitoringRoute(String path) {
        return path.contains("/actuator")
                || path.contains("/swagger-ui")
                || path.contains("/v3/api-docs")
                || path.contains("/swagger-resources")
                || path.contains("/webjars");
    }

    private boolean isAuthorized(String path, List<String> roles) {
        if (path.startsWith("/admin/")) {
            return hasAnyRole(roles, "ROLE_ADMINISTRADOR");
        }
        if (path.startsWith("/vehicles/")) {
            return hasAnyRole(roles, "ROLE_ADMINISTRADOR", "ROLE_COORDINADOR", "ROLE_DESPACHADOR");
        }
        if (path.startsWith("/drivers/")) {
            return hasAnyRole(roles, "ROLE_ADMINISTRADOR", "ROLE_COORDINADOR", "ROLE_DESPACHADOR");
        }
        if (path.startsWith("/maintenance/")) {
            return hasAnyRole(roles, "ROLE_ADMINISTRADOR", "ROLE_COORDINADOR", "ROLE_MECANICO");
        }
        if (path.startsWith("/alerts/")) {
            return hasAnyRole(roles, "ROLE_ADMINISTRADOR", "ROLE_COORDINADOR", "ROLE_DESPACHADOR", "ROLE_MECANICO");
        }
        if (path.startsWith("/assignments/")) {
            return hasAnyRole(roles, "ROLE_ADMINISTRADOR", "ROLE_COORDINADOR", "ROLE_DESPACHADOR");
        }
        // Cualquier otra ruta autenticada (ej: /auth/logout): solo exige token válido
        return true;
    }

    private boolean hasAnyRole(List<String> userRoles, String... requiredRoles) {
        if (userRoles == null) return false;
        for (String required : requiredRoles) {
            if (userRoles.contains(required)) return true;
        }
        return false;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"status\":" + status.value() +
                        ",\"error\":\"" + status.getReasonPhrase() + "\"" +
                        ",\"message\":\"" + message + "\"}"
        );
    }
}