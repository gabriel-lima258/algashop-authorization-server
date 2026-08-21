package com.algaworks.algashop.authorizationserver.infrastructure.mail;

import com.algaworks.algashop.authorizationserver.application.user.UserAccountProperties;
import com.algaworks.algashop.authorizationserver.application.user.mail.AuthUserMailSender;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUser;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

/**
 * Adapter de e-mail: monta o link com o token EM TEXTO PURO e envia.
 *
 * Este e o unico lugar do sistema que ve o token legivel - no banco so existe o hash.
 * E por isso que "reenviar o mesmo link" e impossivel: perder o e-mail obriga a gerar
 * outro token.
 *
 * @Async para nao prender a resposta HTTP no tempo do servidor SMTP, e try/catch em volta
 * do envio para que uma falha de e-mail nao derrube a operacao que o disparou.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthUserMailSenderImpl implements AuthUserMailSender {

    private final JavaMailSender javaMailSender;
    private final UserAccountProperties properties;

    // usando async para evitar delay ao enviar um post junto com serviço de email, um age idempendente do outro
    @Override
    @Async
    public void sendActivationEmail(AuthUser user, String token) {
        String subject = "AlgaShop - Active your account";
        String formatedDuration = formatDuration(properties.getToken().getActivationTtl());
        URI link = buildLink(token);

        String body = """
                Hello %s,
                Use the link bellow to set your password and activate your account:
                %s
                This link expires in %s
                """.formatted(user.getName(), link, formatedDuration);

        send(user.getEmail(), subject, body);
    }

    @Override
    @Async
    public void sendPasswordChangeEmail(AuthUser user, String token) {
        String subject = "AlgaShop - Change your password";
        String formatedDuration = formatDuration(properties.getToken().getPasswordResetTtl());
        URI link = buildLink(token);

        String body = """
                Hello %s,
                Use the link bellow to change your password:
                %s
                This link expires in %s
                """.formatted(user.getName(), link, formatedDuration);

        send(user.getEmail(), subject, body);
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        if (hours > 0) {
            return hours + " hour(s)";
        }

        return duration.toMinutes() + " minute(s)";
    }

    private URI buildLink(String token) {
        return UriComponentsBuilder.fromUriString(properties.getMail().getPasswordChangeUrl())
                .queryParam("token", token)
                .build()
                .toUri();
    }

    private void send(String to, String subject, String body) {
        // try para não travar o fluxo caso de erro
        try {
            log.info("Sending email to {} - {}", to, subject);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(properties.getMail().getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);

            javaMailSender.send(message);

        } catch (Exception e) {
            log.error("Error when sending email to {}: {}", to, e.getMessage(), e);
        }
    }
}
