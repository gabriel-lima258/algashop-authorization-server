package com.algaworks.algashop.authorizationserver.domain.user;

import java.time.Duration;

public class AuthUserTestDataBuilder {

    private AuthUserTestDataBuilder() {}

    /** A senha em texto puro dos usuarios construidos aqui - o que um teste de login digita. */
    public static final String PLAIN_PASSWORD = "test123";

    /**
     * Fase 30: brandNew deixou de receber um hash pronto e passou a receber a PORTA
     * AuthUserPasswordManager - o agregado e quem gera e cifra a senha inicial.
     */
    private static final AuthUserPasswordManager PASSWORD_MANAGER = new AuthUserPasswordManager() {
        @Override
        public String generate() {
            return PLAIN_PASSWORD;
        }

        @Override
        public String encrypt(String plainPassword) {
            return "{noop}" + plainPassword;
        }

        @Override
        public boolean matches(String plainPassword, String encryptPassword) {
            return encrypt(plainPassword).equals(encryptPassword);
        }
    };

    /** Hasher trivial: os testes nao verificam criptografia, so o fluxo. */
    private static final VerificationTokenHasher TOKEN_HASHER = new VerificationTokenHasher() {
        @Override
        public String generate() {
            return "test-token";
        }

        @Override
        public String hash(String plainToken) {
            return "hashed:" + plainToken;
        }

        @Override
        public boolean isEqual(String hashed, String plainToken) {
            return hash(plainToken).equals(hashed);
        }
    };

    /**
     * O usuario nasce NAO verificado (brandNew) e e ativado pelo caminho de verdade:
     * gera token, troca a senha com ele. Nao ha atalho - setEmailVerified e privado de
     * proposito, e desde a Fase 30 quem nao verificou o e-mail nao consegue logar
     * (AuthUser.isDisabled). Forjar o estado aqui produziria um usuario que o dominio
     * nunca criaria.
     */
    public static AuthUser aUser(String email, String name, AuthUserType type) {
        AuthUser user = AuthUser.brandNew(email, name, type, PASSWORD_MANAGER);
        String plainToken = user.generateVerificationToken(Duration.ofHours(1), TOKEN_HASHER);
        user.changePasswordWithToken(plainToken, PLAIN_PASSWORD, PASSWORD_MANAGER, TOKEN_HASHER);
        return user;
    }

    /** Como sai do cadastro: sem e-mail verificado, e portanto sem conseguir logar. */
    public static AuthUser aPendingUser(String email, String name, AuthUserType type) {
        return AuthUser.brandNew(email, name, type, PASSWORD_MANAGER);
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
