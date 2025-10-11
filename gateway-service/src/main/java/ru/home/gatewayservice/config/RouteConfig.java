package ru.home.gatewayservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import javax.crypto.SecretKey;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class RouteConfig {

    @Value("${security.token}")
    private String secret;

    @Value("${gateway.auth-service.uri}")
    private String authServiceUri;

    @Value("${gateway.task-tracker.uri}")
    private String taskTrackerUri;

    @Bean
    public RouterFunction<ServerResponse> gatewayRouterFunctionsPath() {
        return route("auth-service")
                .route(path("/auth/**"), http())
                .before(uri(authServiceUri))
                .build().and(
            route("task-tracker")
                .route(path("/api/**"), http())
                .before(uri(taskTrackerUri))
                .filter(this::authenticationFilter)
                .build());
    }

    private ServerResponse authenticationFilter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        String authHeader = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            SecretKey key = Keys.hmacShaKeyFor(keyBytes);
            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid JWT: " + e.getMessage());
        }

        String username = claims.getSubject();
        if (username == null) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing username in token");
        }

        // Добавляем username в заголовок
        ServerRequest modifiedRequest = ServerRequest.from(request)
                .header("X-Username", username)
                .build();

        return next.handle(modifiedRequest);
    }
}
