package com.algaworks.algashop.authorizationserver.domain.user;

public class AuthUserTestDataBuilder {

    private AuthUserTestDataBuilder() {}

    // hash de mentira: os ITs de consulta nunca autenticam, so persistem e leem.
    // O prefixo {noop} mantem o formato do DelegatingPasswordEncoder usado no seed.
    private static final String PASSWORD_HASH = "{noop}test123";

    public static AuthUser aUser(String email, String name, AuthUserType type) {
        return AuthUser.brandNew(email, name, type, PASSWORD_HASH);
    }

    public static AuthUser aManagerUser() {
        return aUser("john.manager@algashop.com", "John Manager", AuthUserType.MANAGER);
    }

    public static AuthUser anOperatorUser() {
        return aUser("alice.operator@algashop.com", "Alice Operator", AuthUserType.OPERATOR);
    }

    public static AuthUser aCustomerUser() {
        return aUser("bob.customer@gmail.com", "Bob Customer", AuthUserType.CUSTOMER);
    }
}
