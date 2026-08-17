package com.algaworks.algashop.authorizationserver.application.user.query;

import com.algaworks.algashop.authorizationserver.application.util.PageModel;

import java.util.UUID;

public interface AuthUserQueryService {
    AuthUserOutput findById(UUID userId);
    PageModel<AuthUserOutput> findAll(AuthUserFilter filter);
}
