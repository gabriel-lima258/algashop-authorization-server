# algashop-authorization-server

Quem **emite** as credenciais do AlgaShop. O único serviço sem domínio de negócio — sem entidade e com pouquíssimo código, mas com banco: tokens e consentimentos precisam sobreviver a um deploy.

---

## O problema

Dezenove fases construíram um sistema que não sabia quem estava do outro lado. A questão não é "como cada serviço valida senha" — é o contrário disso:

> **Emitir credencial e verificar credencial são responsabilidades diferentes.** Quem verifica nunca vê a senha, e na maioria dos casos nem precisa falar com quem emitiu.

Concentrar a emissão num serviço significa que o segredo mora num lugar só, que auditar e rotacionar vira problema de um serviço, e que acrescentar um microsserviço não acrescenta mais um lugar que sabe validar senha.

---

## Stack

| | |
|---|---|
| **Java** | 25 |
| **Spring Boot** | 4.0.3 |
| **Protocolo** | OAuth 2.1 (Spring Authorization Server) |
| **Porta** | 9000 |
| **Banco** | PostgreSQL — usuários, tokens, consentimentos e sessões (clientes seguem em YAML) |
| **Pacote raiz** | `com.algaworks.algashop.authorizationserver` |

```gradle
implementation 'org.springframework.boot:spring-boot-starter-security-oauth2-authorization-server'
```

Uma dependência e uma `@SpringBootApplication` vazia produzem **seis endpoints** funcionando. O que se configura é *quem pode pedir token e o que esse token vale* — nunca *como* o protocolo funciona.

> A porta é **9000**, não 8081. A 8081 é do `algashop-ordering`, e os dois não subiam juntos; 9000 é também a convenção do Spring Authorization Server.

---

## Os quatro clientes

Todos em `application-development-env.yaml`, divididos por **quem está no fluxo**.

### Com usuário — `authorization_code`

| | `algashop-ecommerce-web` | `algashop-ecommerce-m2m` |
|---|---|---|
| Grant | `authorization_code` + `refresh_token` | `client_credentials` |
| Consentimento | **exigido**, granular por escopo | não se aplica |
| TTLs | código 10m · access 5m · refresh **1h** | access 15m |
| Rotação de refresh | sim (`reuse-refresh-tokens: false`) | — |

O primeiro é o único do projeto com **pessoa** no fluxo: tela de login, tela de consentimento, refresh token. O segundo é o par dele — o que a loja faz sem ninguém logado (listar produto e categoria) mais o cadastro inicial do cliente.

> ⚠️ **PKCE está desligado** (`require-proof-key: false`) no client web. O OAuth 2.1 o tornou obrigatório; está registrado como pendência.

### Sem usuário — `client_credentials`

| | `algashop-test` | `algashop-ordering-service` |
|---|---|---|
| Para que existe | teste manual por `curl` | o `ordering` lendo o catálogo |
| Escopos | os 16 do sistema | **`products:read`** |
| TTL | 15 min | 5 min |

Os dois emitem **JWT**: o resource server confere a assinatura localmente com a chave pública do `/oauth2/jwks`, sem chamar ninguém — e **sem poder revogar**. É por isso que o TTL é curto: com JWT, o tempo de vida é a janela de exposição de um token vazado.

E repare no escopo: o cliente do `ordering` só lê, porque o `ordering` só lê. **O escopo mais estreito que faz o trabalho é o certo.**

---

## Endpoints

| Endpoint | Quem chama | Para quê |
|---|---|---|
| `POST /oauth2/token` | o client | trocar credenciais por token |
| `POST /oauth2/introspect` | o resource server | resolver um token opaco |
| `POST /oauth2/revoke` | o client | cancelar um token |
| `GET /oauth2/jwks` | o resource server | pegar as chaves públicas |
| `GET /.well-known/oauth-authorization-server` | qualquer um | descobrir todos os outros |
| `GET /oauth2/authorize` | o navegador | fluxo com usuário — login e consentimento |
| `GET /userinfo` | o client | claims de identidade do usuário autenticado |
| `GET /connect/logout` | o navegador | RP-initiated logout — encerra a sessão e **revoga** as autorizações |
| `GET /.well-known/openid-configuration` | qualquer um | descoberta OIDC |

O `/oauth2/authorize` passou a ser utilizável na Fase 23, com o client `algashop-ecommerce-web` e o usuário `customer@gmail.com`.

