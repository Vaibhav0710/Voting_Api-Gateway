package com.voting.gateway.config;

import com.voting.gateway.dto.ApiResponse;
import com.voting.gateway.dto.AuthValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouterValidator routerValidator;
    private final WebClient.Builder webClientBuilder;

    public AuthenticationFilter(RouterValidator routerValidator, WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.routerValidator = routerValidator;
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (routerValidator.isSecured.test(request)) {
                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    return onError(exchange, "Missing authorization header", HttpStatus.UNAUTHORIZED);
                }

                String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return onError(exchange, "Invalid authorization header", HttpStatus.UNAUTHORIZED);
                }

                String token = authHeader.substring(7);

                return webClientBuilder.build()
                        .get()
                        .uri("http://user-service/api/v1/auth/validate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<ApiResponse<AuthValidationResponse>>() {})
                        .flatMap(apiResponse -> {
                            if (apiResponse.isSuccess()) {
                                AuthValidationResponse user = apiResponse.getData();
                                log.info("Authenticated user: {}, role: {}", user.getUsername(), user.getRole());
                                
                                // Add headers for downstream services
                                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                                        .header("loggedInUser", user.getUserId().toString())
                                        .header("role", user.getRole())
                                        .build();
                                
                                return chain.filter(exchange.mutate().request(modifiedRequest).build());
                            } else {
                                return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
                            }
                        })
                        .onErrorResume(e -> {
                            log.error("Authentication error: {}", e.getMessage());
                            return onError(exchange, "Authentication service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
                        });
            }
            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    public static class Config {
    }
}
