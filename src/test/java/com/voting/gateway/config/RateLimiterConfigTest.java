package com.voting.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterConfigTest {

    private final KeyResolver resolver = new RateLimiterConfig().userKeyResolver();

    @Test
    void authenticatedRequest_isLimitedPerUser() {
        // By the time the rate limiter runs, AuthenticationFilter has replaced this header with the verified user id
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/votes")
                .header("loggedInUser", "user-42")
                .remoteAddress(new InetSocketAddress("10.0.0.7", 5000)));

        assertThat(resolver.resolve(exchange).block()).isEqualTo("user:user-42");
    }

    @Test
    void anonymousRequest_isLimitedPerClientIp() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/results/x")
                .remoteAddress(new InetSocketAddress("10.0.0.7", 5000)));

        assertThat(resolver.resolve(exchange).block()).isEqualTo("ip:10.0.0.7");
    }

    @Test
    void userKeysAndIpKeys_cannotCollide() {
        MockServerWebExchange asUser = MockServerWebExchange.from(MockServerHttpRequest.get("/x")
                .header("loggedInUser", "10.0.0.7"));
        MockServerWebExchange asIp = MockServerWebExchange.from(MockServerHttpRequest.get("/x")
                .remoteAddress(new InetSocketAddress("10.0.0.7", 5000)));

        assertThat(resolver.resolve(asUser).block()).isNotEqualTo(resolver.resolve(asIp).block());
    }

    @Test
    void missingRemoteAddress_doesNotFail() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/x"));

        assertThat(resolver.resolve(exchange).block()).isEqualTo("ip:unknown");
    }
}
