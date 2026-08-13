package com.algaworks.algashop.authorizationserver;

import com.algaworks.algashop.authorizationserver.util.TestContainerPostgresSQLConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * contextLoads parece o teste mais fraco que existe, e neste servico ele nao e.
 *
 * Subir o contexto aqui significa: as duas migrations aplicadas num banco vazio, os
 * quatro clientes do YAML parseados e registrados, e os dois servicos de persistencia
 * (token e consentimento) resolvidos. Um grant type invalido, um redirect-uri malformado
 * ou uma coluna faltando na migration derrubam este teste - e nenhuma dessas coisas tem
 * outro lugar que as pegue.
 *
 * O Testcontainers entrou junto com o banco: sem ele, a suite so rodaria em quem tem o
 * compose de pe, e migraria o banco de desenvolvimento em vez de um descartavel.
 */
@SpringBootTest
@Import(TestContainerPostgresSQLConfig.class)
class AuthorizationServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
