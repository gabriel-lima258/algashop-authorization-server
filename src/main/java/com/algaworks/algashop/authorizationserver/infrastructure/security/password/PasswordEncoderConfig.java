package com.algaworks.algashop.authorizationserver.infrastructure.security.password;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * Configura o PasswordEncoder usado pelo Authorization Server para codificar e validar senhas
 * (de usuários e de client secrets).
 *
 * Usa o DelegatingPasswordEncoder, que delega a codificação/validação para o encoder correto
 * com base no prefixo armazenado junto da senha (ex.: {bcrypt}$2a..., {noop}123).
 * Isso permite suportar múltiplos algoritmos ao mesmo tempo e migrar de algoritmo sem
 * invalidar as senhas já gravadas.
 *
 * Encoders registrados:
 * - bcrypt: algoritmo padrão para novas senhas (seguro, com salt)
 * - noop: senha em texto puro, sem criptografia (apenas para testes/desenvolvimento)
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();

        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put("noop", NoOpPasswordEncoder.getInstance());

        // "bcrypt" é o encoder padrão: senhas novas serão gravadas com o prefixo {bcrypt}
        return new DelegatingPasswordEncoder("bcrypt", encoders);
    }
}
