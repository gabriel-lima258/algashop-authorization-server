package com.algaworks.algashop.authorizationserver.infrastructure.security.userinfo;

import com.algaworks.algashop.authorizationserver.domain.AuthUser;
import com.algaworks.algashop.authorizationserver.domain.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// serviço de busca de usuario e carrega sua informaçoes ao security

@Service
@RequiredArgsConstructor
public class AuthUserDetailService implements UserDetailsService {

    private final AuthUserRepository authUserRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AuthUser user = authUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found " + email));

        return User.withUsername(email)
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .build();
    }
}
