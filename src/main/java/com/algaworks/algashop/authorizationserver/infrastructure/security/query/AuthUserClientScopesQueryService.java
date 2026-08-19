package com.algaworks.algashop.authorizationserver.infrastructure.security.query;

import com.algaworks.algashop.authorizationserver.domain.user.AuthUserType;

import java.util.Set;

/**
 * Porta de consulta: quais escopos um PAPEL pode levar num CLIENT.
 *
 * Fica em infrastructure/security/query porque e uma pergunta de politica de acesso, nao de
 * dominio - nao existe agregado "escopo permitido", existe uma tabela de decisao. Declarar a
 * interface aqui e a implementacao em infrastructure/persistence mantem quem PERGUNTA
 * (ScopePolicyService) ignorante de que a resposta vem de SQL.
 */
public interface AuthUserClientScopesQueryService {
    Set<String> findAllAllowedScopesByRoleAndClientId(AuthUserType type, String clientId);
}
