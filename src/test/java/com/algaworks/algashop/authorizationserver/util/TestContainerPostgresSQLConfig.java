package com.algaworks.algashop.authorizationserver.util;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

// Mesmo padrao dos outros tres servicos. O container e static para subir UMA vez por
// execucao, em vez de um por classe de teste - o custo de inicializacao do Postgres
// domina o tempo da suite quando isso escapa.
//
// @ServiceConnection substitui url, usuario e senha do datasource pelos do container,
// sem @DynamicPropertySource: e por isso que o application-test-env.yaml nao declara
// conexao nenhuma. O Flyway pega carona no mesmo datasource e roda as migrations do
// zero a cada execucao - o que faz esta suite exercitar o schema, e nao so o contexto.
@TestConfiguration
public class TestContainerPostgresSQLConfig {

    private static PostgreSQLContainer postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Bean
    @ServiceConnection
    protected static PostgreSQLContainer postgresSQLContainer() {
        return postgreSQLContainer;
    }
}
