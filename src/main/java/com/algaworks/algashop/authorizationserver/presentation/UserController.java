package com.algaworks.algashop.authorizationserver.presentation;

import com.algaworks.algashop.authorizationserver.application.user.management.AuthUserInput;
import com.algaworks.algashop.authorizationserver.application.user.management.AuthUserManagementApplicationService;
import com.algaworks.algashop.authorizationserver.application.user.management.AuthUserUpdateInput;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserFilter;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserOutput;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserQueryService;
import com.algaworks.algashop.authorizationserver.application.util.PageModel;
import com.algaworks.algashop.authorizationserver.infrastructure.security.check.SecurityAnnotations.CanReadUsers;
import com.algaworks.algashop.authorizationserver.infrastructure.security.check.SecurityAnnotations.CanWriteUsers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthUserManagementApplicationService authUserManagementApplicationService;
    private final AuthUserQueryService authUserQueryService;

    @CanReadUsers
    @GetMapping
    public PageModel<AuthUserOutput> findAll(AuthUserFilter filter) {
        return authUserQueryService.findAll(filter);
    }

    @CanReadUsers
    @GetMapping("/{userId}")
    public AuthUserOutput findById(@PathVariable UUID userId) {
        return authUserQueryService.findById(userId);
    }

    @CanWriteUsers
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthUserOutput create(@RequestBody @Valid AuthUserInput input) {
        return authUserManagementApplicationService.create(input);
    }

    @CanWriteUsers
    @PutMapping("/{userId}")
    public AuthUserOutput update(@PathVariable UUID userId, @RequestBody @Valid AuthUserUpdateInput input) {
        return authUserManagementApplicationService.update(userId, input);
    }

    @CanWriteUsers
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void anonymizeUser(@PathVariable UUID userId) {
        authUserManagementApplicationService.anonymize(userId);
    }
}
