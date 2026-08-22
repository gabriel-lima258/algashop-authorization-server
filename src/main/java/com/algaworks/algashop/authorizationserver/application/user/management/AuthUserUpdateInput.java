package com.algaworks.algashop.authorizationserver.application.user.management;

import com.algaworks.algashop.authorizationserver.domain.user.AuthUserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserUpdateInput {

    @NotBlank
    private String name;

    @NotNull
    private AuthUserType type;

    // Boolean (nao primitivo) para o @NotNull ter efeito: com boolean, um PUT sem o
    // campo virava enabled=false e desabilitava o usuario em silencio
    @NotNull
    private Boolean enabled;
}