package com.voting.gateway.config;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

/**
 * Decides which requests may pass the gateway without a token.
 * <p>
 * Each open endpoint is an exact (method, path pattern) pair, matched against the whole path, so an open
 * path embedded inside another path (e.g. {@code /api/v1/votes/x/api/v1/results}) is still secured.
 * Services enforce their own rules too; this is the first gate, not the only one.
 */
@Component
public class RouterValidator {

    private record OpenEndpoint(HttpMethod method, PathPattern pattern) {

        boolean matches(ServerHttpRequest request) {
            return method.equals(request.getMethod()) && pattern.matches(request.getPath().pathWithinApplication());
        }
    }

    private static final PathPatternParser PARSER = PathPatternParser.defaultInstance;

    private static final List<OpenEndpoint> OPEN_ENDPOINTS = List.of(
            open(HttpMethod.POST, "/api/v1/auth/register"),
            open(HttpMethod.POST, "/api/v1/auth/login"),
            open(HttpMethod.GET, "/api/v1/candidates/**"),
            open(HttpMethod.GET, "/api/v1/votes/election/*/count"),
            open(HttpMethod.GET, "/api/v1/results/**")
    );

    public boolean isSecured(ServerHttpRequest request) {
        return OPEN_ENDPOINTS.stream().noneMatch(endpoint -> endpoint.matches(request));
    }

    private static OpenEndpoint open(HttpMethod method, String pattern) {
        return new OpenEndpoint(method, PARSER.parse(pattern));
    }
}
