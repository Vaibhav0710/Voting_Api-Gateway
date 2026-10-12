package com.voting.gateway.config;

/**
 * The User Service rejected the bearer token (bad signature, expired, unknown user).
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("Invalid or expired token");
    }
}
