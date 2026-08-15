package com.algaworks.algashop.authorizationserver.infrastructure.security.query;

import java.util.List;

public interface OAuth2AuthorizationQueryService {
    List<String> findAuthorizationIds(String principalName);
}
