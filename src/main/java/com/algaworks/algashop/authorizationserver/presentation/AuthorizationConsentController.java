package com.algaworks.algashop.authorizationserver.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.*;

// classe de controller da pagina de consent, porém essa página é util quando a aplicação faz requisição para
// clients de terceiros como (Goggle Auth), clients da algashop não deve passar por isso. Isso é afim de teste

@Controller
@RequiredArgsConstructor
public class AuthorizationConsentController {
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationConsentService authorizationConsentService;

    @GetMapping(value = "/oauth2/consent")
    public String consent(Principal principal, Model model,
                          @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
                          @RequestParam(OAuth2ParameterNames.SCOPE) String scope,
                          @RequestParam(OAuth2ParameterNames.STATE) String state,
                          @RequestParam(name = OAuth2ParameterNames.USER_CODE, required = false) String userCode) {

        Set<String> scopesToApprove = new HashSet<>();
        Set<String> previouslyApprovedScopes = new HashSet<>();
        RegisteredClient registeredClient = this.registeredClientRepository.findByClientId(clientId);

        OAuth2AuthorizationConsent currentAuthorizationConsent =
                this.authorizationConsentService.findById(registeredClient.getId(), principal.getName());
        Set<String> authorizedScopes;

        if (currentAuthorizationConsent != null) {
            authorizedScopes = currentAuthorizationConsent.getScopes();
        } else {
            authorizedScopes = Collections.emptySet();
        }

        for (String requestedScope : StringUtils.delimitedListToStringArray(scope, " ")) {
            if (OidcScopes.OPENID.equals(requestedScope)) {
                continue;
            }
            if (authorizedScopes.contains(requestedScope)) {
                previouslyApprovedScopes.add(requestedScope);
            } else {
                scopesToApprove.add(requestedScope);
            }
        }

        model.addAttribute("clientId", clientId);
        model.addAttribute("state", state);
        model.addAttribute("scopes", withDescription(scopesToApprove));
        model.addAttribute("previouslyApprovedScopes", withDescription(previouslyApprovedScopes));
        model.addAttribute("principalName", principal.getName());
        model.addAttribute("userCode", userCode);

        if (StringUtils.hasText(userCode)) {
            model.addAttribute("requestURI", "/oauth2/device_verification");
        } else {
            model.addAttribute("requestURI", "/oauth2/authorize");
        }

        return "consent-page";
    }

    private static Set<ScopeWithDescription> withDescription(Set<String> scopes) {
        Set<ScopeWithDescription> scopeWithDescriptions = new HashSet<>();
        for (String scope : scopes) {
            scopeWithDescriptions.add(new ScopeWithDescription(scope));

        }
        return scopeWithDescriptions;
    }

    public static class ScopeWithDescription {
        private static final String DEFAULT_DESCRIPTION = "UNKNOWN SCOPE.";
        private static final Map<String, String> scopeDescriptions = new HashMap<>();

        static {
            scopeDescriptions.put("openid", "Read your profile information.");
            scopeDescriptions.put("products:read", "Read products.");
            scopeDescriptions.put("products:write", "Create and modify products.");
            scopeDescriptions.put("products:stock:write", "update product stock.");
            scopeDescriptions.put("categories:read", "Read categories.");
            scopeDescriptions.put("categories:write", "Create and modify categories.");
            scopeDescriptions.put("invoices:read", "Read invoices.");
            scopeDescriptions.put("invoices:write", "Create and modify invoices.");
            scopeDescriptions.put("credit-cards:read", "Read credit cards.");
            scopeDescriptions.put("credit-cards:write", "Create and modify credit cards.");
            scopeDescriptions.put("orders:read", "Read orders.");
            scopeDescriptions.put("orders:write", "Create and modify orders.");
            scopeDescriptions.put("customers:read", "Read customer information.");
            scopeDescriptions.put("customers:write", "Create and modify customer information.");
            scopeDescriptions.put("shopping-carts:read", "Read shopping carts.");
            scopeDescriptions.put("shopping-carts:write", "Create and modify shopping carts.");
            scopeDescriptions.put("users:read", "Read user information.");
            scopeDescriptions.put("users:write", "Create and modify users.");
        }

        public final String value;
        public final String description;

        ScopeWithDescription(String value) {
            this.value = value;
            this.description = scopeDescriptions.getOrDefault(value, DEFAULT_DESCRIPTION);
        }
    }

}