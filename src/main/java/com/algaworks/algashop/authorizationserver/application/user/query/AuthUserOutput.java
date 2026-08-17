package com.algaworks.algashop.authorizationserver.application.user.query;

import com.algaworks.algashop.authorizationserver.domain.user.AuthUser;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserOutput {

    private UUID id;
    private String name;
    private String email;
    private AuthUserType type;
    private boolean enabled;

    // converte domain para dto sem precisar de um mapper
    public static AuthUserOutput from(AuthUser user) {
        return AuthUserOutput.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .type(user.getType())
                .enabled(user.isEnabled())
                .build();
    }
}