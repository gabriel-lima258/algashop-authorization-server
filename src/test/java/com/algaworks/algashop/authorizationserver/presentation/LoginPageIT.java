package com.algaworks.algashop.authorizationserver.presentation;

import com.algaworks.algashop.authorizationserver.domain.user.AuthUser;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserRepository;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserTestDataBuilder;
import com.algaworks.algashop.authorizationserver.util.TestContainerPostgresSQLConfig;
import com.algaworks.algashop.authorizationserver.util.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Suite de fumaca das telas do authorization server.
 *
 * POR QUE ELA EXISTE
 * Tudo que liga uma pagina Thymeleaf ao Spring Security e STRING, e nenhuma dessas strings
 * e verificada em compilacao:
 *
 *   - o nome dos campos que o UsernamePasswordAuthenticationFilter le ("username", "password");
 *   - a rota para onde o formulario posta ("/login");
 *   - o campo _csrf, que o Thymeleaf injeta sozinho em formularios com th:action;
 *   - quais rotas sao publicas (a pagina de login e o CSS) e quais exigem sessao.
 *
 * Renomear um campo no HTML nao quebra o build, nao aparece em log, e produz um login que
 * devolve /login?error para a senha CORRETA. E a mesma familia do nome de bean em SpEL
 * (Fase 25) e do escopo em hasAuthority (Fase 21) - so que agora dentro de um arquivo que
 * nem e Java.
 *
 * webEnvironment = MOCK (e nao NONE, como o AbstractApplicationTest) porque aqui o objeto de
 * teste E a camada web: sem servlet nao ha filter chain, e sem filter chain nao ha o que
 * afirmar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestContainerPostgresSQLConfig.class, TestSecurityConfig.class})
class LoginPageIT {

    private static final String PASSWORD = "test123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthUserRepository authUserRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private AuthUser user;

    @BeforeEach
    void setUp() {
        user = authUserRepository.save(AuthUserTestDataBuilder.aManagerUser());
    }

    @Test
    void shouldRenderLoginPageAnonymously() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login-page"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    /**
     * O teste mais valioso da suite: fixa o CONTRATO entre o HTML e o filtro.
     * Os tres nomes abaixo nao aparecem em codigo Java nenhum do projeto - eles vivem
     * dentro do template, e sao exatamente o que o UsernamePasswordAuthenticationFilter
     * procura no corpo do POST.
     */
    @Test
    void loginPageShouldCarryTheFieldsTheFilterExpects() throws Exception {
        String html = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(html)
                .contains("name=\"username\"")
                .contains("name=\"password\"")
                // o _csrf NAO esta escrito no template: o Thymeleaf o injeta em
                // formularios com th:action. Sem ele, todo POST /login volta 403.
                .contains("name=\"_csrf\"")
                .contains("action=\"/login\"");
    }

    @Test
    void shouldServeStaticResourcesAnonymously() throws Exception {
        mockMvc.perform(get("/css/main.css"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRequireAuthenticationOnConsentPage() throws Exception {
        mockMvc.perform(get("/oauth2/consent"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    /** A home nao e rota publica: ela redireciona para a loja, mas so para quem ja entrou. */
    @Test
    void shouldRequireAuthenticationOnHome() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void shouldAuthenticateWithValidCredentials() throws Exception {
        mockMvc.perform(formLogin("/login").user(user.getEmail()).password(PASSWORD))
                .andExpect(authenticated().withUsername(user.getEmail()));
    }

    @Test
    void shouldRedirectToLoginErrorWithInvalidPassword() throws Exception {
        mockMvc.perform(formLogin("/login").user(user.getEmail()).password("senha-errada"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    /** Usuario inexistente e senha errada respondem igual - decisao da Fase 24. */
    @Test
    void shouldRedirectToLoginErrorWithUnknownUser() throws Exception {
        mockMvc.perform(formLogin("/login").user("ninguem@algashop.com").password(PASSWORD))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }
}
