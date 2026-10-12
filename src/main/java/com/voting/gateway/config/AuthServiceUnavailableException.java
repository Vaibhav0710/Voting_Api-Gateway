package com.voting.gateway.config;

/**
 * The User Service could not be reached or failed, so the token could not be checked.
 */
public class AuthServiceUnavailableException extends RuntimeException {

    public AuthServiceUnavailableException(Throwable cause) {
        super("Authentication service unavailable", cause);
    }
}
