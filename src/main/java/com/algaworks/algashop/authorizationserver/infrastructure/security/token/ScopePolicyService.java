package com.algaworks.algashop.authorizationserver.infrastructure.security.token;

import com.algaworks.algashop.authorizationserver.domain.user.AuthUserType;
import com.algaworks.algashop.authorizationserver.infrastructure.security.query.AuthUserClientScopesQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A politica de escopo por papel: dado QUEM e a pessoa e QUAL client ela esta usando, quais
 * dos escopos pedidos ela pode de fato levar.
 *
 * O escopo declarado no client e o TETO do que aquele client pode pedir - e ele e igual para
 * todo mundo que usa o client. O admin-web declara users:write; isso nao significa que todo
 * usuario do admin-web possa escrever usuario. Este servico e o corte fino: ele intersecta o
 * que foi PEDIDO com o que aquele PAPEL pode naquele CLIENT (tabela
 * auth_user_type_client_scope).
 *
 *     pedido:    [openid, users:read, users:write]
 *     permitido: [openid, users:read]                 (OPERATOR no admin-web)
 *     -------------------------------------------------
 *     resultado: [openid, users:read]
 *
 * Quem usa o resultado e o AuthUserClientAccessPolicyValidator - e repare no que ele faz com
 * a diferenca: em vez de silenciosamente reduzir o token, ele RECUSA o pedido inteiro com
 * invalid_scope. As duas leituras sao defensaveis; a escolhida evita que o cliente receba um
 * token com menos poder do que pediu e so descubra na primeira chamada que der 403.
 */
@Service
@RequiredArgsConstructor
public class ScopePolicyService {

    private final AuthUserClientScopesQueryService scopesQueryService;

    // 1 - nada pedido, nada a permitir (evita ida ao banco no caso trivial)
    // 2 - busca o que este papel pode neste client
    // 3 - devolve a intersecao
    public Set<String> resolveScopes(AuthUserType role, String clientId, Set<String> authorizedScopes) {
        if (authorizedScopes.isEmpty()) {
            return new HashSet<>();
        }

        // busca allowed scopes no banco
        Set<String> allowedScopes = scopesQueryService.findAllAllowedScopesByRoleAndClientId(role, clientId);

        return authorizedScopes.stream()
                .filter(allowedScopes::contains)
                .collect(Collectors.toSet());
    }
}
