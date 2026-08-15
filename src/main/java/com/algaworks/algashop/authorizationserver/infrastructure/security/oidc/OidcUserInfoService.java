package com.algaworks.algashop.authorizationserver.infrastructure.security.oidc;

import com.algaworks.algashop.authorizationserver.domain.AuthUser;
import com.algaworks.algashop.authorizationserver.domain.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

/**
 * Monta o OidcUserInfo (conjunto de claims de identificação do usuário no padrão OpenID Connect)
 * a partir dos dados do AuthUser gravados no banco.
 *
 * É usado pelo OAuth2TokenCustomizerConfig para enriquecer o ID Token e também serve de fonte
 * para o userinfo endpoint. O padrão OIDC define claims como name, email, birthdate, address,
 * gender etc.; aqui carregamos do repositório as que fazem sentido para a aplicação, além de
 * claims customizadas (type, createdAt).
 */
@Service
@RequiredArgsConstructor
public class OidcUserInfoService {

    private final AuthUserRepository authUserRepository;

    public OidcUserInfo loadUser(String email) {
        AuthUser user = authUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found " + email));

        return OidcUserInfo.builder()
                .subject(user.getId().toString()) // id do user com sub
                .name(user.getName())
                .email(user.getEmail())
                .claim("type", user.getType().name()) // tipo de user, manager e etc
                .claim("createdAt", String.valueOf(user.getCreatedAt().toEpochSecond()))
                .build();
    }
}
