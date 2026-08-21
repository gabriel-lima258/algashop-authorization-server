package com.algaworks.algashop.authorizationserver.application.user.query;

import java.util.UUID;

public class AuthUserNotFoundException extends RuntimeException {
    public AuthUserNotFoundException(UUID userId) {
        super(String.format("User id %s not found ", userId));
    }

    public AuthUserNotFoundException(String message) {
        super(message);
    }
}
