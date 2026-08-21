package com.algaworks.algashop.authorizationserver.domain.user;

/**
 * Porta de senha, no DOMINIO. O agregado precisa gerar e cifrar senha, mas nao pode saber
 * que existe BCrypt - por isso a interface mora aqui e a implementacao na infraestrutura.
 * E o que permite AuthUser.brandNew() cifrar sem importar Spring Security.
 */
public interface AuthUserPasswordManager {
    String generate();
    String encrypt(String plainPassword);
    boolean matches(String plainPassword, String encryptPassword);
}
