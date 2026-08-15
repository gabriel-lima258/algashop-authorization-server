package com.algaworks.algashop.authorizationserver.infrastructure.security.persistence;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

// Tira o estado do OAuth2 da memoria e poe no banco.
//
//   - o REFRESH TOKEN vive 1 hora e representa uma sessao. Perde-lo no restart significa
//     deslogar todo mundo a cada deploy.
//   - o CONSENTIMENTO e uma decisao da PESSOA. Uma decisao do usuario que some no deploy
//     nunca foi uma decisao de verdade - o app voltaria a pedir permissao do zero.
//
// Sem estes dois beans o Spring usa as versoes InMemory, que funcionam e nao avisam nada.
@Configuration
public class OAuth2PersistenceConfig {

    // Guarda uma linha por AUTORIZACAO - nao por token. A mesma linha carrega o codigo,
    // o access token, o refresh token e o id token daquela concessao, com metadados e
    // prazos. E tambem o que permite revogar: apagar a linha invalida tudo de uma vez.
    @Bean
    public JdbcOAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository
    ) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    // Guarda o que a PESSOA autorizou para um CLIENTE - chave (registered_client_id,
    // principal_name). E acumulativo: pedir um escopo novo depois soma ao que ja havia,
    // e o que ja foi consentido nao volta a ser perguntado.
    @Bean
    public JdbcOAuth2AuthorizationConsentService auth2AuthorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository
    ) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }
}
