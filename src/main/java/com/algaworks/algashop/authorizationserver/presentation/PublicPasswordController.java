package com.algaworks.algashop.authorizationserver.presentation;

import com.algaworks.algashop.authorizationserver.application.user.management.PasswordManagementApplicationService;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * As telas publicas do fluxo de senha: pedir o link, e usar o link.
 *
 * Publicas de verdade - quem esqueceu a senha nao tem como estar logado. Toda a
 * autorizacao vem do TOKEN que chega na URL, e por isso ele e o unico segredo do fluxo.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PublicPasswordController {

    private final PasswordManagementApplicationService passwordManagementService;

    @GetMapping("/change-password")
    public String passwordForm(
            @RequestParam(name = "token", required = false)
            String token,
            Model model) {
        if (token == null || token.isBlank()) {
            model.addAttribute("message", "Invalid token.");
            model.addAttribute("success", false);
            return "password-message";
        }

        model.addAttribute("token", token);
        return "password-form";
    }

    @PostMapping(path = "/change-password", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String changePassword(@RequestParam("token") String token,
                                 @RequestParam("newPassword") String newPassword,
                                 Model model) {
        try {
            passwordManagementService.changePasswordWithToken(token, newPassword);
            model.addAttribute("message", "Password changed successfully.");
            model.addAttribute("success", true);
        } catch (AccessDeniedException | AuthUserNotFoundException e) {
            // Os TRES casos - token inexistente, ja usado e expirado - respondem igual.
            // Sem o AuthUserNotFoundException aqui, token invalido escapava para o
            // ApiExceptionHandler e virava um 404 application/problem+json numa tela de
            // navegador, dizendo "User not found by verification token" - feio e informativo
            // demais. E o catch de AccessDenied so pegava o caso de token EXPIRADO.
            log.info("Password change attempt with invalid token");
            model.addAttribute("message", "Invalid token.");
            model.addAttribute("success", false);
        }

        return "password-message";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    /**
     * Responde SEMPRE a mesma coisa, exista ou nao a conta.
     *
     * Deixar o AuthUserNotFoundException subir daria uma resposta diferente para e-mail
     * cadastrado e nao cadastrado - um oraculo para descobrir quem tem conta no AlgaShop.
     * E a mesma decisao que o login ja tomava desde a Fase 24, aplicada aqui.
     */
    @PostMapping("/forgot-password")
    public String forgotPasswordProcessing(@RequestParam("email") String email) {
        try {
            passwordManagementService.requestPasswordChange(email);
        } catch (AuthUserNotFoundException e) {
            log.info("Password change requested for unknown e-mail");
        }
        return "forgot-password-message";
    }
}