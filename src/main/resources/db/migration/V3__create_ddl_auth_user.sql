create table auth_user (
  id uuid not null,
  email varchar(255) default null,
  password varchar(255) default null,
  name varchar(255) default null,
  version bigint not null,
  enabled boolean not null,
  type varchar(255) check (type in ('MANAGER', 'OPERATOR', 'CUSTOMER')),
  created_at timestamp with time zone,
  created_by_user_id uuid,
  last_modified_by_user_id uuid,
  last_modified_date timestamp with time zone,
  constraint uq_auth_user_email unique (email),
  primary key (id)
);