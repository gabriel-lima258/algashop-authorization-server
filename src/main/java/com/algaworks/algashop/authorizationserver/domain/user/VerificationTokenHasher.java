package com.algaworks.algashop.authorizationserver.domain.user;

// dominio para geração de token de email

/**
 * Porta do token de verificacao. Tres operacoes que o agregado precisa e nao sabe fazer:
 * gerar aleatorio, resumir (hash) e comparar em tempo constante.
 *
 * O token viaja em texto puro no e-mail; no banco fica so o hash. Mesma ideia do PKCE:
 * quem le a tabela nao consegue reconstruir o link.
 */
public interface VerificationTokenHasher {
    String generate();
    String hash(String plainToken);
    boolean isEqual(String hashed, String plainToken);
}
