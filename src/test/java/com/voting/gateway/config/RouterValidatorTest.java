package com.voting.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RouterValidatorTest {

    private final RouterValidator validator = new RouterValidator();

    private boolean secured(HttpMethod method, String path) {
        return validator.isSecured(MockServerHttpRequest.method(method, path).build());
    }

    @Test
    void registerAndLogin_areOpen() {
        assertThat(secured(HttpMethod.POST, "/api/v1/auth/register")).isFalse();
        assertThat(secured(HttpMethod.POST, "/api/v1/auth/login")).isFalse();
    }

    @Test
    void readingResultsAndCandidates_isOpen() {
        assertThat(secured(HttpMethod.GET, "/api/v1/results/election/11111111-1111-1111-1111-111111111111")).isFalse();
        assertThat(secured(HttpMethod.GET, "/api/v1/candidates")).isFalse();
        assertThat(secured(HttpMethod.GET, "/api/v1/candidates/election/abc/active")).isFalse();
        assertThat(secured(HttpMethod.GET, "/api/v1/votes/election/abc/count")).isFalse();
    }

    @Test
    void writesAndVotes_areSecured() {
        assertThat(secured(HttpMethod.POST, "/api/v1/votes")).isTrue();
        assertThat(secured(HttpMethod.POST, "/api/v1/candidates")).isTrue();
        assertThat(secured(HttpMethod.DELETE, "/api/v1/candidates/abc")).isTrue();
        assertThat(secured(HttpMethod.GET, "/api/v1/votes/abc")).isTrue();
        assertThat(secured(HttpMethod.GET, "/api/v1/users/abc")).isTrue();
        assertThat(secured(HttpMethod.GET, "/api/v1/auth/validate")).isTrue();
    }

    @Test
    void openPathEmbeddedInsideAnotherPath_isStillSecured() {
        assertThat(secured(HttpMethod.POST, "/api/v1/votes/x/api/v1/results")).isTrue();
        assertThat(secured(HttpMethod.GET, "/api/v1/votes/api/v1/auth/login")).isTrue();
    }

    @Test
    void openPathWithWrongMethod_isSecured() {
        assertThat(secured(HttpMethod.GET, "/api/v1/auth/register")).isTrue();
        assertThat(secured(HttpMethod.DELETE, "/api/v1/results/election/abc")).isTrue();
    }
}
