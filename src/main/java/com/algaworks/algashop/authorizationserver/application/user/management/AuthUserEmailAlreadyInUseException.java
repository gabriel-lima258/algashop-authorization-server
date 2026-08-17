package com.algaworks.algashop.authorizationserver.application.user.management;

public class AuthUserEmailAlreadyInUseException extends RuntimeException {
    public AuthUserEmailAlreadyInUseException(String email) {
        super("Email already in use " + email);
    }
}
