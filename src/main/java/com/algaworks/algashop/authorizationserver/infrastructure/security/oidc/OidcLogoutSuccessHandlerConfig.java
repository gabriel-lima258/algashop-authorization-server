package com.algaworks.algashop.authorizationserver.infrastructure.security.oidc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.oidc.web.authentication.OidcLogoutAuthenticationSuccessHandler;

// classe extende a classe de logout para customizar e revogar o refresh token também
// injetamos o bean que criamos de revoke e comportamental de logout do spring
// dentro do metodo de autenticação

@Configuration
public class OidcLogoutSuccessHandlerConfig {

    @Bean
    public OidcLogoutAuthenticationSuccessHandler oidcLogoutAuthenticationSuccessHandler(
            OidcRevokeAuthorizationsLogoutHandler logoutHandler
    ) {
        var logoutSuccessHandler = new OidcLogoutAuthenticationSuccessHandler();
        logoutSuccessHandler.setLogoutHandler(logoutHandler);
        return logoutSuccessHandler;
    }

}
