package com.voting.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Loads the full gateway (routes, filters, circuit breakers) and checks behaviour that needs no
 * downstream service: CORS preflight, the JSON fallback, and early rejection of unauthenticated requests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "CORS_ALLOWED_ORIGINS=http://localhost:3000"
})
class GatewayContextTest {

    @Autowired private WebTestClient webTestClient;

    @Test
    void corsPreflight_fromAllowedOrigin_isAccepted() {
        webTestClient.options().uri("/api/v1/candidates")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000");
    }

    @Test
    void corsPreflight_fromUnknownOrigin_isRejected() {
        webTestClient.options().uri("/api/v1/candidates")
                .header(HttpHeaders.ORIGIN, "https://evil.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void securedRoute_withoutToken_is401Json() {
        webTestClient.post().uri("/api/v1/votes")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.success").isEqualTo(false);
    }

    @Test
    void fallback_returns503Json() {
        webTestClient.get().uri("/fallback/candidate-service")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").isEqualTo("candidate-service is temporarily unavailable. Please retry shortly.");
    }
}
