package com.algaworks.algashop.authorizationserver.util;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import java.util.UUID;

// Substitui, em teste, a autoconfiguracao do Spring Authorization Server.
//
// Por que ela nao roda aqui: o OAuth2AuthorizationServerAutoConfiguration so entra quando
// existem propriedades spring.security.oauth2.authorizationserver.client.* - e elas moram
// so no application-development-env.yaml, que o grupo de perfis 'test' nao carrega. Sem ela
// somem, em cascata, os beans que o AuthorizationServerSecurityConfig exige:
// RegisteredClientRepository e AuthorizationServerSettings, aqui - e o JwtDecoder, que o
// AbstractApplicationTest cobre com @MockitoBean (estes ITs consultam o banco, nunca
// validam token de verdade).
//
// Repare que nao existe spring.security.oauth2.resourceserver.* em lugar nenhum do
// servico: o chain de /api/** e resource server DE SI MESMO e usa o mesmo JwtDecoder que
// a autoconfiguracao deriva do JWKSource. Em producao essa chave e gerada a cada subida e
// vive em memoria - e a razao da pendencia "cada reinicio invalida todo JWT emitido".
//
// ATENCAO: @TestConfiguration NAO entra por component scan. So vale com @Import explicito
// (ver AbstractApplicationTest). Sem o @Import esta classe compila e nao faz nada - foi
// exatamente o que aconteceu, e o sintoma foi NoSuchBeanDefinitionException longe daqui.
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:9000")
                .build();
    }

    @Bean
    RegisteredClientRepository registeredClientRepository() {
        RegisteredClient client =
                RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId("test-client")
                        .clientSecret("{noop}123")
                        .authorizationGrantType(
                                AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .clientAuthenticationMethod(
                                ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .scope("read")
                        .build();

        return new InMemoryRegisteredClientRepository(client);
    }
}