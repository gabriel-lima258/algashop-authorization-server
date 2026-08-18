package com.algaworks.algashop.authorizationserver.infrastructure.security;

import com.algaworks.algashop.authorizationserver.infrastructure.security.oidc.OidcUserInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.authorization.oidc.web.authentication.OidcLogoutAuthenticationSuccessHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * Configuração central de segurança web do Authorization Server.
 *
 * Define duas SecurityFilterChain separadas, pois o servidor atende dois tipos de requisição
 * com necessidades diferentes:
 *
 * 1. Endpoints do protocolo OAuth2/OIDC (/oauth2/authorize, /oauth2/token, /oauth2/jwks,
 *    /userinfo, /.well-known/...) — tratados pela chain de Order(1), com as regras do
 *    Spring Authorization Server.
 *
 * 2. Aplica segurança de endpoints de usuarios dentro de "/api/*" e libera somente o actuator
 *
 * 3. Demais requisições da aplicação (ex.: a própria página de login) — tratadas pela
 *    chain de Order(3), com autenticação via formulário.
 *
 * A ordem importa: o Spring avalia as chains na sequência de @Order e usa a primeira cujo
 * securityMatcher casar com a requisição; a chain do protocolo precisa vir antes da default.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class AuthorizationServerSecurityConfig {

    private final OidcUserInfoMapper oidcUserInfoMapper;
    private final OidcLogoutAuthenticationSuccessHandler oidcLogoutAuthenticationSuccessHandler;
    private final AlgaShopSecurityProperties properties;

    /**
     * Chain exclusiva dos endpoints do protocolo OAuth2/OIDC.
     *
     * - securityMatcher(getEndpointsMatcher()): restringe esta chain apenas aos endpoints
     *   expostos pelo Spring Authorization Server, deixando o resto para a chain default.
     * - oidc(...): habilita o suporte a OpenID Connect (ID Token, userinfo endpoint,
     *   discovery /.well-known/openid-configuration) e registra o OidcUserInfoMapper
     *   como responsável por montar a resposta do /userinfo (por padrão o Spring exige
     *   um ID Token na autorização; com o mapper customizado controlamos as claims
     *   devolvidas e atendemos também acessos feitos só com access token).
     * - anyRequest().authenticated(): nenhum endpoint do protocolo é acessível anonimamente.
     * - exceptionHandling + LoginUrlAuthenticationEntryPoint: quando um usuário não autenticado
     *   chega via navegador (requisição que aceita HTML, ex.: /oauth2/authorize no fluxo
     *   authorization_code), redireciona para /login em vez de responder 401 — clients de API
     *   (JSON) continuam recebendo o erro padrão.
     *   headers permite o carregamento de iframes
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerFilterChain(HttpSecurity http) {
        var authorizationServer = new OAuth2AuthorizationServerConfigurer();

        http.securityMatcher(authorizationServer.getEndpointsMatcher())
                .cors(Customizer.withDefaults())
                .headers(headers -> {
                    var csp = properties.getCsp();
                    headers.contentSecurityPolicy(c -> c.policyDirectives(csp.getPolicyDirectives()));
                })
                .with(authorizationServer, configurer -> {
                    configurer.oidc(oidc -> oidc
                            .logoutEndpoint(logout -> logout.logoutResponseHandler(oidcLogoutAuthenticationSuccessHandler))
                            .userInfoEndpoint(userInfo -> userInfo.userInfoMapper(oidcUserInfoMapper))
                    );
                })
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(
                        exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                );

        return http.build();
    }

    // configuração de filtro de resources de authorization server
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) {
        http.securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * Chain default para todas as requisições que não são do protocolo OAuth2/OIDC.
     *
     * - anyRequest().authenticated(): toda a aplicação exige usuário autenticado em outros resources.
     * - formLogin(withDefaults()): habilita o login por formulário do Spring Security
     *   (gera a página /login), que autentica o usuário na sessão — é para cá que a
     *   chain de Order(1) redireciona antes de continuar o fluxo de autorização.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .formLogin(Customizer.withDefaults());
        return http.build();
    }
}
