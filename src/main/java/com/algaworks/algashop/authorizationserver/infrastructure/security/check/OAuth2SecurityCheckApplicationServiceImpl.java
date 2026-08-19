package com.algaworks.algashop.authorizationserver.infrastructure.security.check;

import com.algaworks.algashop.authorizationserver.application.security.SecurityCheckApplicationService;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementação de SecurityCheckApplicationService baseada em OAuth2/JWT (Spring Security).
 *
 * Funcionamento:
 * - Lê a Authentication do SecurityContextHolder (contexto de segurança da requisição atual)
 *   e espera que o principal seja um JWT (token de acesso já validado pelo resource server).
 * - getAuthenticatedUserId(): extrai o claim "sub" (subject) do JWT e o converte para UUID.
 *   Se a autenticação for de máquina, lança AccessDeniedException, pois clients não têm user ID.
 * - isAuthenticated(): delega para Authentication.isAuthenticated().
 * - isMachineAuthenticated(): detecta o fluxo client_credentials comparando "aud" e "sub" —
 *   em tokens de máquina o subject é o próprio client_id, que também aparece na audience;
 *   em tokens de usuário o subject é o UUID do usuário e não está na audience.
 *
 * Fica na camada de infraestrutura porque depende do Spring Security; a camada de
 * aplicação enxerga apenas a interface.
 */
@Service("securityCheck")
@Slf4j
public class OAuth2SecurityCheckApplicationServiceImpl implements SecurityCheckApplicationService {

    private static final String SCOPE_USERS_WRITE = "SCOPE_users:write";
    private static final String ROLE_MANAGER = "ROLE_" + AuthUserType.MANAGER.name();

