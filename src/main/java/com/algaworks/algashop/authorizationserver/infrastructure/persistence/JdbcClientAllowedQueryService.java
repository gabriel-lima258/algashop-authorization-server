package com.algaworks.algashop.authorizationserver.infrastructure.persistence;

import com.algaworks.algashop.authorizationserver.domain.user.AuthUserType;
import com.algaworks.algashop.authorizationserver.infrastructure.security.query.ClientAllowedQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Le, por SQL, quais clients um papel pode usar (tabela auth_user_type_client_allowed).
 *
 * Mesmas razoes do JdbcAuthUserClientScopesQueryService: tabela de decisao, sem entidade.
 *
 * Repare no que a ausencia de linha significa: um papel sem nenhuma linha aqui nao consegue
 * autorizar client NENHUM. O default e negar, e e proposital - acrescentar um client novo ao
 * YAML nao o abre para ninguem ate que alguem decida, explicitamente, quem pode usa-lo.
 */
@Component
@RequiredArgsConstructor
public class JdbcClientAllowedQueryService implements ClientAllowedQueryService {

    private final JdbcOperations jdbcOperations;

    private static final String SQL = """
            SELECT client_id
            FROM auth_user_type_client_allowed
            WHERE auth_user_type = ?
            """;

    @Override
    public Set<String> findByRole(AuthUserType role) {
        return new HashSet<>(jdbcOperations.queryForList(SQL, String.class, role.name()));
    }
}
