package com.algaworks.algashop.authorizationserver.contract.base;

import com.algaworks.algashop.authorizationserver.application.user.management.AuthUserManagementApplicationService;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserFilter;
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

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(MockMvcBuilders.webAppContextSetup(context)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8).build());

        RestAssuredMockMvc.enableLoggingOfRequestAndResponseIfValidationFails();

        mockFilterUsers();
        mockValidUserFindById();
        mockInvalidUserIdNotFound();
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

    private void mockAnonymizeUserById() {
        Mockito.doNothing().when(authUserManagementApplicationService).anonymize(deleteUserId);
    }

    private void mockInvalidAnonymizeUserId() {
        Mockito.doThrow(new AuthUserNotFoundException(deleteInvalidUserId))
                .when(authUserManagementApplicationService)
                .anonymize(deleteInvalidUserId);
    }

}
