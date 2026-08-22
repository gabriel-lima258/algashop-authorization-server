package com.algaworks.algashop.authorizationserver.application.user.management;

import com.algaworks.algashop.authorizationserver.application.security.SecurityCheckApplicationService;
import com.algaworks.algashop.authorizationserver.application.user.UserAccountProperties;
import com.algaworks.algashop.authorizationserver.application.user.mail.AuthUserMailSender;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserNotFoundException;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserOutput;
import com.algaworks.algashop.authorizationserver.domain.user.*;
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
        user.setEnabled(input.getEnabled());

        return AuthUserOutput.from(authUserRepository.save(user));
    }

    public AuthUserOutput update(UUID userId, MyUserUpdateInput input) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new AuthUserNotFoundException(userId));

        if (!securityCheck.canEditUser(user.getType(), user.getId())) {
            throw new AccessDeniedException("Cannot edit user");
        }

        user.setName(input.getName());

        return AuthUserOutput.from(authUserRepository.save(user));
    }

    public void anonymize(UUID userId) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new AuthUserNotFoundException(userId));

        verifyCanArchiveOwnUser(user);

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

    private void verifyCanArchiveOwnUser(AuthUser user) {
        // maquina nao tem "proprio perfil" (getAuthenticatedUserId lancaria); o fluxo
        // m2m e sempre administrativo e passa direto
        if (securityCheck.isMachineAuthenticated()) {
            return;
        }
        if (securityCheck.getAuthenticatedUserId().equals(user.getId()) &&
                user.getType() != AuthUserType.CUSTOMER) {
            throw new AccessDeniedException("Only CUSTOMER users can delete their own profile");
        }
    }
}
