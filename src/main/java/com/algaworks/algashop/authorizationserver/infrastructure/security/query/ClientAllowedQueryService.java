package com.algaworks.algashop.authorizationserver.infrastructure.security.query;

import com.algaworks.algashop.authorizationserver.domain.user.AuthUserType;

import java.util.Set;

/**
 * Porta de consulta: quais CLIENTS um PAPEL pode usar.
 *
 * A pergunta que ela responde e anterior a qualquer escopo - "esta pessoa pode sequer abrir
 * esta aplicacao?". Um CUSTOMER com senha valida é recusado no admin-web por aqui, antes de
 * a lista de escopos ser sequer consultada.
 */
public interface ClientAllowedQueryService {
    Set<String> findByRole(AuthUserType role);
}
