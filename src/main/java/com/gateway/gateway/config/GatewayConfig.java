package com.gateway.gateway.config;

import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;

/**
 * Rutas de la API Gateway hacia cada microservicio.
 * Usa lb:// (load balancer) para resolver los nombres de servicio via Eureka.
 *
 * Convención de prefijos de ruta → microservicio:
 *  /auth/**          → auth-security
 *  /vehicles/**      → vehicle-services
 *  /drivers/**       → driver-services
 *  /maintenance/**   → maintenance-services
 *  /alerts/**        → alert-services
 *  /assignments/**   → assignement-services
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouterFunction<ServerResponse> authRoutes() {
        return GatewayRouterFunctions.route("auth-service")
                .route(request -> request.path().startsWith("/auth") ||
                                request.path().startsWith("/admin"),
                        HandlerFunctions.http())
                .filter(lb("auth-security"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> vehicleRoutes() {
        return GatewayRouterFunctions.route("vehicle-service")
                .route(request -> request.path().startsWith("/vehicles"),
                        HandlerFunctions.http())
                .filter(lb("vehicle-services"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> driverRoutes() {
        return GatewayRouterFunctions.route("driver-service")
                .route(request -> request.path().startsWith("/drivers"),
                        HandlerFunctions.http())
                .filter(lb("driver-services"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> maintenanceRoutes() {
        return GatewayRouterFunctions.route("maintenance-service")
                .route(request -> request.path().startsWith("/maintenance"),
                        HandlerFunctions.http())
                .filter(lb("maintenance-services"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> alertRoutes() {
        return GatewayRouterFunctions.route("alert-service")
                .route(request -> request.path().startsWith("/alerts"),
                        HandlerFunctions.http())
                .filter(lb("alert-services"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> assignmentRoutes() {
        return GatewayRouterFunctions.route("assignment-service")
                .route(request -> request.path().startsWith("/assignments"),
                        HandlerFunctions.http())
                .filter(lb("assignement-services"))
                .build();
    }
}