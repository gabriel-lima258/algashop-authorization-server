# algashop-authorization-server

Quem **emite** as credenciais do AlgaShop. O único serviço sem domínio de negócio — sem banco, sem entidade, e quase sem código.

---

## O problema

Dezenove fases construíram um sistema que não sabe quem está do outro lado. A questão não é "como cada serviço valida senha" — é o contrário disso:

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
| **Banco** | nenhum — clientes em memória |
| **Pacote raiz** | `com.algaworks.algashop.authorizationserver` |

```gradle
implementation 'org.springframework.boot:spring-boot-starter-security-oauth2-authorization-server'
```

Uma dependência e uma `@SpringBootApplication` vazia produzem **seis endpoints** funcionando. O que se configura é *quem pode pedir token e o que esse token vale* — nunca *como* o protocolo funciona.

> A porta é **9000**, não 8081. A 8081 é do `algashop-ordering`, e os dois não subiam juntos; 9000 é também a convenção do Spring Authorization Server.

---

## Os dois clientes

Declarados em `application-development-env.yaml`, e escolhidos para tornar visível a decisão mais consequente do desenho — **o formato do token**:

| | `algashop-test` | `algashop-ordering-service` |
|---|---|---|
| Para que existe | teste manual | o `ordering` lendo o catálogo |
| Grant | `client_credentials` | `client_credentials` |
| Escopos | `products:read`, `products:write` | `products:read` |
| TTL | 15 min | **5 min** |
| Formato | `reference` (**opaco**) | `self-contained` (**JWT**) |

**Token opaco** é uma referência sem conteúdo: o resource server resolve chamando `/oauth2/introspect` — uma ida à rede por requisição, e revogação imediata.

**JWT** é auto-contido: o resource server confere a assinatura localmente com a chave pública do `/oauth2/jwks` — nenhuma chamada, e **nenhuma revogação**.

> Os TTLs não são arbitrários. Com JWT, **o tempo de vida é a janela de exposição de um token vazado**, porque não há como cancelá-lo. Daí 5 minutos.

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
| `GET /oauth2/authorize` | o navegador | fluxo com usuário — **não utilizável hoje** |

O último está no contrato mas nenhum cliente tem `authorization_code`, e não há usuário cadastrado.

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

## O que isto ainda não protege

**Nada** — e vale dizer com essa clareza. Emitir token não protege recurso enquanto ninguém exigir o token, e `ordering`, `billing` e `product-catalog` não têm uma linha de resource server. A ordem está certa (não dá para verificar o que não existe), mas o ciclo está aberto.

Outras pendências conhecidas: segredos em `{noop}` num arquivo versionado; clientes em memória; `production-env` vazio, o que faz o servidor subir sem cliente nenhum; e a chave de assinatura não persistida — sem configuração, o Spring gera um par novo a cada subida e **reiniciar invalida todo JWT emitido**.

---

## Documentação

- [Identidade e fundamentos do OAuth 2](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/fundamentos-identidade-oauth2.md) — senha × certificado × token, os quatro papéis, grants e escopo
- [Authorization Server](https://github.com/gabriel-lima258/algashop-docs/blob/main/05-seguranca/authorization-server.md) — a configuração deste serviço, opaco × JWT e quem guarda as chaves
- [Arquitetura](https://github.com/gabriel-lima258/algashop-docs/blob/main/00-visao-geral/arquitetura.md) — onde este serviço entra no mapa

O caderno completo está em [`algashop-docs`](https://github.com/gabriel-lima258/algashop-docs).
