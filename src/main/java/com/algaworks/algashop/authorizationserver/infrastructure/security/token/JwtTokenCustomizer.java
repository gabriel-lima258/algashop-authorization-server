package com.algaworks.algashop.authorizationserver.infrastructure.security.token;

import com.algaworks.algashop.authorizationserver.domain.user.AuthUserType;
import com.algaworks.algashop.authorizationserver.infrastructure.security.oidc.OidcUserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * O ponto onde o AlgaShop escreve as claims que o protocolo nao escreveria sozinho.
 *
 * O Spring Authorization Server chama este customizer UMA VEZ POR TOKEN, com o token ja
 * montado e ainda nao assinado. Alterar aqui e a ultima chance: depois disso a assinatura
 * fecha o conteudo.
 *
 * DUAS ESCRITAS, PARA DOIS PUBLICOS DIFERENTES
 *
 *   ID token   -> recebe TODAS as claims de identidade (name, email, type, createdAt).
 *                 Ele vai para o CLIENT que pediu o login, para montar "Ola, Victoria".
 *
 *   Access token -> recebe apenas DUAS coisas:
 *                   - o "sub" reescrito: por padrao viria o username (o e-mail), e passa a
 *                     ser o UUID do usuario. E-mail muda, id nao - e e esse id que o
 *                     AuditorAware grava como autor e que as APIs comparam para decidir se
 *                     o recurso e "seu" (ver OrderQueryService no ordering).
 *                   - o "role": o AuthUserType virando claim, que na Fase 27 deixou de ser
 *                     informativo e passou a valer como permissao.
 *
 * Mandar as claims de identidade tambem para o access token seria vazamento: ele circula em
 * toda requisicao a toda API, e nenhuma delas precisa saber o e-mail de quem chama.
 *
 * POR QUE SO EM FLUXO COM PESSOA
 * O "role" so entra quando o grant e authorization_code ou refresh_token. Em
 * client_credentials nao existe usuario - o "sub" e o proprio client_id, e nao ha papel a
 * atribuir. A consequencia pratica esta do outro lado: isCustomer() e falso para token de
 * maquina, e e por isso que canRegisterUserOfType(CUSTOMER) exige justamente uma maquina.
 *
 * ARMADILHA: o refresh_token PRECISA estar na lista. Sem ele, renovar o token devolveria um
 * access token sem "role" - o usuario perderia os poderes na primeira renovacao, cinco
 * minutos depois de logar, sem erro nenhum no caminho.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final OidcUserInfoService oidcUserInfoService;

    // 1 - identifica o tipo de token -> OpenID, AccessToken
    // 2 - extrai o username (email) do principal autenticado
    // 3.1 - carrega todas as claims do usuário via OidcUserInfoService e adiciona ao token de open id
    // 3.2 - carregamos somente o subject ao access token para sub receber id ao inves email
    // 4 - buscamos o type de user e transforma em role no token
    @Override
    public void customize(JwtEncodingContext context) {
        String tokenType = context.getTokenType().getValue();
        AuthorizationGrantType authorizationGrantType = context.getAuthorizationGrantType();

        if (isOpenIdToken(tokenType)) {
            customizeIdToken(context);
        } else if (isAccessToken(tokenType) && isUserDelegatingFlow(authorizationGrantType)) {
            customizeAccessToken(context);
        }
    }

    private void customizeAccessToken(JwtEncodingContext context) {
        OidcUserInfo oidcUserInfo = loadUserInfo(context);
        String role = oidcUserInfo.getClaimAsString("type");

        context.getClaims().subject(oidcUserInfo.getSubject());
        context.getClaims().claim("role", role);
    }

    private void customizeIdToken(JwtEncodingContext context) {
        OidcUserInfo oidcUserInfo = loadUserInfo(context);
        context.getClaims().claims(claims -> claims.putAll(oidcUserInfo.getClaims()));
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

    private boolean isUserDelegatingFlow(AuthorizationGrantType authorizationGrantType) {
        return isAuthCodeFlow(authorizationGrantType) || isRefreshTokenFlow(authorizationGrantType);
    }
}
