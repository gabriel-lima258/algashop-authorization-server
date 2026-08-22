package com.algaworks.algashop.authorizationserver.infrastructure.security;

import com.algaworks.algashop.authorizationserver.application.user.management.AuthUserManagementApplicationService;
import com.algaworks.algashop.authorizationserver.application.user.management.PasswordManagementApplicationService;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserQueryService;
import com.algaworks.algashop.authorizationserver.infrastructure.security.check.OAuth2SecurityCheckApplicationServiceImpl;
import com.algaworks.algashop.authorizationserver.infrastructure.security.code.DelegatingAuthorizationCodeRequestValidator;
import com.algaworks.algashop.authorizationserver.infrastructure.security.oidc.OidcUserInfoMapper;
import com.algaworks.algashop.authorizationserver.presentation.MyUserController;
import com.algaworks.algashop.authorizationserver.presentation.UserController;
import com.algaworks.algashop.authorizationserver.util.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.oidc.web.authentication.OidcLogoutAuthenticationSuccessHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Matriz de autorizacao do authorization server, no mesmo espirito da do ordering:
 * para cada rota, quem NAO pode (401/403) e quem pode (passa pela seguranca).
 *
 * Dois grupos com regras diferentes:
 *
 *  - /api/v1/users (administrativo): escopo users:read|write E NAO ter papel CUSTOMER.
 *    Cliente da loja fica fora mesmo que o token carregue o escopo.
 *  - /api/v1/users/me: nenhum escopo - so usuario autenticado que nao seja maquina
 *    (canAccessOwnProfile). O DELETE exige ainda o papel CUSTOMER (CanDeleteOwnProfile).
 *
 * A fatia importa a AuthorizationServerSecurityConfig REAL (as 4 chains) e a implementacao
 * REAL do bean "securityCheck", porque e ela que as expressoes SpEL consultam. Maquina e
 * detectada por aud contendo o sub - os tokens sinteticos abaixo reproduzem isso.
 *
 * O caso positivo afirma "nao e 401 nem 403" (nao "e 200"): teste de seguranca falha
 * quando a seguranca muda, nao quando o controller devolve 4xx/5xx de negocio.
 */
@WebMvcTest(controllers = {UserController.class, MyUserController.class})
@Import({AuthorizationServerSecurityConfig.class,
        OAuth2SecurityCheckApplicationServiceImpl.class,
        TestSecurityConfig.class,
        AuthorizationMatrixTest.MatrixTestConfig.class})
class AuthorizationMatrixTest {

    private static final String USER_SUBJECT = "019d7764-3e11-7000-8000-00000000000a";
    private static final String MACHINE_SUBJECT = "algashop-ecommerce-m2m";
    private static final String AUDIENCE = "algashop-web";

    private static final String USER_ID = "019d7764-3e11-7000-8000-000000000001";
    private static final String JSON = "application/json";

    private static final String CREATE_BODY = """
            {"name":"John Doe","email":"john.doe@algashop.com","type":"MANAGER"}""";
    private static final String UPDATE_BODY = """
            {"name":"John Doe","type":"MANAGER","enabled":true}""";
    private static final String MY_UPDATE_BODY = """
            {"name":"John Doe"}""";

    @Autowired
    private MockMvc mockMvc;

    // exigido pelo oauth2ResourceServer().jwt(); nunca chamado - o post-processor jwt()
    // injeta a autenticacao pronta (este teste cobre AUTORIZACAO, nao validacao de token)
    @MockitoBean
    private JwtDecoder jwtDecoder;

    // dependencias da chain OAuth2/OIDC, irrelevantes para /api/** mas necessarias
    // para a config real subir
    @MockitoBean
    private OidcUserInfoMapper oidcUserInfoMapper;
    @MockitoBean
    private OidcLogoutAuthenticationSuccessHandler oidcLogoutAuthenticationSuccessHandler;
    @MockitoBean
    private DelegatingAuthorizationCodeRequestValidator delegatingRequestValidator;

