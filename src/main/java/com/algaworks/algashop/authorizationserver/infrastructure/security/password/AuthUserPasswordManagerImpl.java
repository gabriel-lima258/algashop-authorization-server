package com.algaworks.algashop.authorizationserver.infrastructure.security.password;

import com.algaworks.algashop.authorizationserver.domain.user.AuthUserPasswordManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adapter da porta de senha sobre o DelegatingPasswordEncoder (prefixo {bcrypt}/{noop}).
 *
 * O generate() produz uma senha aleatoria que NINGUEM recebe: e so para o registro nascer
 * com um hash valido. Quem define a senha de verdade e o proprio usuario, pelo link do
 * e-mail - foi assim que a pendencia da Fase 25 (senha impressa no stdout) foi fechada.
 */
@Component
@RequiredArgsConstructor
public class AuthUserPasswordManagerImpl implements AuthUserPasswordManager {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String generate() {
        return RandomStringUtils.secure().next(12);
    }

    @Override
    public String encrypt(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }

    @Override
    public boolean matches(String plainPassword, String encryptPassword) {
        return passwordEncoder.matches(plainPassword, encryptPassword);
    }
}
