# algashop-authorization-server

Quem **emite** as credenciais do AlgaShop — e, desde a Fase 25, quem administra os usuários que elas identificam. Começou sem domínio de negócio nenhum; hoje tem agregado, API e o banco que tokens, consentimentos e sessões exigem.

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

## Os cinco clientes

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

### Sem segredo — `algashop-admin-web` (Fase 26)

O primeiro cliente **público** do projeto: uma SPA de administração, que roda inteira no navegador.

| | |
|---|---|
| Grant | `authorization_code` — **e só ele** |
| Autenticação do cliente | **`none`** — não tem segredo para apresentar |
| PKCE | **obrigatório** (`require-proof-key: true`) |
| Consentimento | não (cliente próprio, não de terceiro) |
| Refresh token | **não existe** |
| TTLs | access 5m · código **2m** |

Um segredo embutido em bundle JavaScript está a um *view-source* de distância — não é segredo, é string. No lugar dele, **PKCE**: o cliente sorteia um `code_verifier` por requisição, manda `SHA256(verifier)` no `/authorize` e o verifier original no `/token`. O servidor recalcula e compara.

```bash
# challenge e verifier
V=$(openssl rand -base64 32 | tr '+/' '-_' | tr -d '=')
C=$(printf %s "$V" | openssl dgst -binary -sha256 | openssl base64 | tr '+/' '-_' | tr -d '=')

# ...login no navegador, /oauth2/authorize com code_challenge=$C ...

# a troca NAO leva client_secret nenhum
curl -s -X POST http://auth.algashop.local:9000/oauth2/token \
  -d grant_type=authorization_code -d client_id=algashop-admin-web \
  -d code=$CODE -d redirect_uri=http://admin.algashop.local:4200 \
  -d code_verifier=$V
```

Sem `refresh_token`, a renovação é **silent refresh**: um iframe escondido chama `/oauth2/authorize?prompt=none`, o cookie de sessão vai junto, e o servidor devolve um código novo sem mostrar tela.

> ⚠️ Sem sessão, `prompt=none` **redireciona para `/login`** em vez de devolver `login_required` — o `/oauth2/authorize` exige autenticação na filter chain e o endpoint nunca é alcançado. Dentro do iframe, a SPA fica em silêncio. Registrado como pendência.

Detalhes em [PKCE e clientes públicos](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/pkce-e-clientes-publicos.md).

---

## As telas (Fase 28)

O servidor deixou de usar a tela padrão do Spring Security. Três páginas próprias em **Thymeleaf**, com CSS e logo do projeto:

| Rota | Página | Pública? |
|---|---|---|
| `GET /login` | formulário de login | ✅ |
| `GET /logout` | confirmação (o `POST` é quem encerra) | exige sessão |
| `GET /oauth2/consent` | consentimento, com escopo em linguagem de gente | exige sessão |
| `GET /` | redireciona para a loja | exige sessão |
| `/css/**`, `/img/**`, `/favicon.ico` | estáticos | ✅ |

```gradle
implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
```

O que sustenta o login são quatro strings que **nenhum compilador verifica**: os campos `username` e `password`, o destino `POST /login`, e o `_csrf` — este último não escrito por nós, e sim injetado pelo Thymeleaf em formulários com `th:action`. Trocar por `action` devolve 403 em todo login. Há uma suíte (`LoginPageIT`) que lê o HTML servido justamente para travar isso.

> ⚠️ **A recuperação de senha ainda não existe.** `forgot-password.html`, `password-form.html` e `password-message.html` estão versionados sem rota, e `/forgot-password` — que está liberado no filter chain e linkado na tela de login — responde **404**. Implementação prevista para a próxima fase.

Detalhes em [Telas e formulários de login](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/telas-e-formularios-de-login.md).

---

## Controle de acesso por papel (RBAC — Fase 27)

O `AuthUserType` deixou de ser informativo: ele viaja como claim `role` no access token e vira `ROLE_*` nos quatro serviços. Duas tabelas decidem o que cada papel alcança:

