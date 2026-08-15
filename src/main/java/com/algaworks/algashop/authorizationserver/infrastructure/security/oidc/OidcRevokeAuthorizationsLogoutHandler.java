package com.algaworks.algashop.authorizationserver.infrastructure.security.oidc;

import com.algaworks.algashop.authorizationserver.infrastructure.security.query.OAuth2AuthorizationQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcLogoutAuthenticationToken;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import java.util.List;

// - OAuth2AuthorizationService -> remove e salva informacoes no banco de dados de authorization
// - OAuth2AuthorizationQueryService -> criamos um metodo de busca de principal ja que o
// OAuth2AuthorizationService não oferece por padrão
// - SecurityContextLogoutHandler -> mantemos o comportamento padrão de logout do spring instanciando ele

@Component
@RequiredArgsConstructor
public class OidcRevokeAuthorizationsLogoutHandler implements LogoutHandler {

    private final OAuth2AuthorizationQueryService oAuth2AuthorizationQueryService;
    private final OAuth2AuthorizationService oAuth2AuthorizationService;
    private final SecurityContextLogoutHandler securityContextLogoutHandler =
            new SecurityContextLogoutHandler();

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, @Nullable Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return;
        }

        performLogout(request, response, authentication);
        revokeAuthorizations(authentication);
    }

    // logica original do spring
    private void performLogout(HttpServletRequest request, HttpServletResponse response,
                              Authentication authentication) {
        OidcLogoutAuthenticationToken oidcLogoutAuthentication = (OidcLogoutAuthenticationToken) authentication;
        if (oidcLogoutAuthentication.isPrincipalAuthenticated()) {
            this.securityContextLogoutHandler.logout(request, response,
                    (Authentication) oidcLogoutAuthentication.getPrincipal());
        }
    }

    private void revokeAuthorizations(@Nullable Authentication authentication) {
        String email = authentication.getName();
        List<String> authorizationIds = oAuth2AuthorizationQueryService.findAuthorizationIds(email);
        for (String authorizationId : authorizationIds) {
            OAuth2Authorization authorization = oAuth2AuthorizationService.findById(authorizationId);
            if (authorization != null) {
                oAuth2AuthorizationService.remove(authorization);
            }
        }
    }
}