Contrato completo em [`openapi/authorization-server.yml`](https://github.com/gabriel-lima258/algashop-docs/blob/main/openapi/authorization-server.yml).

---

## Como rodar

Não está no `docker-compose` — sobe direto:

```bash
./gradlew bootRun
```

Token opaco:

```bash
curl -s -u algashop-test:testing123 \
  -d grant_type=client_credentials \
  -d scope="products:read products:write" \
  http://localhost:9000/oauth2/token
```

Token JWT:

```bash
curl -s -u algashop-ordering-service:secret123 \
  -d grant_type=client_credentials \
  -d scope=products:read \
  http://localhost:9000/oauth2/token
```

O primeiro `access_token` é uma string sem estrutura; o segundo tem dois pontos e decodifica.

Resolver o opaco, e ver as chaves:

```bash
curl -s -u algashop-test:testing123 -d token=<ACCESS_TOKEN> http://localhost:9000/oauth2/introspect
curl -s http://localhost:9000/oauth2/jwks
```

---

## Usuários e identidade (OIDC)

Desde a Fase 24 o servidor tem usuários de verdade, na tabela `auth_user`, e emite **ID token** além do access token.

| E-mail | Tipo | Senha (`123456`) guardada como |
|---|---|---|
| `john.doe@email.com` | `CUSTOMER` | `{noop}` |
| `victoria.garcia@algashop.com` | `MANAGER` | `{noop}` |
| `jeff.roman@algashop.com` | `OPERATOR` | **`{bcrypt}`** |

Os dois formatos convivem pelo `DelegatingPasswordEncoder` — o prefixo é o que permite trocar de algoritmo sem invalidar as senhas já gravadas.

**Os dois tokens têm públicos diferentes:** o *access token* vai para as APIs e carrega `scope`; o *ID token* vai para o client que pediu o login e carrega `name`, `email`, `type`. Mandar ID token para a API é o erro clássico do OIDC.

```bash
# fluxo completo: login em /login, consentir em /oauth2/authorize, trocar o code
curl -s -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:9000/userinfo

# logout: encerra a sessão E revoga as autorizações do usuário
curl -s -G --data-urlencode "id_token_hint=$ID_TOKEN" \
     --data-urlencode "post_logout_redirect_uri=http://algashop-ecommerce:9080?logout-success" \
     http://localhost:9000/connect/logout
```

> ⚠️ O logout **não** invalida um access token já emitido nas APIs: elas validam a assinatura localmente e o aceitam até o `exp`. Os 5 minutos de TTL são a janela.

Detalhes em [OpenID Connect: identidade, sessão e logout](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/openid-connect-e-sessao.md).

---

## O estado, e por que ele existe

Tokens, consentimentos e **sessões** vão para o Postgres (`JdbcOAuth2AuthorizationService`, `JdbcOAuth2AuthorizationConsentService` e `@EnableJdbcHttpSession`), com schema versionado por Flyway.

A razão não é escala: **consentimento é uma decisão do usuário**, e uma decisão que some no deploy nunca foi uma decisão. Junto vem o refresh token, que representa a sessão da pessoa — em memória, cada reinício deslogaria todo mundo.

```bash
# o banco precisa existir antes: ele entra pelo etc/postgres/init-user-db.sh do meta,
# que só roda quando o volume do Postgres está vazio
psql -h localhost -p 5433 -U postgres -c "CREATE DATABASE authorization_server;"
```

> As migrations são **cópias fiéis** do schema da biblioteca — a aplicação as lê por `RowMapper`, e mudar uma coluna quebra em runtime. E migration aplicada não se edita, nem para acrescentar comentário: o checksum muda e o Flyway recusa subir.

Detalhes do fluxo, do consentimento e da rotação em [Authorization code e consentimento](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/authorization-code-e-consentimento.md).

---

## Pendências conhecidas

- **PKCE desligado** no client web, apesar de o OAuth 2.1 exigi-lo.
- **Tokens em texto puro no banco** — quem lê a tabela se passa por qualquer usuário.
- **`logging.level.org.springframework.security: TRACE`** registra credenciais e tokens.
- **Um único usuário, em memória, com senha no YAML** — placeholder até existir um `UserDetailsService`.
- **Segredos `{noop}`** num arquivo versionado, e **clientes em memória**.
- **`docker-env` e `production-env` vazios** — sem datasource, o servidor nem sobe nesses perfis.
- **Chave de assinatura não persistida** — cada reinício invalida todo JWT emitido.
- **Não há tela de revogação de consentimento** — só apagando a linha no banco.
- **`@EnableJpaAuditing` não está ligado** — a classe base promete auditoria e nada preenche os campos; há um NPE latente para usuário criado pela aplicação.
- **O logout é global por usuário**, revogando autorizações de todos os clients.
- **Não há cadastro nem troca de senha** pela aplicação — o `AuthUser` é anêmico porque ainda não há operação sobre ele.

---

## Documentação

- [Identidade e fundamentos do OAuth 2](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/fundamentos-identidade-oauth2.md) — senha × certificado × token, os quatro papéis, grants e escopo
- [Authorization code e consentimento](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/authorization-code-e-consentimento.md) — o fluxo com pessoa, consentimento e refresh
- [OpenID Connect: identidade, sessão e logout](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/openid-connect-e-sessao.md) — ID token, usuários no banco, `/userinfo` e logout
- [Authorization Server](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/authorization-server.md) — a configuração deste serviço, opaco × JWT e quem guarda as chaves
- [Arquitetura](https://github.com/gabriel-lima258/algashop-docs/blob/main/00-visao-geral/arquitetura.md) — onde este serviço entra no mapa

O caderno completo está em [`algashop-docs`](https://github.com/gabriel-lima258/algashop-docs).
