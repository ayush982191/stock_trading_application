package com.trading.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Base64;

@Component
@Slf4j
public class JWTAuthFilter extends AbstractGatewayFilterFactory<JWTAuthFilter.Config> {

    @Value("${jwt.secret}")
    private String secret;

    public JWTAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // 1. Skip public endpoints
            if (isPublicEndpoint(path)) {
                log.debug("Public endpoint, skipping JWT: {}", path);
                return chain.filter(exchange);
            }

            // 2. Require Authorization header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorizedResponse(exchange, "Authorization header missing or invalid");
            }

            // 3. Extract token
            String token = authHeader.substring(7);

            // 4. Validate token and extract claims
            try {
                Claims claims = extractToken(token);
                String userId = claims.get("userId", String.class);
                String email = claims.getSubject();
                log.debug("Authenticated userId={} email={}", userId, email);

                // 5. Mutate request: forward identity downstream
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", userId != null ? userId : "")
                        .header("X-User-Email", email != null ? email : "")
                        .build();

                return chain.filter(
                        exchange.mutate().request(modifiedRequest).build()
                );
            } catch (Exception e) {
                log.warn("Invalid JWT token: {}", e.getMessage());
                return unauthorizedResponse(exchange, "Invalid or expired token");
            }
        };
    }

    private Claims extractToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        log.warn("Unauthorized: {}", message);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private boolean isPublicEndpoint(String path) {
        return path.contains("/register")
                || path.contains("/login")
                || path.contains("/health")
                || path.contains("/websocket");
    }

    public static class Config {
        // Add filter-specific config fields here if needed
    }
}