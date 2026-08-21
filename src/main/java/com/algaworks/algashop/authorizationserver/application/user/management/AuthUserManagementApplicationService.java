package com.algaworks.algashop.authorizationserver.application.user.management;

import com.algaworks.algashop.authorizationserver.application.security.SecurityCheckApplicationService;
import com.algaworks.algashop.authorizationserver.application.user.UserAccountProperties;
import com.algaworks.algashop.authorizationserver.application.user.mail.AuthUserMailSender;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserNotFoundException;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserOutput;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUser;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserPasswordManager;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserRepository;
import com.algaworks.algashop.authorizationserver.domain.user.VerificationTokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthUserManagementApplicationService {

    private final AuthUserRepository authUserRepository;
    private final SecurityCheckApplicationService securityCheck;
    private final AuthUserPasswordManager passwordManager;
    private final VerificationTokenHasher tokenHasher;
    private final UserAccountProperties properties;

    private final AuthUserMailSender emailSender;

    public AuthUserOutput create(AuthUserInput input) {
        if (!securityCheck.canRegisterUserOfType(input.getType())) {
            throw new AccessDeniedException("Cannot register user of type " + input.getType());
        }

        if (authUserRepository.existsByEmail(input.getEmail())) {
            throw new AuthUserEmailAlreadyInUseException(input.getEmail());
        }

        AuthUser user = AuthUser.brandNew(
                input.getEmail(),
                input.getName(),
                input.getType(),
                passwordManager
        );

        // o usuario ativa o fluxo de geração de token de verificação
        String plainHasher = user.generateVerificationToken(properties.getToken().getActivationTtl(), tokenHasher);

        // envia email com token
        emailSender.sendActivationEmail(user, plainHasher);

        return AuthUserOutput.from(authUserRepository.save(user));
    }

    public AuthUserOutput update(UUID userId, AuthUserUpdateInput input) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new AuthUserNotFoundException(userId));

        verifyCanEditUser(user, input);

        user.setName(input.getName());
        user.setType(input.getType());
        user.setEnabled(input.isEnabled());

        return AuthUserOutput.from(authUserRepository.save(user));
    }

    public void anonymize(UUID userId) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new AuthUserNotFoundException(userId));
        user.anonymize();
        authUserRepository.save(user);
    }

    private void verifyCanEditUser(AuthUser authUser, AuthUserUpdateInput input) {
        if (!securityCheck.canEditUser(authUser.getType(), authUser.getId())) {
            throw new AccessDeniedException("Cannot edit user of type " + authUser.getType());
        }

        if (!securityCheck.canChangeUserType(authUser.getType(), input.getType())) {
            throw new AccessDeniedException("Cannot change user type to " + input.getType());
        }
    }
}
