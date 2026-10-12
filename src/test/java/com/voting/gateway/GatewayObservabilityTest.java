package com.voting.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gateway metrics (including per-route gateway timings) are scrapeable and health probes are exposed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@TestPropertySource(properties = "eureka.client.enabled=false")
class GatewayObservabilityTest {

    @Autowired private WebTestClient webTestClient;

    @Test
    void prometheusEndpoint_exposesMetricsWithServiceTag() {
        String body = webTestClient.get().uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(body).contains("application=\"api-gateway\"");
    }

    @Test
    void readinessProbe_isExposed() {
        webTestClient.get().uri("/actuator/health/readiness").exchange().expectStatus().isOk();
    }
}
