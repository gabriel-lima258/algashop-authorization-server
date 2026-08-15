package com.algaworks.algashop.authorizationserver.infrastructure.security.token;

import com.algaworks.algashop.authorizationserver.infrastructure.security.oidc.OidcUserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/**
 * Customiza os tokens JWT emitidos pelo Authorization Server, ajustando as claims
 * conforme o tipo de token:
 *
 * - ID Token (OpenID Connect): por padrão traz apenas claims básicas (sub, iss, aud, exp...).
 *   Aqui adicionamos as claims de identificação do usuário (name, email, type, createdAt...)
 *   carregadas pelo OidcUserInfoService, para que os clients recebam essas informações
 *   direto no token, sem precisar consultar o userinfo endpoint.
 *
 * - Access Token (nos fluxos authorization_code e refresh_token): sobrescreve a claim "sub",
 *   que por padrão viria com o username (email), para conter o id do usuário — identificador
 *   estável que os resource servers usam para reconhecer o dono do token.
 *
 *   - ID Token — é para o client (a aplicação que pediu o login, ex.: o frontend).
 *   Serve para identificar quem logou: o client lê as claims (name, email, type...) e usa para exibir
 *   "Olá, Gabriel", montar o perfil etc. Ele não é enviado para as APIs.
 *   - Access Token — é para os resource servers (suas APIs, ex.: o microserviço de ecommerce). A API valida
 *   para decidir se autoriza o acesso. É por isso que seu customizer ajusta o sub dele para o id do usuário.
 */
@Configuration
@RequiredArgsConstructor
public class OAuth2TokenCustomizerConfig {

    private final OidcUserInfoService oidcUserInfoService;

    // 1 - identifica o tipo de token -> OpenID, AccessToken
    // 2 - extrai o username (email) do principal autenticado
    // 3.1 - carrega todas as claims do usuário via OidcUserInfoService e adiciona ao token de open id
    // 3.2 - carregamos somente o subject ao access token para sub receber id ao inves email
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            String tokenType = context.getTokenType().getValue();
            AuthorizationGrantType authorizationGrantType = context.getAuthorizationGrantType();

            if (isOpenIdToken(tokenType)) {
                OidcUserInfo oidcUserInfo = loadUserInfo(context);
                context.getClaims().claims(claims -> claims.putAll(oidcUserInfo.getClaims()));
            } else if (isAccessToken(tokenType) &&
                    (isAuthCodeFlow(authorizationGrantType) || isRefreshTokenFlow(authorizationGrantType))) {
                OidcUserInfo oidcUserInfo = loadUserInfo(context);
                context.getClaims().subject(oidcUserInfo.getSubject());
            }
        };
    }

    private boolean isRefreshTokenFlow(AuthorizationGrantType authorizationGrantType) {
        return AuthorizationGrantType.REFRESH_TOKEN.equals(authorizationGrantType);
    }


    private boolean isAuthCodeFlow(AuthorizationGrantType authorizationGrantType) {
        return AuthorizationGrantType.AUTHORIZATION_CODE.equals(authorizationGrantType);
    }

    private OidcUserInfo loadUserInfo(JwtEncodingContext context) {
        String email = context.getPrincipal().getName();
        return oidcUserInfoService.loadUser(email);
    }

    private boolean isAccessToken(String tokenType) {
        return OAuth2TokenType.ACCESS_TOKEN.getValue().equals(tokenType);
    }

    private boolean isOpenIdToken(String tokenType) {
        return OidcParameterNames.ID_TOKEN.equals(tokenType);
    }
}
