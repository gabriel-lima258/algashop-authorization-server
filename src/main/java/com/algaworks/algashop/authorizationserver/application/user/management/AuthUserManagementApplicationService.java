package com.algaworks.algashop.authorizationserver.application.user.management;

import com.algaworks.algashop.authorizationserver.application.security.SecurityCheckApplicationService;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserNotFoundException;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserOutput;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUser;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthUserManagementApplicationService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityCheckApplicationService securityCheck;

    public AuthUserOutput create(AuthUserInput input) {
        if (!securityCheck.canRegisterUserOfType(input.getType())) {
            throw new AccessDeniedException("Cannot register user of type " + input.getType());
        }

        if (authUserRepository.existsByEmail(input.getEmail())) {
            throw new AuthUserEmailAlreadyInUseException(input.getEmail());
        }

        String tempPassword = RandomStringUtils.secure().nextAlphabetic(12);
        System.out.println(tempPassword);
        String passwordHash = passwordEncoder.encode(tempPassword);

        AuthUser user = AuthUser.brandNew(
                input.getEmail(),
                input.getName(),
                input.getType(),
                passwordHash
        );

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
