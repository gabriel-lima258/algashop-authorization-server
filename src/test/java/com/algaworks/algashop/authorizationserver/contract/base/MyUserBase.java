package com.algaworks.algashop.authorizationserver.contract.base;

import com.algaworks.algashop.authorizationserver.application.security.SecurityCheckApplicationService;
import com.algaworks.algashop.authorizationserver.application.user.management.AuthUserManagementApplicationService;
import com.algaworks.algashop.authorizationserver.application.user.management.MyUserUpdateInput;
import com.algaworks.algashop.authorizationserver.application.user.management.PasswordManagementApplicationService;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserOutput;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserQueryService;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserType;
import com.algaworks.algashop.authorizationserver.presentation.MyUserController;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

// O recurso /me nao tem id no path: o usuario e resolvido pelo SecurityCheck, mockado
// com sub fixo - mesmo padrao do MyCustomerControllerContractTest do ordering.
// O stub do update usa any(MyUserUpdateInput.class): o TIPO do matcher e o que resolve
// a sobrecarga certa entre update(UUID, AuthUserUpdateInput) e update(UUID, MyUserUpdateInput).
@WebMvcTest(controllers = MyUserController.class)
public class MyUserBase {

    public static final UUID AUTHENTICATED_USER_ID =
            UUID.fromString("019d7764-3e11-7000-8000-00000000000a");

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AuthUserQueryService authUserQueryService;

    @MockitoBean
    private AuthUserManagementApplicationService authUserManagementApplicationService;

    @MockitoBean
    private PasswordManagementApplicationService passwordManagementApplicationService;

    @MockitoBean
    private SecurityCheckApplicationService securityCheck;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(MockMvcBuilders.webAppContextSetup(context)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8).build());

        RestAssuredMockMvc.enableLoggingOfRequestAndResponseIfValidationFails();

        Mockito.when(securityCheck.getAuthenticatedUserId()).thenReturn(AUTHENTICATED_USER_ID);

        mockFindMe();
        mockUpdateMe();
    }

    private void mockFindMe() {
        Mockito.when(authUserQueryService.findById(AUTHENTICATED_USER_ID))
                .thenReturn(aCustomerOutput().build());
    }

    // o contrato ecoa o name do request no response, entao o stub le o input recebido
    private void mockUpdateMe() {
        Mockito.when(authUserManagementApplicationService.update(
                        Mockito.eq(AUTHENTICATED_USER_ID), Mockito.any(MyUserUpdateInput.class)))
                .then((answer) -> {
                    MyUserUpdateInput input = answer.getArgument(1);
                    return aCustomerOutput()
                            .name(input.getName())
                            .build();
                });
    }

    private AuthUserOutput.AuthUserOutputBuilder aCustomerOutput() {
        return AuthUserOutput.builder()
                .id(AUTHENTICATED_USER_ID)
                .name("Bob Customer")
                .email("bob.customer@gmail.com")
                .type(AuthUserType.CUSTOMER)
                .enabled(true);
    }
}