    @Override
    public UUID getAuthenticatedUserId() {
        if (isMachineAuthenticated()) {
            throw new AccessDeniedException("Machina users does not have user ID");
        }
        Jwt jwt = getJwt();

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID in JWT subject: {}", jwt.getSubject(), e);
            throw new AuthorizationDeniedException("Invalid user ID in JWT subject");
        }
    }

    @Override
    public boolean isAuthenticated() {
        try {
            return getAuthentication().isAuthenticated();
        } catch (IllegalStateException e) {
            log.debug(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isMachineAuthenticated() {
        Jwt jwt;

        try {
            jwt = getJwt();
        } catch (IllegalStateException e) {
            log.debug(e.getMessage(), e);
            return false;
        }

        // aud e OPCIONAL no JWT: getAudience() devolve null quando o claim nao vem.
        // Sem o guarda, o AuditorAware (que chama este metodo antes de CADA persistencia)
        // derruba toda escrita com NPE.
        List<String> audience = jwt.getAudience();
        return audience != null && audience.contains(jwt.getSubject());
    }

    @Override
    public boolean canAccessOwnProfile() {
        return this.isAuthenticated() && !isMachineAuthenticated();
    }

    /**
     * Quem pode cadastrar quem.
     *
     * Duas condicoes valem para todos: estar autenticado e ter SCOPE_users:write. Depois
     * disso, o tipo do usuario a ser criado decide:
     *
     *   CUSTOMER  -> so MAQUINA. Cliente se cadastra pela loja (client_credentials do
     *                algashop-ecommerce-m2m), nao por alguem do back-office criando conta
     *                em nome dele. E a regra que da sentido ao isMachineAuthenticated()
     *                aparecendo como PERMISSAO, e nao como restricao.
     *   MANAGER
     *   OPERATOR  -> so MANAGER. Operador nao cria colega, nem promove ninguem.
     *
     * Repare no que NAO existe: um caminho em que OPERATOR cria alguem. Ele tem acesso ao
     * admin-web e a users:read, mas users:write nao esta na lista dele em
     * auth_user_type_client_scope - entao o token nem chega aqui com o escopo.
     * Duas barreiras para a mesma regra, em camadas diferentes, de proposito.
     */
    @Override
    public boolean canRegisterUserOfType(AuthUserType registrationType) {
        if (!isAuthenticated()) {
            return false;
        }

        if (!hasAuthority(SCOPE_USERS_WRITE)) {
            return false;
        }

        // registro de customer é feita via client
        if (registrationType == AuthUserType.CUSTOMER) {
            return isMachineAuthenticated();
        }

        // se for admin pode registrar um admin ou um operator
        if (hasAuthority(ROLE_MANAGER)) {
            return registrationType == AuthUserType.MANAGER ||
                   registrationType == AuthUserType.OPERATOR;
        }

        return false;
    }

    /**
     * Quem pode editar qual registro.
     *
     * A ordem das checagens e o desenho da regra:
     *
     *   1. maquina nao edita ninguem - nao ha "proprio registro" para uma maquina;
     *   2. o PROPRIO registro sempre pode ser editado, qualquer que seja o papel. E a mesma
     *      ideia do /me: o id nao vem da requisicao, vem do token, entao nao ha como pedir
     *      para editar o de outro por engano;
     *   3. sobrando o caso "registro de outra pessoa", so MANAGER passa - e ainda assim
     *      apenas sobre MANAGER e OPERATOR.
     *
     * O catch(AccessDeniedException) cobre o caso de getAuthenticatedUserId() recusar em vez
     * de responder. Ele lanca quando o token e de maquina - situacao que o passo 1 ja
     * barrou - mas a guarda fica porque a alternativa e um 500 no lugar de um 403.
     */
    @Override
    public boolean canEditUser(AuthUserType editType, UUID editUserId) {
        if (isMachineAuthenticated()) {
            return false;
        }

        // se usuario autenticado for o user edit pode editar
        try {
            if (getAuthenticatedUserId().equals(editUserId)) {
                return true;
            }
        } catch (AccessDeniedException e) {
            return false;
        }

        if (hasAuthority(ROLE_MANAGER)) {
            return editType == AuthUserType.MANAGER || editType == AuthUserType.OPERATOR;
        }

        return false;
    }

    /**
     * Quais transicoes de papel sao permitidas.
     *
     * Complementa canEditUser: aquele responde "posso mexer neste registro?", este responde
     * "posso mexer NESTE CAMPO?". Sao perguntas diferentes - um OPERATOR pode editar o
     * proprio nome (canEditUser: sim) e nao pode promover-se a MANAGER (aqui: nao).
     *
     * Nao mudar o tipo passa sempre (currentType == newType) - e o caso do PUT que so altera
     * nome ou enabled, e sem esta linha toda edicao exigiria ser MANAGER.
     *
     * As duas transicoes possiveis sao MANAGER <-> OPERATOR, e so por um MANAGER. CUSTOMER
     * nao aparece em transicao nenhuma, o que casa com a invariante que o proprio agregado ja
     * protege ("Cannot change type of a CUSTOMER user"): a regra existe em dois lugares
     * porque responde a duas perguntas - aqui, quem tem PERMISSAO; no dominio, o que e
     * possivel independentemente de quem peca.
     */
    @Override
    public boolean canChangeUserType(AuthUserType currentType, AuthUserType newType) {
        if (currentType == newType) {
            return true;
        }

        if (hasAuthority(ROLE_MANAGER)) {
            if (currentType == AuthUserType.MANAGER && newType == AuthUserType.OPERATOR) {
                return true;
            }

            if (currentType == AuthUserType.OPERATOR && newType == AuthUserType.MANAGER) {
                return true;
            }
        }

        return false;
    }

    private Jwt getJwt() {
        Authentication authentication = getAuthentication();
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        throw new IllegalStateException("Authentication principal is not a JWT");
    }

    // retorna o contexto de autenticação de usuario
    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No authentication found");
        }
        return authentication;
    }

    /**
     * Compara authority por string exata - e por isso os prefixos "SCOPE_" e "ROLE_" vem
     * escritos nas constantes do topo da classe. O JwtGrantedAuthoritiesDelegatingConverter
     * e quem os produz; errar o prefixo aqui nega tudo em silencio.
     */
    private boolean hasAuthority(String authority) {
        Authentication authentication;
        try {
            authentication = getAuthentication();
        } catch (IllegalStateException e) {
            log.debug(e.getMessage(), e);
            return false;
        }

        // verifica se authority existe
        return authentication.getAuthorities()
                .stream().anyMatch(a -> Objects.equals(a.getAuthority(), authority));
    }
}
