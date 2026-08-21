package com.algaworks.algashop.authorizationserver.domain.user;

import com.algaworks.algashop.authorizationserver.domain.AbstractAuditableAggregateRoot;
import com.algaworks.algashop.authorizationserver.domain.DomainException;
import com.algaworks.algashop.authorizationserver.domain.util.IdGenerator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "auth_user")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthUser extends AbstractAuditableAggregateRoot<AuthUser> {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    private String email;
    private String password;
    private String name;
    private boolean enabled;
    private boolean emailVerified;

    private String verificationToken;

    private OffsetDateTime verificationTokenExpirationDate;

    @Enumerated(EnumType.STRING)
    private AuthUserType type;

    public static AuthUser brandNew(String email,
                                    String name,
                                    AuthUserType type,
                                    AuthUserPasswordManager passwordManager) {
        AuthUser user = new AuthUser();

        user.setId(IdGenerator.generateTimeBasedUUID());
        user.setEmail(email);
        user.setName(name);
        user.setType(type);
        user.setPassword(passwordManager.encrypt(passwordManager.generate()));
        user.setEnabled(true);
        user.setEmailVerified(false);

        return user;
    }

    public void anonymize() {
        this.setName("Anonymized User");
        this.setEmail("anonymized-" + this.id + "@deleted.local");
        this.setEnabled(false);
    }

    public void setName(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException();
        }
        this.name = name;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setType(AuthUserType type) {
        Objects.requireNonNull(type);
        if (this.type == AuthUserType.CUSTOMER) {
            throw new DomainException("Cannot change type of a CUSTOMER user");
        }
        this.type = type;
    }

    /**
     * Gera o token e devolve o TEXTO PURO - que sai daqui e nunca mais volta.
     * O agregado guarda so o hash e o prazo; quem precisar do link tem esta unica chance.
     */
    public String generateVerificationToken(Duration expiresIn, VerificationTokenHasher hasher) {
        String plainToken = hasher.generate();
        // hash é salvo no banco atraves do plain
        this.verificationToken = hasher.hash(plainToken);
        this.verificationTokenExpirationDate = OffsetDateTime.now().plus(expiresIn);
        return plainToken;
    }

    /**
     * A operacao de negocio inteira, num lugar so: valida o token, troca a senha, apaga o
     * token e - se for a primeira vez - marca o e-mail como verificado.
     *
     * Usar o token o CONSOME (cleanVerificationToken): um link vale uma vez. E a ativacao
     * ser efeito da troca de senha e o que faz "definir a primeira senha" e "recuperar a
     * senha" serem o mesmo caminho.
     */
    public void changePasswordWithToken(String plainToken,
                                        String plainPassword,
                                        AuthUserPasswordManager passwordManager,
                                        VerificationTokenHasher tokenHasher
    ) {
        verifyToken(plainToken, tokenHasher);
        setPassword(passwordManager.encrypt(plainPassword));
        cleanVerificationToken();

        if (!isEmailVerified()) {
            setEmailVerified(true);
        }
    }

    /**
     * Duas razoes para nao logar, e a segunda e nova: e-mail nao verificado.
     * Quem foi cadastrado e nunca clicou no link tem conta, tem senha aleatoria que
     * ninguem conhece, e nao entra.
     */
    public boolean isDisabled() {
        return !isEnabled() || !isEmailVerified();
    }

    private void setId(UUID id) {
        Objects.requireNonNull(id);
        this.id = id;
    }

    private void setPassword(String password) {
        if (StringUtils.isBlank(password)) {
            throw new IllegalArgumentException();
        }
        this.password = password;
    }

    private void setEmail(String email) {
        if (StringUtils.isBlank(email)) {
            throw new IllegalArgumentException();
        }
        this.email = email;
    }

    private void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    // limpa os valores no banco após confirmar
    private void cleanVerificationToken() {
        this.verificationToken = null;
        this.verificationTokenExpirationDate = null;
    }

    // Token errado e token expirado lancam excecoes diferentes aqui, mas a aplicacao
    // traduz as duas para o mesmo 403 - quem tenta adivinhar nao deve saber qual foi.
    private void verifyToken(String plainToken, VerificationTokenHasher tokenHasher) {
       if (! tokenHasher.isEqual(this.verificationToken, plainToken)) {
           throw new IllegalArgumentException("Invalid token.");
       }

       if (isTokenExpired()) {
           throw new IllegalStateException("Token has expired.");
       }
    }

    private boolean isTokenExpired() {
        if (verificationTokenExpirationDate == null) {
            return true;
        }
        return OffsetDateTime.now().isAfter(verificationTokenExpirationDate);
    }

}
