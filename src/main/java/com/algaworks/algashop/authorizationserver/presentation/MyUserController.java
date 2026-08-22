package com.algaworks.algashop.authorizationserver.presentation;

import com.algaworks.algashop.authorizationserver.application.security.SecurityCheckApplicationService;
import com.algaworks.algashop.authorizationserver.application.user.management.*;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserOutput;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserQueryService;
import com.algaworks.algashop.authorizationserver.infrastructure.security.check.SecurityAnnotations;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.algaworks.algashop.authorizationserver.infrastructure.security.check.SecurityAnnotations.*;

/**
 * Controller REST que expõe os dados do próprio usuário autenticado ("quem sou eu").
 *
 * Funcionamento do GET /api/v1/users/me:
 * 1. Usa o SecurityCheckApplicationService para extrair o UUID do usuário a partir
 *    do token JWT da requisição (claim "sub") — o cliente não informa o ID na URL,
 *    o que impede consultar dados de outro usuário.
 * 2. Com esse ID, consulta o AuthUserQueryService em db e retorna um AuthUserOutput
 *    com os dados do usuário.
 *
 * Se a requisição vier de um client de máquina (client_credentials), a extração do
 * user ID falha com AccessDeniedException, pois não há usuário humano associado.
 */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class MyUserController {

    private final SecurityCheckApplicationService securityCheck;
    private final AuthUserQueryService queryService;
    private final AuthUserManagementApplicationService userManagementApplicationService;
    private final PasswordManagementApplicationService passwordManagementApplicationService;

    @GetMapping
    @CanAccessOwnProfile
    public AuthUserOutput getMe() {
        UUID userId = securityCheck.getAuthenticatedUserId();
        return queryService.findById(userId);
    }

    @PostMapping("/password-change")
    @CanAccessOwnProfile
    public void requestPasswordChange() {
        passwordManagementApplicationService.requestPasswordChange(securityCheck.getAuthenticatedUserId());
    }

    @PutMapping
    @CanAccessOwnProfile
    public AuthUserOutput updateMe(@RequestBody @Valid MyUserUpdateInput input) {
        return userManagementApplicationService.update(securityCheck.getAuthenticatedUserId(), input);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @CanDeleteOwnProfile
    public void deleteMe() {
        userManagementApplicationService.anonymize(securityCheck.getAuthenticatedUserId());
    }
}
