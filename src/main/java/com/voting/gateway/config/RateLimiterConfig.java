package com.voting.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * Rate-limit bucket per caller: the verified user id when the request is authenticated, else the client IP.
 * <p>
 * The {@code loggedInUser} header is trustworthy here because {@link AuthenticationFilter} runs first on every
 * route, strips any client-sent copy and sets it only from a verified token. Prefixes keep user and IP buckets apart.
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst(AuthenticationFilter.USER_HEADER);
            if (userId != null) {
                return Mono.just("user:" + userId);
            }
            InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
            String ip = remote == null || remote.getAddress() == null ? "unknown" : remote.getAddress().getHostAddress();
            return Mono.just("ip:" + ip);
        };
    }
}
