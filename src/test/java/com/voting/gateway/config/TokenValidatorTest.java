package com.voting.gateway.config;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TokenValidator} against a stand-in user-service ({@link MockWebServer}).
 */
class TokenValidatorTest {

    private MockWebServer userService;
    private TokenValidator validator;

    @BeforeEach
    void setUp() throws IOException {
        userService = new MockWebServer();
        userService.start();
        validator = new TokenValidator(WebClient.builder(), userService.url("/api/v1/auth/validate").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        userService.shutdown();
    }

    private void respond(int status, String body) {
        userService.enqueue(new MockResponse().setResponseCode(status)
                .setHeader("Content-Type", "application/json").setBody(body));
    }

    @Test
    void validToken_returnsUserAndForwardsBearer() throws InterruptedException {
        respond(200, "{\"success\":true,\"data\":{\"userId\":\"00000000-0000-0000-0000-000000000001\","
                + "\"username\":\"asha\",\"role\":\"ROLE_VOTER\"}}");

        var user = validator.validate("tok").block();

        assertThat(user.getRole()).isEqualTo("ROLE_VOTER");
        RecordedRequest request = userService.takeRequest();
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer tok");
    }

    @Test
    void userServiceSays401_isInvalidToken() {
        respond(401, "{\"success\":false,\"message\":\"Authentication required\"}");

        assertThatThrownBy(() -> validator.validate("tok").block()).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void unsuccessfulBody_isInvalidToken() {
        respond(200, "{\"success\":false}");

        assertThatThrownBy(() -> validator.validate("tok").block()).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void userService5xx_isUnavailable() {
        respond(500, "{}");

        assertThatThrownBy(() -> validator.validate("tok").block())
                .isInstanceOf(AuthServiceUnavailableException.class);
    }

    @Test
    void userServiceUnreachable_isUnavailable() throws IOException {
        userService.shutdown();

        assertThatThrownBy(() -> validator.validate("tok").block())
                .isInstanceOf(AuthServiceUnavailableException.class);
    }
}
