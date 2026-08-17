package com.algaworks.algashop.authorizationserver.application.user.query;

import com.algaworks.algashop.authorizationserver.application.AbstractApplicationTest;
import com.algaworks.algashop.authorizationserver.application.util.PageModel;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUser;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserRepository;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserTestDataBuilder;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserType;
import com.algaworks.algashop.authorizationserver.util.TestContainerPostgresSQLConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// =====================================================================================
// Teste de integração do AuthUserQueryService
// =====================================================================================
//
// ISOLAMENTO DE DADOS:
//   O profile "test" sobrescreve spring.flyway.locations para excluir db/testdata —
//   o seed de 3 usuários do afterMigrate.sql roda fora da transação do teste e
//   contaminaria as contagens desta suíte. Banco nasce vazio; cada teste insere os
//   próprios dados no @BeforeEach e o @Transactional faz rollback ao final.
//   Ver: src/test/resources/application-test-env.yaml
//
// Só existe este IT no serviço por ora — as anotações ficam direto na classe.
// Quando surgir o segundo, extrair um AbstractIntegrationTest como no ordering.
//
// Sem webEnvironment = NONE (que o ordering usa): aqui o contexto PRECISA ser web.
// A autoconfiguração do Spring Authorization Server, que materializa o
// RegisteredClientRepository a partir do YAML, só roda em aplicação servlet — com
// NONE ela some e as @Configuration do serviço que dependem dela quebram o contexto.
// =====================================================================================
class AuthUserQueryServiceIT extends AbstractApplicationTest {

    @Autowired
    private AuthUserQueryService authUserQueryService;

    @Autowired
    private AuthUserRepository authUserRepository;

    private AuthUser johnManager;
    private AuthUser aliceOperator;
    private AuthUser carolOperator;
    private AuthUser bobCustomer;

    @BeforeEach
    void setUp() {
        johnManager = authUserRepository.save(AuthUserTestDataBuilder.aManagerUser());
        aliceOperator = authUserRepository.save(AuthUserTestDataBuilder.anOperatorUser());
        carolOperator = authUserRepository.save(
                AuthUserTestDataBuilder.aUser("carol.operator@algashop.com", "Carol Operator", AuthUserType.OPERATOR));
        bobCustomer = authUserRepository.save(AuthUserTestDataBuilder.aCustomerUser());
    }

    // =====================================================
    //  Busca por ID
    // =====================================================

    @Test
    void shouldFindById() {
        AuthUserOutput output = authUserQueryService.findById(johnManager.getId());

        assertThat(output)
                .extracting(
                        AuthUserOutput::getId,
                        AuthUserOutput::getName,
                        AuthUserOutput::getEmail,
                        AuthUserOutput::getType,
                        AuthUserOutput::isEnabled
                ).containsExactly(
                        johnManager.getId(),
                        johnManager.getName(),
                        johnManager.getEmail(),
                        johnManager.getType(),
                        johnManager.isEnabled()
                );
    }

    @Test
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        UUID nonExistingId = UUID.randomUUID();

        assertThatThrownBy(() -> authUserQueryService.findById(nonExistingId))
                .isInstanceOf(AuthUserNotFoundException.class);
    }

    // =====================================================
    //  Filtro por e-mail — case insensitive, LIKE
    // =====================================================

    @Test
    void shouldFilterByEmailContainingDomainName() {
        AuthUserFilter filter = new AuthUserFilter();
        filter.setEmail("gmail");

        PageModel<AuthUserOutput> result = authUserQueryService.findAll(filter);

        assertThat(result.getContent())
                .hasSize(1)
                .extracting(AuthUserOutput::getEmail)
                .containsExactly(bobCustomer.getEmail());
    }

    @Test
    void shouldFilterByEmailIgnoringCase() {
        AuthUserFilter filter = new AuthUserFilter();
        filter.setEmail("ALGASHOP");

        PageModel<AuthUserOutput> result = authUserQueryService.findAll(filter);

        assertThat(result.getContent())
                .hasSize(3)
                .extracting(AuthUserOutput::getEmail)
                .containsExactlyInAnyOrder(
                        johnManager.getEmail(),
                        aliceOperator.getEmail(),
                        carolOperator.getEmail()
                );
    }

    @Test
    void shouldReturnEmptyPageWhenNoUserMatchesEmail() {
        AuthUserFilter filter = new AuthUserFilter();
        filter.setEmail("nonexistent@xyz.com");

        PageModel<AuthUserOutput> result = authUserQueryService.findAll(filter);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // =====================================================
    //  Filtro por tipo + paginação
    // =====================================================

    @Test
    void shouldFilterByTypeReturningFirstPage() {
        AuthUserFilter filter = new AuthUserFilter();
        filter.setType(AuthUserType.OPERATOR);
        filter.setSize(1);
        filter.setPage(0);

        PageModel<AuthUserOutput> result = authUserQueryService.findAll(filter);

        // sort default NAME ASC: Alice Operator vem antes de Carol Operator
        assertThat(result.getContent())
                .hasSize(1)
                .extracting(AuthUserOutput::getName)
                .containsExactly(aliceOperator.getName());
        assertThat(result.getNumber()).isZero();
        assertThat(result.getSize()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void shouldFilterByTypeReturningSecondPage() {
        AuthUserFilter filter = new AuthUserFilter();
        filter.setType(AuthUserType.OPERATOR);
        filter.setSize(1);
        filter.setPage(1);

        PageModel<AuthUserOutput> result = authUserQueryService.findAll(filter);

        assertThat(result.getContent())
                .hasSize(1)
                .extracting(AuthUserOutput::getName)
                .containsExactly(carolOperator.getName());
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void shouldFilterByTypeReturningAllMatches() {
        AuthUserFilter filter = new AuthUserFilter();
        filter.setType(AuthUserType.OPERATOR);

        PageModel<AuthUserOutput> result = authUserQueryService.findAll(filter);

        assertThat(result.getContent())
                .hasSize(2)
                .extracting(AuthUserOutput::getType)
                .containsOnly(AuthUserType.OPERATOR);
    }
}
