package com.algaworks.algashop.authorizationserver.presentation;

import com.algaworks.algashop.authorizationserver.infrastructure.security.AlgaShopSecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// rota para redirecionar para o url padrão caso tente acessar o home do authorization server

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final AlgaShopSecurityProperties properties;

    @GetMapping("/")
    public String home() {
        return "redirect:" + properties.getDefaultRedirectUri();
    }
}
