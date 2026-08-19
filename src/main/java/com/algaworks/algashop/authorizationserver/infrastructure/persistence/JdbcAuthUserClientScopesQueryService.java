package com.algaworks.algashop.authorizationserver.infrastructure.persistence;

import com.algaworks.algashop.authorizationserver.domain.user.AuthUserType;
import com.algaworks.algashop.authorizationserver.infrastructure.security.query.AuthUserClientScopesQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Le a politica de escopo direto da tabela, por SQL.
 *
 * POR QUE JdbcOperations E NAO UM REPOSITORIO JPA
 * auth_user_type_client_scope nao tem entidade, e nao deveria ter: nao e um agregado, e uma
 * tabela de decisao - tres colunas que juntas SAO a chave primaria, sem identidade propria,
 * sem ciclo de vida e sem invariante para proteger. Mapear isso como @Entity criaria um
 * objeto de dominio que o dominio nao reconhece, e traria contexto de persistencia, cache de
 * primeiro nivel e lazy loading para uma consulta que devolve strings.
 *
 * E o mesmo criterio que separa query service de repositorio no resto do projeto: quem so le
 * projecao nao precisa do caminho do agregado.
 *
 * A consulta e chamada uma vez por requisicao de autorizacao e usa o indice composto
 * (auth_user_type, client_id) criado na V5 - por isso a ordem das colunas no indice espelha
 * a ordem do WHERE.
 */
@Component
@RequiredArgsConstructor
public class JdbcAuthUserClientScopesQueryService implements AuthUserClientScopesQueryService {

    // usamos jdbc operations quando não envolve nenhuma entidade na busca
    private final JdbcOperations jdbcOperations;

    private static final String SQL = """
            SELECT scope
            FROM auth_user_type_client_scope
            WHERE auth_user_type = ?
            AND client_id = ?
            """;

    @Override
    public Set<String> findAllAllowedScopesByRoleAndClientId(AuthUserType role, String clientId) {
        return new HashSet<>(jdbcOperations.queryForList(SQL, String.class, role.name(), clientId));
    }
}