    @MockitoBean
    private AuthUserQueryService authUserQueryService;
    @MockitoBean
    private AuthUserManagementApplicationService authUserManagementApplicationService;
    @MockitoBean
    private PasswordManagementApplicationService passwordManagementApplicationService;

    // as properties sao @ConfigurationProperties e nao entram na fatia; instancia real
    // minima para as chains construirem CSP e redirect
    @TestConfiguration
    static class MatrixTestConfig {
        @Bean
        AlgaShopSecurityProperties algaShopSecurityProperties() {
            var properties = new AlgaShopSecurityProperties();
            properties.setDefaultRedirectUri("http://localhost:8080/");

            var csp = new AlgaShopSecurityProperties.CspProperties();
            csp.setPolicyDirectives("default-src 'self'");
            properties.setCsp(csp);

            var cors = new AlgaShopSecurityProperties.CorsProperties();
            cors.setAllowedOrigins(List.of("http://localhost:8080"));
            properties.setCors(cors);

            var cookie = new AlgaShopSecurityProperties.CookieProperties();
            cookie.setSameSite("Lax");
            cookie.setDomainName("localhost");
            cookie.setSecure(false);
            properties.setCookie(cookie);

            return properties;
        }
    }

    // -------------------------------------------------------------------------
    // Tokens sinteticos
    // -------------------------------------------------------------------------

    /** Token de usuario: sub e UUID e NAO aparece na audience. */
    private static RequestPostProcessor userToken(String... authorities) {
        return jwt()
                .jwt(builder -> builder.subject(USER_SUBJECT).audience(List.of(AUDIENCE)))
                .authorities(Stream.of(authorities)
                        .map(SimpleGrantedAuthority::new)
                        .toArray(SimpleGrantedAuthority[]::new));
    }

    /** Token de maquina (client_credentials): sub == client_id, presente na audience. */
    private static RequestPostProcessor machineToken(String... authorities) {
        return jwt()
                .jwt(builder -> builder.subject(MACHINE_SUBJECT)
                        .audience(List.of(MACHINE_SUBJECT, AUDIENCE)))
                .authorities(Stream.of(authorities)
                        .map(SimpleGrantedAuthority::new)
                        .toArray(SimpleGrantedAuthority[]::new));
    }

    // -------------------------------------------------------------------------
    // Rotas administrativas: escopo + NAO ser CUSTOMER
    // -------------------------------------------------------------------------

