package com.voting.gateway.config;

import com.voting.gateway.dto.ApiResponse;
import com.voting.gateway.dto.AuthValidationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Asks the User Service ({@code GET /api/v1/auth/validate}) who a bearer token belongs to.
 * <p>
 * 401/403 or an unsuccessful body → {@link InvalidTokenException}; any other failure (5xx, timeout,
 * connection refused) → {@link AuthServiceUnavailableException}, so a bad token is never reported as an outage.
 */
@Component
public class TokenValidator {

    private static final ParameterizedTypeReference<ApiResponse<AuthValidationResponse>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient webClient;
    private final String validateUrl;

    public TokenValidator(WebClient.Builder webClientBuilder,
                          @Value("${gateway.auth.validate-url:http://user-service/api/v1/auth/validate}")
                          String validateUrl) {
        this.webClient = webClientBuilder.build();
        this.validateUrl = validateUrl;
    }

    public Mono<AuthValidationResponse> validate(String token) {
        return webClient.get()
                .uri(validateUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(TokenValidator::isRejection, response -> Mono.error(new InvalidTokenException()))
                .bodyToMono(RESPONSE_TYPE)
                .flatMap(TokenValidator::userOf)
                .onErrorMap(error -> !(error instanceof InvalidTokenException), AuthServiceUnavailableException::new);
    }

    private static boolean isRejection(HttpStatusCode status) {
        return status.value() == HttpStatus.UNAUTHORIZED.value() || status.value() == HttpStatus.FORBIDDEN.value();
    }

    private static Mono<AuthValidationResponse> userOf(ApiResponse<AuthValidationResponse> response) {
        if (!response.isSuccess() || response.getData() == null) {
            return Mono.error(new InvalidTokenException());
        }
        return Mono.just(response.getData());
    }
}
