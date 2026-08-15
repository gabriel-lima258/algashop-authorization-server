package com.algaworks.algashop.authorizationserver.infrastructure.security.oidc;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Define o que o endpoint /userinfo responde quando um client consulta os dados do usuário.
 *
 * O Spring Authorization Server chama esta função a cada requisição ao /userinfo,
 * e ela decide de onde tirar as claims:
 *
 * - Se a autorização tem um ID Token (fluxo com openid, ex.: authorization_code),
 *   reaproveita as claims que já foram colocadas nele pelo OAuth2TokenCustomizerConfig
 *   (name, email, type, createdAt...).
 *
 * - Se não tem ID Token (ex.: acesso só com access token), devolve apenas o "sub"
 *   extraído do próprio JWT — o mínimo que o padrão OIDC exige na resposta.
 *
 * É registrada na AuthorizationServerSecurityConfig via oidc.userInfoEndpoint().userInfoMapper().
 */
@Component
public class OidcUserInfoMapper
        implements Function<OidcUserInfoAuthenticationContext, OidcUserInfo> {

    @Override
    public OidcUserInfo apply(OidcUserInfoAuthenticationContext context) {
        OAuth2Authorization authorization = context.getAuthorization();
        var idTokenHoldeer = authorization.getToken(OidcIdToken.class);
        if (idTokenHoldeer == null) {
            Authentication authentication = context.getAuthentication();
            JwtAuthenticationToken principal = (JwtAuthenticationToken) authentication.getPrincipal();

            return OidcUserInfo.builder()
                    .claim("sub", principal.getToken().getClaims().get("sub"))
                    .build();
        }
        return new OidcUserInfo(idTokenHoldeer.getClaims());
    }
}
