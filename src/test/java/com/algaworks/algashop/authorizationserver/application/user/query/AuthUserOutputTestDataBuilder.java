package com.algaworks.algashop.authorizationserver.application.user.query;

import com.algaworks.algashop.authorizationserver.domain.user.AuthUserType;

import java.util.UUID;

public class AuthUserOutputTestDataBuilder {

    private AuthUserOutputTestDataBuilder() {}

    public static AuthUserOutput.AuthUserOutputBuilder aManagerUser() {
        return AuthUserOutput.builder()
                .id(UUID.randomUUID())
                .name("John Manager")
                .email("john.manager@algashop.com")
                .type(AuthUserType.MANAGER)
                .enabled(true);
    }

    public static AuthUserOutput.AuthUserOutputBuilder anOperatorUser() {
        return AuthUserOutput.builder()
                .id(UUID.randomUUID())
                .name("Alice Operator")
                .email("alice.operator@algashop.com")
                .type(AuthUserType.OPERATOR)
                .enabled(true);
    }
}
