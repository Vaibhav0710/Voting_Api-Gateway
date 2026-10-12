package com.voting.gateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voting.gateway.dto.ApiResponse;
import com.voting.gateway.dto.AuthValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway authentication for every route.
 * <ol>
 *   <li>Always strips client-sent {@code loggedInUser} / {@code role} headers, so nobody can claim an identity
 *       (or another user's rate-limit bucket) by setting them.</li>
 *   <li>Open endpoints ({@link RouterValidator}) pass through without a token.</li>
 *   <li>Otherwise the bearer token is verified by the User Service ({@link TokenValidator}) and the coarse role
 *       rules ({@link RoleAuthorizer}) are applied; the verified user id and role are then added as headers.</li>
 * </ol>
 * Errors are JSON {@link ApiResponse}s: 401 missing/invalid token, 403 wrong role, 503 User Service down.
 */
@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    static final String USER_HEADER = "loggedInUser";
    static final String ROLE_HEADER = "role";
    private static final String BEARER_PREFIX = "Bearer ";

    private final RouterValidator routerValidator;
    private final RoleAuthorizer roleAuthorizer;
    private final TokenValidator tokenValidator;
    private final ObjectMapper objectMapper;

    public AuthenticationFilter(RouterValidator routerValidator, RoleAuthorizer roleAuthorizer,
                                TokenValidator tokenValidator, ObjectMapper objectMapper) {
        super(Config.class);
        this.routerValidator = routerValidator;
        this.roleAuthorizer = roleAuthorizer;
        this.tokenValidator = tokenValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerWebExchange stripped = withoutIdentityHeaders(exchange);
            ServerHttpRequest request = stripped.getRequest();
            if (!routerValidator.isSecured(request)) {
                return chain.filter(stripped);
            }
            String token = bearerToken(request);
            if (token == null) {
                return reject(stripped, HttpStatus.UNAUTHORIZED, "Missing or malformed Authorization header");
            }
            return tokenValidator.validate(token)
                    .flatMap(user -> authorize(stripped, chain, user))
                    .onErrorResume(InvalidTokenException.class,
                            e -> reject(stripped, HttpStatus.UNAUTHORIZED, "Invalid or expired token"))
                    .onErrorResume(AuthServiceUnavailableException.class,
                            e -> reject(stripped, HttpStatus.SERVICE_UNAVAILABLE, "Authentication service unavailable"));
        };
    }

    private Mono<Void> authorize(ServerWebExchange exchange, GatewayFilterChain chain, AuthValidationResponse user) {
        ServerHttpRequest request = exchange.getRequest();
        if (!roleAuthorizer.isAllowed(request.getMethod(), request.getPath().value(), user.getRole())) {
            return reject(exchange, HttpStatus.FORBIDDEN, "Access denied");
        }
        ServerHttpRequest withIdentity = request.mutate()
                .header(USER_HEADER, user.getUserId().toString())
                .header(ROLE_HEADER, user.getRole())
                .build();
        return chain.filter(exchange.mutate().request(withIdentity).build());
    }

    private static ServerWebExchange withoutIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_HEADER);
                    headers.remove(ROLE_HEADER);
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private static String bearerToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX) || header.length() == BEARER_PREFIX.length()) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length());
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            DataBuffer body = response.bufferFactory().wrap(objectMapper.writeValueAsBytes(ApiResponse.error(message)));
            return response.writeWith(Mono.just(body));
        } catch (JsonProcessingException e) {
            log.error("Could not serialise gateway error response", e);
            return response.setComplete();
        }
    }

    public static class Config {
    }
}