| Tabela | Responde |
|---|---|
| `auth_user_type_client_allowed` | *este papel pode **abrir** este client?* |
| `auth_user_type_client_scope` | *abrindo, quais escopos pode **levar**?* |

As duas são consultadas **no `/oauth2/authorize`**, antes de qualquer código ser emitido — quem não pode não recebe token, e token assinado não se revoga.

```
CUSTOMER  -> admin-web      : access_denied   "not allowed to authorize this client"
MANAGER   -> ecommerce-web  : access_denied
OPERATOR  pedindo users:write no admin-web : invalid_scope [users:write]
MANAGER   pedindo o mesmo                  : code emitido
```

E a matriz de negócio sobre usuários:

| | MANAGER | OPERATOR | CUSTOMER | máquina |
|---|---|---|---|---|
| Criar MANAGER/OPERATOR | ✅ | ❌ | ❌ | ❌ |
| Criar CUSTOMER | ❌ | ❌ | ❌ | ✅ |
| Editar o próprio registro | ✅ | ✅ | ✅ | ❌ |
| Editar outro | ✅ *(≠ CUSTOMER)* | ❌ | ❌ | ❌ |
| Promover/rebaixar | ✅ *(MANAGER ↔ OPERATOR)* | ❌ | ❌ | ❌ |

> ⚠️ **As duas tabelas se preenchem em conjunto.** Um papel listado em `allowed` sem nenhuma linha em `scope` autentica e recebe `invalid_scope` em tudo — "entra e não faz nada". Aconteceu com o `CUSTOMER` na loja.

Fluxo completo em [RBAC e controle de acesso](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/rbac-e-controle-de-acesso.md).

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

### API de usuários (Fase 25)

Endpoints que **não são de protocolo**: domínio próprio deste serviço, protegido como qualquer resource server.

| Endpoint | Escopo | Para quê |
|---|---|---|
| `GET /api/v1/users` | `users:read` | listar — filtro por nome, e-mail (parcial, case-insensitive) e tipo, paginado |
| `GET /api/v1/users/{id}` | `users:read` | um usuário |
| `POST /api/v1/users` | `users:write` | cadastrar |
| `PUT /api/v1/users/{id}` | `users:write` | atualizar nome, tipo e `enabled` |
| `DELETE /api/v1/users/{id}` | `users:write` | **anonimizar** — a linha permanece |
| `GET /api/v1/users/me` | *(exige ser pessoa)* | o próprio perfil, resolvido pelo `sub` do token |

O `/me` é o único que não pede escopo: ele exige que o token represente **uma pessoa**. Token de `client_credentials` leva **403** — não por falta de permissão, mas por ausência de sujeito.

```bash
curl -s -H "Authorization: Bearer $USER_TOKEN" http://localhost:9000/api/v1/users/me
# {"id":"019d7764-…","name":"Victoria Garcia","email":"victoria.garcia@algashop.com","type":"MANAGER","enabled":true}

curl -s -G -H "Authorization: Bearer $TOKEN" http://localhost:9000/api/v1/users \
  --data-urlencode "email=ALGASHOP.COM" -d type=OPERATOR -d size=1 -d page=0
```

O `DELETE` não apaga: troca nome e e-mail por valores neutros e desliga a conta. A linha precisa sobreviver porque o id dela pode estar gravado como autor em qualquer registro auditado de qualquer serviço.

> ⚠️ **A senha temporária do cadastro não é entregue.** `POST /users` gera 12 caracteres, imprime no **stdout** e grava só o hash — não há e-mail nem retorno no corpo. O usuário criado pela API **não consegue logar**.

