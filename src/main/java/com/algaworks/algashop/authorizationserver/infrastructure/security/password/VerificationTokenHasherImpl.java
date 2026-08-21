package com.algaworks.algashop.authorizationserver.infrastructure.security.password;

import com.algaworks.algashop.authorizationserver.domain.user.VerificationTokenHasher;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * SHA-256 + Base64 URL-safe. Diferente de senha, token de verificacao NAO usa BCrypt:
 * ele e aleatorio de 24 caracteres (nao ha dicionario a proteger), tem vida curta, e
 * precisa ser buscavel por hash no banco - BCrypt gera salt novo a cada chamada e nunca
 * daria o mesmo valor para o mesmo token.
 *
 * MessageDigest.isEqual compara em tempo constante, contra ataque de temporizacao.
 */
@Component
public class VerificationTokenHasherImpl implements VerificationTokenHasher {

    @Override
    public String generate() {
        return RandomStringUtils.secure().nextAlphabetic(24);
    }

    // codigo semelhante ao PKCE
    @SneakyThrows
    @Override
    public String hash(String plainToken) {
        MessageDigest messageHasher = MessageDigest.getInstance("SHA-256");
        byte[] hash = messageHasher.digest(plainToken.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    @Override
    public boolean isEqual(String hashed, String plainToken) {
        return MessageDigest.isEqual(
                hashed.getBytes(StandardCharsets.UTF_8),
                hash(plainToken).getBytes(StandardCharsets.UTF_8)
        );
    }
}
