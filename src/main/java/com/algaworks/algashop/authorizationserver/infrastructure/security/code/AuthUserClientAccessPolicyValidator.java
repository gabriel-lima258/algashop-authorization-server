package com.algaworks.algashop.authorizationserver.infrastructure.security.code;

import com.algaworks.algashop.authorizationserver.domain.user.AuthUser;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserRepository;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserType;
import com.algaworks.algashop.authorizationserver.infrastructure.security.query.ClientAllowedQueryService;
import com.algaworks.algashop.authorizationserver.infrastructure.security.token.ScopePolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Onde o papel do usuario decide se o codigo de autorizacao chega a existir.
 *
 * ONDE ISTO RODA, E POR QUE IMPORTA
 * Este Consumer e chamado pelo OAuth2AuthorizationCodeRequestAuthenticationProvider durante
 * o GET /oauth2/authorize - depois de a pessoa ter feito login, e ANTES de qualquer codigo
 * ser emitido. E o ponto mais cedo em que se pode dizer "voce nao".
 *
 * Negar aqui e melhor que negar na API por tres motivos:
 *   - o usuario descobre imediatamente, na tela, e nao depois de logado e navegando;
 *   - nenhum token com poder indevido chega a ser assinado - e token assinado nao se revoga;
 *   - a regra fica num lugar so, em vez de replicada em cada resource server.
 *
 * AS DUAS PERGUNTAS
 *   1. Este PAPEL pode usar este CLIENT?      (tabela auth_user_type_client_allowed)
 *      Nao -> OAuth2ErrorCodes.ACCESS_DENIED
 *      E o que impede um CUSTOMER de abrir o admin-web mesmo tendo senha valida.
 *
 *   2. Este PAPEL pode levar estes ESCOPOS neste client?  (auth_user_type_client_scope)
 *      Nao -> OAuth2ErrorCodes.INVALID_SCOPE, nomeando os escopos recusados
 *      E o que faz OPERATOR e MANAGER, no MESMO client, receberem tokens diferentes.
 *
 * O SHORT-CIRCUIT DO PRINCIPAL
 * Se o principal e nulo, nao autenticado ou anonimo, o metodo simplesmente RETORNA. Nao e
 * permissividade: nesse ponto o Spring ainda vai redirecionar para /login, e checar papel de
 * quem nao se identificou nao faria sentido. Quem passa por aqui autenticado ja e alguem.
 *
 * POR QUE RECONSTRUIR O TOKEN ANTES DE LANCAR
 * O OAuth2AuthorizationCodeRequestAuthenticationException exige o token da requisicao para
 * saber PARA ONDE mandar o erro - sem redirect_uri e state, o Spring nao consegue devolver o
 * erro ao client e responde uma pagina de erro generica em vez do redirect que a spec manda.
 * O buildCodeRequest existe so para isso, e o setAuthenticated(true) e o que sinaliza que a
 * falha e de AUTORIZACAO (a pessoa e conhecida) e nao de autenticacao.
 */
@Component
@RequiredArgsConstructor
public class AuthUserClientAccessPolicyValidator implements Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> {

    private final AuthUserRepository authUserRepository;
    private final ClientAllowedQueryService clientAllowedQueryService;
    private final ScopePolicyService scopePolicyService;

    @Override
    public void accept(OAuth2AuthorizationCodeRequestAuthenticationContext context) {
        OAuth2AuthorizationCodeRequestAuthenticationToken authentication = context.getAuthentication();

        Authentication principal = (Authentication) authentication.getPrincipal();

        if (principal == null ||
            !principal.isAuthenticated() ||
            principal instanceof AnonymousAuthenticationToken) {
            return;
        }

        String email = principal.getName();
        String clientId = authentication.getClientId();

        AuthUser authUser = authUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));

        // valida se o user não tem acesso aquele client por causa da role e usa o padrão de erro spring
        if (!canUseClient(authUser.getType(), clientId)) {
            OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED,
                    "The authenticated user type is not allowed to authorize this client.",
                    null);
            var codeRequest = buildCodeRequest(authentication, principal);
            throw new OAuth2AuthorizationCodeRequestAuthenticationException(error, codeRequest);
        }

        Set<String> unauthorizedScopes = getUnauthorizedScopes(authentication, authUser.getType());

        // valida o scopes nao permitidos
        if (!unauthorizedScopes.isEmpty()) {
            OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE,
                    "The authenticated user type is not allowed to use these scopes " + unauthorizedScopes,
                    null);
            var codeRequest = buildCodeRequest(authentication, principal);
            throw new OAuth2AuthorizationCodeRequestAuthenticationException(error, codeRequest);
        }
    }

    // se um usuario tem a role mas não possui o scope adequada a aquele client, ele é bloqueado e não logado
    // isso vem da tabela auth_user_type_client_scope em conjunto da auth_user_type_client_allowed
    private Set<String> getUnauthorizedScopes(
            OAuth2AuthorizationCodeRequestAuthenticationToken authentication,
            AuthUserType type) {
        Set<String> originalScopes = authentication.getScopes();
        Set<String> filteredScopes = scopePolicyService.resolveScopes(type, authentication.getClientId(), originalScopes);

        // removo todos os scopes não autorizadas
        HashSet<String> result = new HashSet<>(originalScopes);
        result.removeAll(filteredScopes);

        return result;
    }

    private boolean canUseClient(AuthUserType type, String clientId) {
        Set<String> allowedClients = clientAllowedQueryService.findByRole(type);
        return allowedClients.contains(clientId);
    }


    // criando o build padrão do spring antes do expection error
    private OAuth2AuthorizationCodeRequestAuthenticationToken buildCodeRequest(
            OAuth2AuthorizationCodeRequestAuthenticationToken authentication,
            Authentication principal) {
        var resultToken = new OAuth2AuthorizationCodeRequestAuthenticationToken(
                authentication.getAuthorizationUri(),
                authentication.getClientId(),
                principal,
                authentication.getRedirectUri(),
                authentication.getState(),
                authentication.getScopes(),
                authentication.getAdditionalParameters()
        );

        resultToken.setAuthenticated(true);

        return resultToken;
    }
}
