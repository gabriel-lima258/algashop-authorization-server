package com.algaworks.algashop.authorizationserver.application.user.mail;

import com.algaworks.algashop.authorizationserver.domain.user.AuthUser;

/**
 * Porta de e-mail na camada de APLICACAO (nao no dominio): mandar e-mail nao e regra de
 * negocio de usuario, e efeito colateral de um caso de uso. O dominio gera o token; quem
 * o entrega e outra responsabilidade.
 */
public interface AuthUserMailSender {
    void sendActivationEmail(AuthUser user, String token);
    void sendPasswordChangeEmail(AuthUser user, String token);
}