    static Stream<Arguments> adminRoutes() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/v1/users", "SCOPE_users:read", null, null),
                Arguments.of(HttpMethod.GET, "/api/v1/users/" + USER_ID, "SCOPE_users:read", null, null),
                Arguments.of(HttpMethod.POST, "/api/v1/users", "SCOPE_users:write", JSON, CREATE_BODY),
                Arguments.of(HttpMethod.PUT, "/api/v1/users/" + USER_ID, "SCOPE_users:write", JSON, UPDATE_BODY),
                Arguments.of(HttpMethod.DELETE, "/api/v1/users/" + USER_ID, "SCOPE_users:write", null, null)
        );
    }

    @ParameterizedTest(name = "{0} {1} sem token -> 401")
    @MethodSource("adminRoutes")
    void shouldRejectAdminRequestWithoutToken(HttpMethod method, String path, String scope,
                                              String contentType, String body) throws Exception {
        mockMvc.perform(request(method, path, contentType, body))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("rota administrativa sem token deveria ser 401")
                        .isEqualTo(401));
    }

    // a regra central do desafio: cliente da loja NAO acessa o administrativo,
    // mesmo portando o escopo exigido
    @ParameterizedTest(name = "{0} {1} com {2} mas papel CUSTOMER -> 403")
    @MethodSource("adminRoutes")
    void shouldRejectCustomerOnAdminRoutes(HttpMethod method, String path, String scope,
                                           String contentType, String body) throws Exception {
        mockMvc.perform(request(method, path, contentType, body)
                        .with(userToken(scope, "ROLE_CUSTOMER")))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("papel CUSTOMER com escopo %s deveria ser 403", scope)
                        .isEqualTo(403));
    }

    @ParameterizedTest(name = "{0} {1} sem o escopo exigido -> 403")
    @MethodSource("adminRoutes")
    void shouldRejectAdminRequestWithoutScope(HttpMethod method, String path, String scope,
                                              String contentType, String body) throws Exception {
        mockMvc.perform(request(method, path, contentType, body)
                        .with(userToken("SCOPE_totally:unrelated", "ROLE_MANAGER")))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("token sem o escopo %s deveria ser 403", scope)
                        .isEqualTo(403));
    }

    @ParameterizedTest(name = "{0} {1} com {2} e sem papel CUSTOMER -> passa")
    @MethodSource("adminRoutes")
    void shouldAllowAdminRequestWithRequiredScope(HttpMethod method, String path, String scope,
                                                  String contentType, String body) throws Exception {
        mockMvc.perform(request(method, path, contentType, body)
                        .with(userToken(scope, "ROLE_MANAGER")))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("com o escopo %s a requisicao nao deveria parar na seguranca", scope)
                        .isNotIn(401, 403));
    }

    // -------------------------------------------------------------------------
    // Recurso /me: usuario autenticado nao-maquina; DELETE exige papel CUSTOMER
    // -------------------------------------------------------------------------

    static Stream<Arguments> myUserRoutes() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/v1/users/me", null, null),
                Arguments.of(HttpMethod.PUT, "/api/v1/users/me", JSON, MY_UPDATE_BODY),
                Arguments.of(HttpMethod.DELETE, "/api/v1/users/me", null, null),
                Arguments.of(HttpMethod.POST, "/api/v1/users/me/password-change", null, null)
        );
    }

    @ParameterizedTest(name = "{0} {1} sem token -> 401")
    @MethodSource("myUserRoutes")
    void shouldRejectMyUserRequestWithoutToken(HttpMethod method, String path,
                                               String contentType, String body) throws Exception {
        mockMvc.perform(request(method, path, contentType, body))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("/me sem token deveria ser 401")
                        .isEqualTo(401));
    }

    // maquina nao tem "proprio perfil": canAccessOwnProfile() nega qualquer /me
    @ParameterizedTest(name = "{0} {1} com token de maquina -> 403")
    @MethodSource("myUserRoutes")
    void shouldRejectMachineTokenOnMyUserRoutes(HttpMethod method, String path,
                                                String contentType, String body) throws Exception {
        mockMvc.perform(request(method, path, contentType, body)
                        .with(machineToken("SCOPE_users:read", "SCOPE_users:write")))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("token de maquina em /me deveria ser 403")
                        .isEqualTo(403));
    }

    @ParameterizedTest(name = "{0} {1} com token de usuario -> passa")
    @MethodSource("myUserRoutes")
    void shouldAllowUserTokenOnMyUserRoutes(HttpMethod method, String path,
                                            String contentType, String body) throws Exception {
        // DELETE /me exige o papel CUSTOMER alem do proprio perfil - ver teste dedicado
        String role = HttpMethod.DELETE.equals(method) ? "ROLE_CUSTOMER" : "ROLE_OPERATOR";

        mockMvc.perform(request(method, path, contentType, body)
                        .with(userToken(role)))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("token de usuario em %s nao deveria parar na seguranca", path)
                        .isNotIn(401, 403));
    }

    // so CUSTOMER exclui o proprio perfil; um MANAGER autenticado leva 403 ja no
    // @PreAuthorize (CanDeleteOwnProfile), antes de chegar na camada de aplicacao
    @Test
    void shouldRejectSelfDeleteWithoutCustomerRole() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/users/me")
                        .with(userToken("ROLE_MANAGER")))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("DELETE /me sem papel CUSTOMER deveria ser 403")
                        .isEqualTo(403));
    }

    private static MockHttpServletRequestBuilder request(HttpMethod method, String path,
                                                         String contentType, String body) {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.request(method, path);
        if (contentType != null) {
            builder.contentType(contentType).content(body == null ? "{}" : body);
        }
        return builder;
    }
}
