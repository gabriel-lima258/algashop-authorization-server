package com.algaworks.algashop.authorizationserver.infrastructure.security.code;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator.*;

/**
 * Encadeia a politica do AlgaShop DEPOIS das validacoes que o Spring ja fazia.
 *
 * O setAuthenticationValidator() do provider SUBSTITUI o validador padrao - ele nao
 * acrescenta. Plugar o AuthUserClientAccessPolicyValidator diretamente ali desligaria, em
 * silencio, duas checagens fundamentais do protocolo:
 *
 *   DEFAULT_REDIRECT_URI_VALIDATOR - confere que a redirect_uri pedida esta registrada no
 *                                    client. Sem ela, qualquer um poderia mandar o codigo
 *                                    de autorizacao para o proprio servidor.
 *   DEFAULT_SCOPE_VALIDATOR        - confere que os escopos pedidos existem no client.
 *
 * Dai o andThen: primeiro o protocolo, depois o negocio. A ordem tambem importa - checar
 * papel contra uma redirect_uri que nem e valida seria trabalho jogado fora, e pior, poderia
 * emitir a mensagem de erro errada.
 *
 * O acoplamento com a config e por bean: a AuthorizationServerSecurityConfig percorre os
 * AuthenticationProvider do authorization endpoint e injeta este validador em cada
 * OAuth2AuthorizationCodeRequestAuthenticationProvider que encontrar.
 */
@Component
@RequiredArgsConstructor
public class DelegatingAuthorizationCodeRequestValidator
        implements Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> {

    private final AuthUserClientAccessPolicyValidator clientAccessPolicyValidator;

    @Override
    public void accept(OAuth2AuthorizationCodeRequestAuthenticationContext context) {
        DEFAULT_REDIRECT_URI_VALIDATOR
                .andThen(DEFAULT_SCOPE_VALIDATOR)
                .andThen(clientAccessPolicyValidator)
                .accept(context);
    }
}
