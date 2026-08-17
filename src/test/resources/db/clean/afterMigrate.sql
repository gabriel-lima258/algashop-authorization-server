-- ===================================================================================
-- Flyway afterMigrate.sql — Versão de TESTE (profile test)
-- ===================================================================================
--
-- Substitui o afterMigrate de db/testdata (que insere o seed de 3 usuarios): o profile
-- test troca o location para db/clean, entao ESTE script roda no lugar do seed.
-- Executa 1x na subida do contexto; o isolamento entre testes fica por conta do
-- rollback do @Transactional. Ver docs/03-testes-integracao/testes-integracao-query-services.md
--
-- CASCADE e obrigatorio: spring_session_attributes referencia spring_session por FK,
-- e o Postgres recusa TRUNCATE em tabela referenciada sem ele.
-- auth_user PRECISA estar na lista — e a tabela que o seed popula e que os ITs de
-- listagem/paginacao contam; sobrar linha aqui quebra totalElements/totalPages.

truncate table auth_user cascade;
truncate table oauth2_authorization cascade;
truncate table oauth2_authorization_consent cascade;
truncate table spring_session cascade;
truncate table spring_session_attributes cascade;
