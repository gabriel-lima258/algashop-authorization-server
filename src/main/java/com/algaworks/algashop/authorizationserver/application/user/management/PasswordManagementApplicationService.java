package com.algaworks.algashop.authorizationserver.application.user.management;

import com.algaworks.algashop.authorizationserver.application.user.UserAccountProperties;
import com.algaworks.algashop.authorizationserver.application.user.mail.AuthUserMailSender;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserNotFoundException;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUser;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserPasswordManager;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserRepository;
import com.algaworks.algashop.authorizationserver.domain.user.VerificationTokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orquestra o fluxo de senha; as REGRAS ficam no agregado.
 *
 * Repare na divisao: este servico busca, chama o AuthUser e salva. Quem valida o token,
 * confere a expiracao, troca a senha e marca o e-mail como verificado e o proprio
 * AuthUser - por isso "orquestrado via dominio".
 *
 * A busca e por HASH (o banco nao tem o token legivel), e as excecoes de dominio
 * (IllegalArgument/IllegalState) sao traduzidas para AccessDenied: token invalido e
 * token expirado devem ser indistinguiveis para quem tenta adivinhar.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PasswordManagementApplicationService {

    private final AuthUserRepository authUserRepository;
    private final AuthUserPasswordManager passwordManager;
    private final VerificationTokenHasher tokenHasher;
    private final UserAccountProperties properties;

    private final AuthUserMailSender emailSender;

    public void changePasswordWithToken(String plainToken, String newPlainPassword) {
        // 1 - gera um hash de token
        String hash = tokenHasher.hash(plainToken);
        // 2 - faz a busca do token no banco
        AuthUser user = authUserRepository.findByVerificationToken(hash)
                .orElseThrow(() -> new AuthUserNotFoundException("User not found by verification token"));

        try {
            // 3 - trocamos a senha com a regra de dominio
            user.changePasswordWithToken(plainToken, newPlainPassword, passwordManager, tokenHasher);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new AccessDeniedException(e.getMessage());
        }

        // 4 - salvamos no repositório
        authUserRepository.save(user);
    }

    public void requestPasswordChange(UUID userId) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new AuthUserNotFoundException(userId));
        requestPassword(user);
    }

    public void requestPasswordChange(String email) {
        AuthUser user = authUserRepository.findByEmail(email)
                .orElseThrow(() -> new AuthUserNotFoundException(email));
        requestPassword(user);
    }

    private void requestPassword(AuthUser user) {
        String plainToken = user.generateVerificationToken(properties.getToken().getPasswordResetTtl(), tokenHasher);
        // envia o email com token
        emailSender.sendPasswordChangeEmail(user, plainToken);
        authUserRepository.save(user);
    }
}
