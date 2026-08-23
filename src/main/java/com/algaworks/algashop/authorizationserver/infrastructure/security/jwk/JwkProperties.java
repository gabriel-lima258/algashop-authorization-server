package com.algaworks.algashop.authorizationserver.infrastructure.security.jwk;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

// classe de properties para geração jwk (senhas privadas do security)

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "jwk")
public class JwkProperties {

    @NotBlank
    private String privateKey;
    @NotBlank
    private String privateKeyId;
}
