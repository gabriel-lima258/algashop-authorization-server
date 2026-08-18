package com.algaworks.algashop.authorizationserver.contract.base;

import com.algaworks.algashop.authorizationserver.application.user.management.AuthUserInput;
import com.algaworks.algashop.authorizationserver.application.user.management.AuthUserManagementApplicationService;
import com.algaworks.algashop.authorizationserver.application.user.management.AuthUserUpdateInput;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserFilter;
import com.algaworks.algashop.authorizationserver.domain.DomainException;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserNotFoundException;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserOutput;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserOutputTestDataBuilder;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserQueryService;
import com.algaworks.algashop.authorizationserver.application.util.PageModel;
import com.algaworks.algashop.authorizationserver.presentation.UserController;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@WebMvcTest(controllers = UserController.class)
public class UserBase {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AuthUserQueryService authUserQueryService;

    @MockitoBean
    private AuthUserManagementApplicationService authUserManagementApplicationService;

    public static final UUID validUserId = UUID.fromString("019d7764-3e11-7000-8000-000000000001");
    public static final UUID invalidUserId = UUID.fromString("019d7764-3e11-7000-8000-000000000002");
    public static final UUID deleteUserId = UUID.fromString("019d7764-3e11-7000-8000-000000000003");
    public static final UUID deleteInvalidUserId = UUID.fromString("019d7764-3e11-7000-8000-000000000004");
    public static final UUID validUpdateUserId = UUID.fromString("019d7764-3e11-7000-8000-000000000005");
    public static final UUID invalidUpdateUserId = UUID.fromString("019d7764-3e11-7000-8000-000000000006");
    public static final UUID customerUpdateUserId = UUID.fromString("019d7764-3e11-7000-8000-000000000007");

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(MockMvcBuilders.webAppContextSetup(context)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8).build());

        RestAssuredMockMvc.enableLoggingOfRequestAndResponseIfValidationFails();

        mockFilterUsers();
        mockValidUserFindById();
        mockInvalidUserIdNotFound();
        mockCreateUser();
        mockUpdateUser();
        mockInvalidUpdateUserId();
        mockCustomerUpdateNotAllowed();
        mockAnonymizeUserById();
        mockInvalidAnonymizeUserId();
    }

    // o contrato manda ?size=10&page=0 e afirma que a resposta devolve o mesmo size,
    // entao o stub le o size do filtro recebido em vez de devolver valor fixo
    private void mockFilterUsers() {
        Mockito.when(authUserQueryService.findAll(
                Mockito.any(AuthUserFilter.class)
        )).then((answer) -> {
            AuthUserFilter filter = answer.getArgument(0);

            return PageModel.<AuthUserOutput>builder()
                    .number(0)
                    .size(filter.getSize())
                    .totalPages(1)
                    .totalElements(2)
                    .content(
                            List.of(
                                    AuthUserOutputTestDataBuilder.aManagerUser().build(),
                                    AuthUserOutputTestDataBuilder.anOperatorUser().build()
                            )
                    ).build();
        });
    }

    private void mockValidUserFindById() {
        Mockito.when(authUserQueryService.findById(validUserId))
                .thenReturn(AuthUserOutputTestDataBuilder.aManagerUser()
                        .id(validUserId)
                        .build());
    }

    private void mockInvalidUserIdNotFound() {
        Mockito.when(authUserQueryService.findById(invalidUserId))
                .thenThrow(new AuthUserNotFoundException(invalidUserId));
    }

    // os contratos de create/update ecoam campos do request no response
    // (fromRequest().body('$.name') etc.), entao os stubs leem o input recebido
    // em vez de devolver valores fixos - mesmo truque do mockFilterUsers
    private void mockCreateUser() {
        Mockito.when(authUserManagementApplicationService.create(Mockito.any(AuthUserInput.class)))
                .then((answer) -> {
                    AuthUserInput input = answer.getArgument(0);
                    return AuthUserOutput.builder()
                            .id(UUID.randomUUID())
                            .name(input.getName())
                            .email(input.getEmail())
                            .type(input.getType())
                            .enabled(true)
                            .build();
                });
    }

    private void mockUpdateUser() {
        Mockito.when(authUserManagementApplicationService.update(
                        Mockito.eq(validUpdateUserId), Mockito.any(AuthUserUpdateInput.class)))
                .then((answer) -> {
                    AuthUserUpdateInput input = answer.getArgument(1);
                    return AuthUserOutput.builder()
                            .id(validUpdateUserId)
                            .name(input.getName())
                            .email("john.manager@algashop.com")
                            .type(input.getType())
                            .enabled(input.isEnabled())
                            .build();
                });
    }

    private void mockInvalidUpdateUserId() {
        Mockito.when(authUserManagementApplicationService.update(
                        Mockito.eq(invalidUpdateUserId), Mockito.any(AuthUserUpdateInput.class)))
                .thenThrow(new AuthUserNotFoundException(invalidUpdateUserId));
    }

    // o setType do dominio recusa mudar o tipo de um CUSTOMER (DomainException -> 422);
    // a mensagem aqui e a mesma do dominio porque o contrato afirma o campo detail
    private void mockCustomerUpdateNotAllowed() {
        Mockito.when(authUserManagementApplicationService.update(
                        Mockito.eq(customerUpdateUserId), Mockito.any(AuthUserUpdateInput.class)))
                .thenThrow(new DomainException("Cannot change type of a CUSTOMER user"));
    }

    private void mockAnonymizeUserById() {
        Mockito.doNothing().when(authUserManagementApplicationService).anonymize(deleteUserId);
    }

    private void mockInvalidAnonymizeUserId() {
        Mockito.doThrow(new AuthUserNotFoundException(deleteInvalidUserId))
                .when(authUserManagementApplicationService)
                .anonymize(deleteInvalidUserId);
    }

}
