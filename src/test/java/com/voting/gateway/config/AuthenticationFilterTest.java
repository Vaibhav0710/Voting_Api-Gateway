package com.voting.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voting.gateway.dto.AuthValidationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthenticationFilterTest {

    private final TokenValidator tokenValidator = mock(TokenValidator.class);
    private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    private final GatewayFilterChain chain = exchange -> {
        forwarded.set(exchange);
        return Mono.empty();
    };
    private GatewayFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuthenticationFilter(new RouterValidator(), new RoleAuthorizer(), tokenValidator,
                new ObjectMapper().findAndRegisterModules())
                .apply(new AuthenticationFilter.Config());
    }

    private MockServerWebExchange run(MockServerHttpRequest.BaseBuilder<?> request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        filter.filter(exchange, chain).block();
        return exchange;
    }

    private static AuthValidationResponse user(UUID id, String role) {
        return AuthValidationResponse.builder().userId(id).username("asha").role(role).build();
    }

    @Test
    void spoofedIdentityHeadersOnOpenRoute_areStripped() {
        run(MockServerHttpRequest.get("/api/v1/results/election/abc")
                .header("loggedInUser", "victim-id")
                .header("role", "ROLE_ADMIN"));

        assertThat(forwarded.get().getRequest().getHeaders().containsKey("loggedInUser")).isFalse();
        assertThat(forwarded.get().getRequest().getHeaders().containsKey("role")).isFalse();
        verifyNoInteractions(tokenValidator);
    }

    @Test
    void validVoterToken_forwardsVerifiedIdentityOnly() {
        UUID voterId = UUID.randomUUID();
        when(tokenValidator.validate("good-token")).thenReturn(Mono.just(user(voterId, "ROLE_VOTER")));

        run(MockServerHttpRequest.post("/api/v1/votes")
                .header("Authorization", "Bearer good-token")
                .header("loggedInUser", "someone-else"));

        assertThat(forwarded.get().getRequest().getHeaders().get("loggedInUser")).containsExactly(voterId.toString());
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("role")).isEqualTo("ROLE_VOTER");
    }

    @Test
    void missingToken_onSecuredRoute_is401() {
        MockServerWebExchange exchange = run(MockServerHttpRequest.post("/api/v1/votes"));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("\"success\":false");
        assertThat(forwarded.get()).isNull();
    }

    @Test
    void invalidToken_is401_notServiceUnavailable() {
        when(tokenValidator.validate("bad-token")).thenReturn(Mono.error(new InvalidTokenException()));

        MockServerWebExchange exchange = run(MockServerHttpRequest.post("/api/v1/votes")
                .header("Authorization", "Bearer bad-token"));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(forwarded.get()).isNull();
    }

    @Test
    void userServiceDown_is503() {
        when(tokenValidator.validate("any")).thenReturn(Mono.error(new AuthServiceUnavailableException(null)));

        MockServerWebExchange exchange = run(MockServerHttpRequest.post("/api/v1/votes")
                .header("Authorization", "Bearer any"));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void adminCastingVote_is403() {
        when(tokenValidator.validate("admin-token")).thenReturn(Mono.just(user(UUID.randomUUID(), "ROLE_ADMIN")));

        MockServerWebExchange exchange = run(MockServerHttpRequest.post("/api/v1/votes")
                .header("Authorization", "Bearer admin-token"));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(forwarded.get()).isNull();
    }
}
