package com.voting.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

class RoleAuthorizerTest {

    private final RoleAuthorizer authorizer = new RoleAuthorizer();

    @Test
    void castingAVote_needsVoter() {
        assertThat(authorizer.isAllowed(HttpMethod.POST, "/api/v1/votes", "ROLE_VOTER")).isTrue();
        assertThat(authorizer.isAllowed(HttpMethod.POST, "/api/v1/votes", "ROLE_ADMIN")).isFalse();
    }

    @Test
    void candidateWrites_needAdmin() {
        assertThat(authorizer.isAllowed(HttpMethod.POST, "/api/v1/candidates", "ROLE_ADMIN")).isTrue();
        assertThat(authorizer.isAllowed(HttpMethod.PATCH, "/api/v1/candidates/abc/status", "ROLE_VOTER")).isFalse();
        assertThat(authorizer.isAllowed(HttpMethod.DELETE, "/api/v1/candidates/abc", "ROLE_VOTER")).isFalse();
    }

    @Test
    void otherAuthenticatedRequests_areLeftToTheServices() {
        assertThat(authorizer.isAllowed(HttpMethod.GET, "/api/v1/votes/abc", "ROLE_VOTER")).isTrue();
        assertThat(authorizer.isAllowed(HttpMethod.POST, "/api/v1/votes/verify/abc", "ROLE_ADMIN")).isTrue();
    }

    @Test
    void unknownRole_isDeniedForRoleRestrictedRoutes() {
        assertThat(authorizer.isAllowed(HttpMethod.POST, "/api/v1/votes", null)).isFalse();
        assertThat(authorizer.isAllowed(HttpMethod.POST, "/api/v1/candidates", "ROLE_SUPERUSER")).isFalse();
    }
}
