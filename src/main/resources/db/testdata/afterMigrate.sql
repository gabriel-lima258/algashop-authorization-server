set session_replication_role = 'replica';

DELETE FROM auth_user;
DELETE FROM oauth2_authorization;
DELETE FROM oauth2_authorization_consent;
DELETE FROM spring_session_attributes;
DELETE FROM spring_session;
DELETE FROM auth_user_type_client_scope;
DELETE FROM auth_user_type_client_allowed;

INSERT INTO auth_user (id, email, password, name, version, "type", enabled, created_at, last_modified_date, email_verified)
VALUES('6e148bd5-47f6-4022-b9da-07cfaa294f7a', 'john.doe@email.com', '{noop}123456', 'John Doe', 0, 'CUSTOMER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,true);

INSERT INTO auth_user (id, email, password, name, version, "type", enabled, created_at, last_modified_date, email_verified)
VALUES('f6a7b8c9-d0e1-f2a3-b4c5-d6e7f8a9b0c1', 'sophia.anderson@email.com', '{noop}123456', 'Sophia Anderson', 0, 'CUSTOMER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,true);

INSERT INTO auth_user (id, email, password, name, version, "type", enabled, created_at, last_modified_date, email_verified)
VALUES('019d7764-3b02-7be2-9112-039fda30e965', 'victoria.garcia@algashop.com', '{noop}123456', 'Victoria Garcia', 0, 'MANAGER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,true);

INSERT INTO auth_user (id, email, password, name, version, "type", enabled, created_at, last_modified_date, email_verified)
VALUES('019d7764-3b02-70d3-8caf-8d2f02a4b4c2', 'jeff.roman@algashop.com', '{bcrypt}$2a$10$TYlaa0oLIGnqG5Jdoaa.mePxJD9ywmV7F6RiryjR00yqMccyF9zou', 'Jefferson Roman', 0, 'OPERATOR', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,true);

-- categoriza scopes de acordo com roles adequados ao user
INSERT INTO auth_user_type_client_scope (auth_user_type, client_id, scope) VALUES
('MANAGER', 'algashop-admin-web', 'openid'),
('MANAGER', 'algashop-admin-web', 'products:read'),
('MANAGER', 'algashop-admin-web', 'products:write'),
('MANAGER', 'algashop-admin-web', 'products:stock:write'),
('MANAGER', 'algashop-admin-web', 'categories:read'),
('MANAGER', 'algashop-admin-web', 'categories:write'),
('MANAGER', 'algashop-admin-web', 'invoices:read'),
('MANAGER', 'algashop-admin-web', 'orders:read'),
('MANAGER', 'algashop-admin-web', 'customers:read'),
('MANAGER', 'algashop-admin-web', 'shopping-carts:read'),
('MANAGER', 'algashop-admin-web', 'users:read'),
('MANAGER', 'algashop-admin-web', 'users:write'),
('OPERATOR', 'algashop-admin-web', 'openid'),
('OPERATOR', 'algashop-admin-web', 'products:read'),
('OPERATOR', 'algashop-admin-web', 'products:write'),
('OPERATOR', 'algashop-admin-web', 'categories:read'),
('OPERATOR', 'algashop-admin-web', 'categories:write'),
('OPERATOR', 'algashop-admin-web', 'invoices:read'),
('OPERATOR', 'algashop-admin-web', 'orders:read'),
('OPERATOR', 'algashop-admin-web', 'customers:read'),
('OPERATOR', 'algashop-admin-web', 'shopping-carts:read'),
('OPERATOR', 'algashop-admin-web', 'users:read'),
('CUSTOMER', 'algashop-ecommerce-web', 'openid'),
('CUSTOMER', 'algashop-ecommerce-web', 'orders:read'),
('CUSTOMER', 'algashop-ecommerce-web', 'orders:write'),
('CUSTOMER', 'algashop-ecommerce-web', 'customers:read'),
('CUSTOMER', 'algashop-ecommerce-web', 'customers:write'),
('CUSTOMER', 'algashop-ecommerce-web', 'shopping-carts:read'),
('CUSTOMER', 'algashop-ecommerce-web', 'shopping-carts:write'),
('CUSTOMER', 'algashop-ecommerce-web', 'credit-cards:read'),
('CUSTOMER', 'algashop-ecommerce-web', 'credit-cards:write'),
('CUSTOMER', 'algashop-ecommerce-web', 'users:read');


INSERT INTO auth_user_type_client_allowed (auth_user_type, client_id)
VALUES ('MANAGER', 'algashop-admin-web'),
       ('OPERATOR', 'algashop-admin-web'),
       ('CUSTOMER', 'algashop-ecommerce-web');

set session_replication_role = 'origin';