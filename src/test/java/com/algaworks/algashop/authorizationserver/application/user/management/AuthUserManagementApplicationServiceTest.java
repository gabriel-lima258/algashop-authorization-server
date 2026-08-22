package com.algaworks.algashop.authorizationserver.application.user.management;

import com.algaworks.algashop.authorizationserver.application.security.SecurityCheckApplicationService;
import com.algaworks.algashop.authorizationserver.application.user.UserAccountProperties;
import com.algaworks.algashop.authorizationserver.application.user.mail.AuthUserMailSender;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserNotFoundException;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserOutput;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUser;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserPasswordManager;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserRepository;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserTestDataBuilder;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserType;
import com.algaworks.algashop.authorizationserver.domain.user.VerificationTokenHasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Cobre as regras que o desafio do /me introduziu na camada de aplicacao:
 * o self-update (sobrecarga com MyUserUpdateInput, que so altera o name) e o guard
 * do anonymize - quem e o autenticado, quem esta sendo excluido, e os casos em que a
 * operacao nao deve prosseguir mesmo depois do @PreAuthorize (admin excluindo a si
 * mesmo) ou deve prosseguir sem consultar identidade (token de maquina).
 */
@ExtendWith(MockitoExtension.class)
class AuthUserManagementApplicationServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;
    @Mock
    private SecurityCheckApplicationService securityCheck;
    @Mock
    private AuthUserPasswordManager passwordManager;
    @Mock
    private VerificationTokenHasher tokenHasher;
    @Mock
    private UserAccountProperties properties;
    @Mock
    private AuthUserMailSender emailSender;

    @InjectMocks
    private AuthUserManagementApplicationService service;

    // -------------------------------------------------------------------------
    // Self-update: update(UUID, MyUserUpdateInput)
    // -------------------------------------------------------------------------

    @Test
    void shouldUpdateOnlyNameOnSelfUpdate() {
        AuthUser user = AuthUserTestDataBuilder.aCustomerUser();
        String originalEmail = user.getEmail();

        Mockito.when(authUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
        Mockito.when(securityCheck.canEditUser(user.getType(), user.getId())).thenReturn(true);
        Mockito.when(authUserRepository.save(Mockito.any(AuthUser.class)))
                .thenAnswer(answer -> answer.getArgument(0));

        AuthUserOutput output = service.update(user.getId(),
                MyUserUpdateInput.builder().name("Victoria Garcia").build());

        assertThat(output.getName()).isEqualTo("Victoria Garcia");
        assertThat(output.getEmail()).isEqualTo(originalEmail);
        assertThat(output.getType()).isEqualTo(AuthUserType.CUSTOMER);
        assertThat(output.isEnabled()).isTrue();
    }

    @Test
    void shouldNotSelfUpdateWhenUserDoesNotExist() {
        UUID unknownUserId = UUID.randomUUID();
        Mockito.when(authUserRepository.findById(unknownUserId)).thenReturn(Optional.empty());

        assertThatExceptionOfType(AuthUserNotFoundException.class)
                .isThrownBy(() -> service.update(unknownUserId,
                        MyUserUpdateInput.builder().name("Victoria Garcia").build()));

        Mockito.verify(authUserRepository, Mockito.never()).save(Mockito.any());
    }

    // -------------------------------------------------------------------------
    // Anonymize: exclusao logica e o guard verifyCanArchiveOwnUser
    // -------------------------------------------------------------------------

    @Test
    void shouldAnonymizeWhenCustomerDeletesOwnProfile() {
        AuthUser user = AuthUserTestDataBuilder.aCustomerUser();

        Mockito.when(authUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
        Mockito.when(securityCheck.isMachineAuthenticated()).thenReturn(false);
        Mockito.when(securityCheck.getAuthenticatedUserId()).thenReturn(user.getId());

        service.anonymize(user.getId());

        assertThat(user.getName()).isEqualTo("Anonymized User");
        assertThat(user.getEmail()).isEqualTo("anonymized-" + user.getId() + "@deleted.local");
        assertThat(user.isEnabled()).isFalse();
        Mockito.verify(authUserRepository).save(user);
    }

    @Test
    void shouldNotAllowNonCustomerToDeleteOwnProfile() {
        AuthUser user = AuthUserTestDataBuilder.aManagerUser();

        Mockito.when(authUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
        Mockito.when(securityCheck.isMachineAuthenticated()).thenReturn(false);
        Mockito.when(securityCheck.getAuthenticatedUserId()).thenReturn(user.getId());

        assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> service.anonymize(user.getId()));

        assertThat(user.isEnabled()).isTrue();
        Mockito.verify(authUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void shouldAllowManagerToAnonymizeAnotherUser() {
        AuthUser user = AuthUserTestDataBuilder.anOperatorUser();

        Mockito.when(authUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
        Mockito.when(securityCheck.isMachineAuthenticated()).thenReturn(false);
        Mockito.when(securityCheck.getAuthenticatedUserId()).thenReturn(UUID.randomUUID());

        service.anonymize(user.getId());

        assertThat(user.isEnabled()).isFalse();
        Mockito.verify(authUserRepository).save(user);
    }

    // maquina nao tem identidade de usuario (getAuthenticatedUserId lancaria): o guard
    // precisa liberar o fluxo m2m sem consultar quem e o autenticado
    @Test
    void shouldAllowMachineToAnonymizeUser() {
        AuthUser user = AuthUserTestDataBuilder.aManagerUser();

        Mockito.when(authUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
        Mockito.when(securityCheck.isMachineAuthenticated()).thenReturn(true);

        service.anonymize(user.getId());

        assertThat(user.isEnabled()).isFalse();
        Mockito.verify(authUserRepository).save(user);
        Mockito.verify(securityCheck, Mockito.never()).getAuthenticatedUserId();
    }
}
