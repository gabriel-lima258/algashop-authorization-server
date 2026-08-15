set session_replication_role = 'replica';

DELETE FROM auth_user;
DELETE FROM oauth2_authorization;
DELETE FROM oauth2_authorization_consent;
DELETE FROM spring_session_attributes;
DELETE FROM spring_session;

INSERT INTO auth_user (id, email, password, name, version, "type", enabled, created_at, last_modified_date)
VALUES('019d7764-3b02-7fd5-b0e7-c47c58592857', 'john.doe@email.com', '{noop}123456', 'John Doe', 0, 'CUSTOMER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO auth_user (id, email, password, name, version, "type", enabled, created_at, last_modified_date)
VALUES('019d7764-3b02-7be2-9112-039fda30e965', 'victoria.garcia@algashop.com', '{noop}123456', 'Victoria Garcia', 0, 'MANAGER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO auth_user (id, email, password, name, version, "type", enabled, created_at, last_modified_date)
VALUES('019d7764-3b02-70d3-8caf-8d2f02a4b4c2', 'jeff.roman@algashop.com', '{bcrypt}$2a$10$TYlaa0oLIGnqG5Jdoaa.mePxJD9ywmV7F6RiryjR00yqMccyF9zou', 'Jefferson Roman', 0, 'OPERATOR', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

set session_replication_role = 'origin';