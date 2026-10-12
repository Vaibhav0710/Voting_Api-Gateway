package com.voting.gateway.controller;

import com.voting.gateway.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Target of the per-route circuit breakers ({@code fallbackUri: forward:/fallback/<service>}): answers 503 JSON
 * when a service is down, slow or has no registered instance, instead of a raw gateway error.
 * <p>
 * For {@code POST /api/v1/votes} a 503 can follow a timeout after the vote was in fact stored; clients retry with
 * the same idempotency key and receive the original receipt.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/{service}")
    public ResponseEntity<ApiResponse<Void>> serviceUnavailable(@PathVariable String service) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(service + " is temporarily unavailable. Please retry shortly."));
    }
}
