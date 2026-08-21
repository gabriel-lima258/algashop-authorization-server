package com.algaworks.algashop.authorizationserver.presentation;

import com.algaworks.algashop.authorizationserver.application.security.SecurityCheckApplicationService;
import com.algaworks.algashop.authorizationserver.application.user.management.PasswordManagementApplicationService;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserOutput;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private final PasswordManagementApplicationService passwordManagementApplicationService;

    @GetMapping
    @CanAccessOwnProfile
    public AuthUserOutput getMe() {
        UUID authenticatedUserId = securityCheck.getAuthenticatedUserId();
        return queryService.findById(authenticatedUserId);
    }

    @PostMapping("/password-change")
    @CanAccessOwnProfile
    public void requestPasswordChange() {
        passwordManagementApplicationService.requestPasswordChange(securityCheck.getAuthenticatedUserId());
    }
}
