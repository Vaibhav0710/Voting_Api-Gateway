package com.voting.gateway.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimiterPropertiesTest {

    private static FilterDefinition findRateLimiterFilter(GatewayProperties properties, String routeId) {
        RouteDefinition route = properties.getRoutes().stream()
                .filter(r -> routeId.equals(r.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Route not found: " + routeId));

        return route.getFilters().stream()
                .filter(f -> "RequestRateLimiter".equals(f.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("RequestRateLimiter filter not found on route: " + routeId));
    }

    @Nested
    @SpringBootTest
    @TestPropertySource(properties = {
            "eureka.client.enabled=false"
    })
    class DefaultsTest {

        @Autowired
        private GatewayProperties gatewayProperties;

        @Test
        void authRoute_rateLimitDefaults_are10And20() {
            FilterDefinition filter = findRateLimiterFilter(gatewayProperties, "user-service");

            assertThat(filter.getArgs())
                    .containsEntry("redis-rate-limiter.replenishRate", "10")
                    .containsEntry("redis-rate-limiter.burstCapacity", "20");
        }
    }

    @Nested
    @SpringBootTest
    @TestPropertySource(properties = {
            "eureka.client.enabled=false",
            "GATEWAY_AUTH_REPLENISH_RATE=100",
            "GATEWAY_AUTH_BURST_CAPACITY=200"
    })
    class OverridesTest {

        @Autowired
        private GatewayProperties gatewayProperties;

        @Test
        void authRoute_rateLimits_areConfigurableViaEnvVars() {
            FilterDefinition filter = findRateLimiterFilter(gatewayProperties, "user-service");

            assertThat(filter.getArgs())
                    .containsEntry("redis-rate-limiter.replenishRate", "100")
                    .containsEntry("redis-rate-limiter.burstCapacity", "200");
        }
    }
}
