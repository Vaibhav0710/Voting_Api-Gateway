package com.voting.gateway.config;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Coarse role rules applied at the gateway after the token is verified, so obviously forbidden requests
 * never reach a service: only voters cast votes, only admins change candidates. Finer rules (vote owner,
 * admin-only reads) stay in the services.
 */
@Component
public class RoleAuthorizer {

    static final String VOTER = "ROLE_VOTER";
    static final String ADMIN = "ROLE_ADMIN";

    private static final PathPattern VOTES = PathPatternParser.defaultInstance.parse("/api/v1/votes");
    private static final PathPattern CANDIDATES = PathPatternParser.defaultInstance.parse("/api/v1/candidates/**");

    public boolean isAllowed(HttpMethod method, String path, String role) {
        PathContainer pathContainer = PathContainer.parsePath(path);
        if (HttpMethod.POST.equals(method) && VOTES.matches(pathContainer)) {
            return VOTER.equals(role);
        }
        if (!HttpMethod.GET.equals(method) && CANDIDATES.matches(pathContainer)) {
            return ADMIN.equals(role);
        }
        return true;
    }
}
