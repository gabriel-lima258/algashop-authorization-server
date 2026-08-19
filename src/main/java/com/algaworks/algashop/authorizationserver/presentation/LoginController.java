package com.algaworks.algashop.authorizationserver.presentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        // retorna o nome da pagina redenrizada em resources/templates
        return "login-page";
    }
}
