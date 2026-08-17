package com.algaworks.algashop.authorizationserver.infrastructure.security.check;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class SecurityAnnotations {

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasAuthority('SCOPE_users:read')")
    public @interface CanReadUsers {}

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasAuthority('SCOPE_users:write')")
    public @interface CanWriteUsers {}

    // acessamos a classe OAuth2SecurityCheckApplicationServiceImpl e pegamos o metodo canAccessOwnProfile
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("@securityCheck.canAccessOwnProfile()")
    public @interface CanAccessOwnProfile {}
}