Contrato completo em [`openapi/authorization-server.yml`](https://github.com/gabriel-lima258/algashop-docs/blob/main/openapi/authorization-server.yml).

---

## Como rodar

Direto, com o Postgres do compose de pé:

```bash
./gradlew bootRun
```

Ou dentro do compose (o serviço entrou no `docker-compose.services.yml`):

```bash
./gradlew bootJar
docker build -t algashop/authorization-server:dev .
docker compose up -d          # na raiz do meta
```

> O issuer é **`http://auth.algashop.local:9000`**, e ele viaja dentro do claim `iss` de todo token. Dentro da rede do compose quem responde por esse nome é o `hostname:` do container; **na sua máquina** ele precisa estar no `/etc/hosts` — junto com `algashop.local` e `admin.algashop.local`, que a lista de `etc/hostnames/hostnames` traz. Sem eles, o navegador não abre `/login` e nada que rode por `bootRun` valida token.
>
> Os três ficam sob o mesmo domínio-pai de propósito: o cookie de sessão sai com `Domain=algashop.local`, e é isso que permite ao iframe do silent refresh chegar autenticado.

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

- **PKCE desligado no client confidencial** (`algashop-ecommerce-web`) — o `algashop-admin-web` já o usa; o OAuth 2.1 exige dos dois.
- **`prompt=none` sem sessão vai para `/login`** em vez de `login_required`, e o iframe do silent refresh fica esperando em silêncio.
- **`secure: false` no cookie de sessão** e `http://` nas redirect URIs — aceitável só em desenvolvimento.
- **Tokens em texto puro no banco** — quem lê a tabela se passa por qualquer usuário.
- **`logging.level.org.springframework.security: TRACE`** registra credenciais e tokens.
- **Senha temporária vaza e não chega a ninguém** — `System.out.println` no cadastro, e nenhum canal de entrega. Sem isso, o usuário criado pela API não loga.
- **Segredos `{noop}`** num arquivo versionado, e **clientes em memória**.
- **`production-env` vazio** — sem datasource e sem clientes (o grupo `production` não herda `development-env`), o servidor não sobe nesse perfil. O `docker-env` deixou de ser vazio: o serviço entrou no `docker-compose.services.yml` e aponta para `algashop-postgres:5432`.
- **Chave de assinatura não persistida** — cada reinício invalida todo JWT emitido.
- **Não há tela de revogação de consentimento** — só apagando a linha no banco.
- **O logout é global por usuário**, revogando autorizações de todos os clients.
- **Não há troca de senha** pela aplicação (o cadastro e a anonimização chegaram na Fase 25).
- **"Máquina ou pessoa?" é heurística** — deduzido comparando `aud` e `sub`, não afirmado por um claim.
- **Sem `AuthorizationMatrixTest`** — os outros três serviços têm; aqui o `@WebMvcTest` arrastaria a filter chain do protocolo inteira.

---

## Documentação

- [Identidade e fundamentos do OAuth 2](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/fundamentos-identidade-oauth2.md) — senha × certificado × token, os quatro papéis, grants e escopo
- [Authorization code e consentimento](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/authorization-code-e-consentimento.md) — o fluxo com pessoa, consentimento e refresh
- [OpenID Connect: identidade, sessão e logout](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/openid-connect-e-sessao.md) — ID token, usuários no banco, `/userinfo` e logout
- [Telas e formulários de login](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/telas-e-formularios-de-login.md) — o contrato invisível entre o HTML e o filtro, e a tela de consentimento própria
- [RBAC e controle de acesso](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/rbac-e-controle-de-acesso.md) — papel no token, política de client e escopo, e o fluxo guiado das quatro camadas
- [Gestão de usuários e auditoria](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/gestao-de-usuarios-e-auditoria.md) — a API de usuários, `/me`, token de pessoa × de máquina e a auditoria com autor real
- [Authorization Server](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/authorization-server.md) — a configuração deste serviço, opaco × JWT e quem guarda as chaves
- [Arquitetura](https://github.com/gabriel-lima258/algashop-docs/blob/main/00-visao-geral/arquitetura.md) — onde este serviço entra no mapa

O caderno completo está em [`algashop-docs`](https://github.com/gabriel-lima258/algashop-docs).
