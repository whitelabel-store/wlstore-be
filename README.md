# WLStore: e-commerce whitelabel em microsserviços

Plataforma de e-commerce **whitelabel** e **multi-tenant**: uma única instalação atende várias lojas (tenants), cada uma com seu catálogo, preços, estoque e pedidos isolados.

Projeto de estudo aplicado a um produto real.

Este repositório contém só o **backend**. O frontend vive em repositórios
próprios, cada um com build e deploy independentes:

| Repositório | Conteúdo |
| --- | --- |
| [wlstore-vitrine](https://github.com/whitelabel-store/wlstore-vitrine) | Loja (`/`): home, busca, categorias, vitrines |
| [wlstore-painel](https://github.com/whitelabel-store/wlstore-painel) | Painel do lojista (`/painel`) |
| [wlstore-platform-fe](https://github.com/whitelabel-store/wlstore-platform-fe) | Pacotes `@whitelabel-store/ui` e `@whitelabel-store/http` |

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem / runtime | Java 21 (virtual threads) |
| Framework | Spring Boot 4.1.1 (Spring 7, Jackson 3 em `tools.jackson.*`) |
| Persistência | PostgreSQL 16, Spring Data JPA, Hibernate 7, Flyway |
| Mensageria | RabbitMQ 3.13 (Spring AMQP) |
| Descoberta de serviços | Spring Cloud Netflix Eureka |
| Mapeamento / boilerplate | MapStruct, Lombok |
| Testes | JUnit 6, Mockito, AssertJ, Testcontainers 2.0.5 |
| Documentação da API | springdoc-openapi (Swagger UI) |

## Arquitetura

```
                 ┌────────────────────┐
  cliente ─────► │  API Gateway (TBD) │
                 └─────────┬──────────┘
        ┌──────────┬───────┴────┬───────────┬───────────┐
        ▼          ▼            ▼           ▼           ▼
   catalog     pricing      inventory     order      identity
   service     service      service      service     service
      │ (outbox)   ▲            ▲           │
      └────► RabbitMQ  catalog.events ──────┘
              (topic)   product.created | updated | deleted

   discovery-server (Eureka)  ·  wlstore-common (módulo compartilhado)
```

Princípios:

- **Um bounded context por serviço, um banco por serviço.** O catálogo não conhece preço nem estoque.
- **Comunicação assíncrona por eventos.** O catálogo publica, e os demais serviços consomem e mantêm seus próprios dados.
- **Multi-tenancy por schema compartilhado**, com coluna `tenant_id` e `@TenantId` do Hibernate.
- **Consistência eventual**, com entrega *at-least-once* e consumidores idempotentes.

## Módulos

| Módulo | Porta | Banco | Estado |
|---|---|---|---|
| `wlstore-common` | n/a | n/a | Pronto |
| `catalog-service` | 8081 | catalogdb (5532) | Pronto (CRUD, multi-tenant, publica eventos) |
| `discovery-server` | 8761 | n/a | Esqueleto (Eureka Server) |
| `pricing-service` | a definir | pricingdb | Esqueleto, **próximo** |
| `inventory-service` | a definir | inventorydb (5732) | Esqueleto |
| `order-service` | a definir | orderdb (5632) | Esqueleto |
| `payment-service` | a definir | a definir | Esqueleto |
| `notification-service` | a definir | a definir | Esqueleto (AMQP + mail) |
| `identity-service` | a definir | a definir | Esqueleto (resource server) |
| `api-gateway` | a definir | n/a | Não criado |
| `config-server` | a definir | n/a | Não criado |

> "Esqueleto" significa projeto Spring gerado com dependências e `application.yaml` básico, sem regra de negócio.

## O que já está feito

### `wlstore-common` (módulo compartilhado)
- Auto-configuration via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`: qualquer serviço que adicionar a dependência ganha multi-tenancy, JPA e tratamento de erros.
- **Tenant:** filtro `X-Tenant-Id`, resolvedor de tenant do Hibernate e `DEFAULT_TENANT` (`00000000-0000-0000-0000-000000000000`) para contextos sem requisição (testes, jobs).
- `ProblemDetail` (RFC 9457) para erros, `ResourceNotFoundException`, `PageResponseDTO`.

### `catalog-service`
- **Modelo:** categorias em árvore (*materialized path*), produtos, variantes (SKU) e imagens.
- **Regras:** sincronização de variantes por SKU no update, unicidade de categoria raiz por tenant, versionamento otimista (`version`).
- **Banco:** Flyway `V1` (schema), `V2` (índices e unicidade da raiz), `V3` (`tb_outbox_event`). `ddl-auto: validate`, `open-in-view: false`. Migrations aplicadas nunca são editadas.
- **API:** CRUD de categorias e produtos, paginação, validação, Swagger.
- **Eventos (transactional outbox):**
    1. `ProductEventRecorder` grava o evento **na mesma transação** do produto (`Propagation.MANDATORY`), evitando *dual write*.
    2. Payload *snapshot*: `productId`, `tenantId`, `status`, `variants[sku, status]`. Quem consome detecta SKU removido comparando com o que já conhece.
    3. `OutboxRelayer` (`@Scheduled`, 1s) lê pendentes com `FOR UPDATE SKIP LOCKED` (seguro com várias instâncias) e **para na primeira falha** para preservar a ordem por produto.
    4. `OutboxEventPublisher` publica no exchange `catalog.events` (topic) com *publisher confirms* (`publisher-confirm-type: correlated`). Só devolve sucesso depois do ack do broker.
    5. Falha do broker deixa o evento pendente, e ele é reenviado depois. Isso pode gerar duplicata, então o consumidor precisa ser idempotente. O `messageId` da mensagem é o id do evento.
- `OutboxEvent` **não** usa `@TenantId` de propósito. O relayer não tem requisição, e com o filtro ele só enxergaria eventos do tenant padrão. O tenant viaja na coluna e no payload.

### Infraestrutura local
- `docker-compose.yaml` na raiz: um Postgres por serviço e o RabbitMQ, todos lendo o `.env` (veja [Variáveis de ambiente](#variáveis-de-ambiente)).

### Testes (17 verdes no catalog-service)
- Unitários: `ProductServiceImplTest`, `ProductSnapshotTest`, `ProductEventRecorderTest`, `OutboxRelayerTest`, `OutboxEventPublisherTest`.
- Integração com Testcontainers (Postgres e RabbitMQ reais, via `@ServiceConnection`): repositório de categorias, aplicação das migrations, `OutboxEventRepositoryTest` (SKIP LOCKED), `ProductOutboxIntegrationTest` e `OutboxRelayIntegrationTest` (produto criado → mensagem na fila → linha marcada como publicada).
- Rodar: `mvn -pl catalog-service -am test`. Os testes não usam o RabbitMQ do compose.

## Lições registradas
- `@TenantId` é preenchido no **flush**, então `saveAndFlush` onde o tenant é necessário antes do commit.
- Mensagem publicada em exchange sem fila ligada é **descartada sem erro**. O consumidor declara e liga a própria fila.
- O exchange só é declarado na primeira conexão com o broker.
- Sem `publisher-confirm-type: correlated`, o `CorrelationData` nunca completa e todo publish dá timeout.
- Cuidado com imports no IntelliJ: `jakarta.transaction.Transactional` vs `org.springframework...Transactional`, e `com.fasterxml` vs `tools.jackson`.

## O que falta (ordem sugerida)

### 1. Consumidores idempotentes
- **pricing-service**
    - Preço em **centavos** (`bigint`), moeda BRL.
    - Histórico por período: `tb_price(sku, amount_cents, valid_from, valid_to, tenant_id)`. Definir novo preço fecha a linha anterior, e só uma fica vigente por SKU (índice parcial).
    - Inbox: `tb_processed_event(message_id PK)`. O insert e o trabalho acontecem na mesma transação, e a chave primária barra duplicata mesmo com duas instâncias.
    - Fila `pricing.catalog-events` ligada a `product.*`, com DLQ. O tenant é definido explicitamente a partir do payload.
    - A chave entre serviços é o SKU.
- **inventory-service:** mesmo padrão de inbox, reservas e saldo por SKU, com concorrência tratada (lock otimista ou atualização atômica).
- Decisões em aberto: sobreposição de validade de preço, e o que fazer com SKU removido no catálogo.

### 2. Borda e configuração
- `api-gateway` (ver recomendação abaixo) e `config-server`.
- Adoção do `wlstore-common` nos outros serviços, adicionando-os aos `<modules>` do `pom.xml` raiz.
- Eureka client nos serviços e registro no `discovery-server`.

### 3. Fluxo de compra
- **order-service:** pedido com **snapshot do preço** no momento da compra, máquina de estados.
- **payment-service:** integração com gateway de pagamento, idempotência e webhooks.
- **notification-service:** e-mails por eventos (AMQP + mail).
- Saga (coreografia por eventos) para pedido → reserva de estoque → pagamento, com compensação.

### 4. Identidade
- **Keycloak** (adiado): realm por tenant ou *Organizations*, claim `tenant_id` no JWT, e o gateway passa a **derivar** o tenant do token em vez de confiar no header do cliente.
- `identity-service` e papéis (admin da loja, cliente).

### 5. Produção, por último
- Dockerfile por serviço, compose completo, CI.
- Kubernetes, Helm, infraestrutura como código.
- Observabilidade: logs estruturados, métricas, tracing distribuído.

### Pendências menores
- Trocar `Confirm.isAck()/getReason()` (depreciados) por `ack()/reason()`.
- Ajustar o intervalo do relayer por ambiente (`application-<perfil>.yaml`).
- Trocar `@Valid` em `List` por `List<@Valid ...>` (aviso HV000271).
- Teste de SKIP LOCKED com duas transações concorrentes; teste MockMvc do `TenantFilter`.
- Substituir os `https://example.com/...` dos tipos de `ProblemDetail`; slug de produto; filtro por preço (depende do pricing).

## Como rodar localmente

```bash
cp .env.example .env                 # uma vez; ajuste se quiser outras portas/senhas
docker compose up -d                 # catalog-db, pricing-db, rabbitmq (lendo o .env)
mvn -pl catalog-service -am test     # testes (Testcontainers, precisam do Docker; não usam o .env)
mvn -pl catalog-service -am spring-boot:run
mvn -pl pricing-service -am spring-boot:run
```

Sem flag nenhuma, os serviços sobem no perfil `local` e leem o mesmo `.env`
do compose. Pela IDE é igual (rodando da raiz ou do módulo).

Swagger: catálogo em `http://localhost:8081/swagger-ui.html`, pricing em
`http://localhost:8082/swagger-ui.html`. Painel do RabbitMQ:
`http://localhost:15672`. Toda requisição precisa do header `X-Tenant-Id: <uuid>`.

## Dados de demonstração (seed)

Categorias e produtos chegam de jeitos diferentes:

| O quê | Como | Onde vale |
| ----- | ---- | --------- |
| árvore de categorias | migration do Flyway (`V6__seed_categories.sql`) | todo banco novo, sozinho |
| produtos de demonstração | `scripts/seed-demo.sh`, pela API, a partir de `seed/demo-catalog.json` | quando você roda |

Produto é dado, não código: não entra no build nem na imagem. Banco novo
(local, develop, prod) começa só com as categorias.

```bash
scripts/seed-demo.sh local      # backend local rodando (8081/8082)
scripts/seed-demo.sh develop    # cluster (precisa do make hosts da infra)
scripts/seed-demo.sh prod       # pede confirmação
```

- Cadastra **pela API**: criar produto publica o evento que o pricing
  consome, como no painel. SQL direto pularia esse fluxo.
- Pode rodar de novo: produto que já existe (pelo título) é pulado; o preço é
  regravado.
- No arquivo, a categoria é o **caminho de títulos**
  (`Tecnologia > Eletrônicos, Áudio e Vídeo > Fones de Ouvido`), porque os
  ids mudam a cada banco. Árvores que não vêm da migration ficam em
  `categories` e são criadas se a raiz não existir.
- No cluster, depois de cadastrados, os produtos entram no backup do banco e
  voltam num `make destroy && make setup` da infra.

## Variáveis de ambiente

Três ambientes, um jeito de configurar cada um:

| Ambiente | Perfil Spring | De onde vêm os valores |
| -------- | ------------- | ---------------------- |
| **local** (sua máquina) | `local` (padrão: nenhum perfil escolhido) | `.env` na raiz (copiado de `.env.example`), o mesmo do `docker compose` |
| **develop** (cluster) | `develop` | Kubernetes: values do repo de infra + Secrets gerados pela plataforma |
| **prod** (cluster) | `prod` | idem; `application-prod.yaml` desliga a tela do Swagger |

Arquivos de config de cada serviço (`src/main/resources/`):

| Arquivo | Vale em | O quê |
| ------- | ------- | ----- |
| `application.yaml` | todos | o comum; conexões **só** por variável, sem valor padrão: faltou uma no cluster, o serviço não sobe (em vez de usar senha de exemplo) |
| `application-local.yaml` | local | importa o `.env`, monta as conexões com `localhost`, liga o `show-sql` |
| `application-prod.yaml` | prod | só o que muda em produção |

### Variáveis por serviço

| Serviço | Local (`.env`) | develop / prod (Kubernetes) | Origem no cluster |
| ------- | -------------- | --------------------------- | ----------------- |
| catalog | `CATALOG_DB_PORT`, `CATALOG_POSTGRES_DB`, `CATALOG_POSTGRES_USER`, `CATALOG_POSTGRES_PASSWORD` | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Secret `postgres` (banco `catalogdb`) |
| pricing | `PRICING_DB_PORT`, `PRICING_POSTGRES_DB`, `PRICING_POSTGRES_USER`, `PRICING_POSTGRES_PASSWORD` | idem | Secret `postgres` (banco `pricingdb`) |
| ambos (eventos) | `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` | `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD` | Secret `rabbitmq` |
| ambos | — | `SPRING_PROFILES_ACTIVE` (`develop`/`prod`) | values (`{{ .Values.app.environment }}`) |
| ambos | — | `LOGGING_PATTERN_LEVEL` (trace id no log), `JAVA_TOOL_OPTIONS`, `OTEL_*` | values (tracing) |
| order / inventory (em construção) | `ORDER_*`, `INVENTORY_*` (`docker compose --profile order\|inventory up -d`) | `SPRING_DATASOURCE_*` | quando entrarem no deploy |

No cluster, as senhas são geradas e seladas pela plataforma (`make seal` no
repo de infra); os values só ligam os nomes:
`SPRING_DATASOURCE_URL: "jdbc:postgresql://$(DB_HOST):$(DB_PORT)/catalogdb"`.
Nenhuma senha de develop/prod passa por este repo.

### Adicionar uma variável

1. Nome no padrão do Spring (`app.minha-config` → `APP_MINHACONFIG` no ambiente).
2. Local: valor no `.env.example` (e no seu `.env`), lido em `application-local.yaml`.
3. Config que muda por ambiente: `env:` nos values do serviço no repo de infra.
4. Segredo: `projects/wlstore/secrets/<env>.env` no repo de infra + `make seal`;
   o serviço recebe pelo Secret `app-secrets`.
5. Sem valor padrão no `application.yaml` para o que é segredo ou conexão.

### Problemas comuns

| Sintoma | Causa | Solução |
| ------- | ----- | ------- |
| `docker compose up`: `required variable ... is missing a value` | sem `.env` | `cp .env.example .env` |
| local: `password authentication failed` | o volume do Postgres foi criado com outra senha (a do `.env` só vale na primeira subida) | use a senha antiga no `.env`, ou recrie: `docker compose down -v` (apaga os dados locais) |
| no cluster: `Driver org.postgresql.Driver claims to not accept jdbcUrl, ${SPRING_DATASOURCE_URL}` | variável não chegou ao pod | conferir os values e o Secret no repo de infra (`kubectl describe pod`) |
| app local sobe com perfil errado | `SPRING_PROFILES_ACTIVE` exportado no shell | `unset SPRING_PROFILES_ACTIVE` |

## Como funciona a mensageria (RabbitMQ)

O catálogo publica eventos de produto e o pricing consome, sem um conhecer o outro. A ligação entre os dois é só o exchange `catalog.events` (topic) e a chave de roteamento `product.*` — o catálogo nem sabe que o pricing existe.

### Fluxo básico
O catálogo grava o produto e a carta (`tb_outbox`) na mesma transação, e um relayer entrega a carta ao exchange. A chave (`product.created`, `product.updated` ou `product.deleted`) casa com o binding `product.*` da fila `pricing.catalog-events`, e a carta espera lá até o pricing buscar, gravar o SKU (`tb_known_sku`) e confirmar (ack).

![Fluxo básico: catálogo publica, RabbitMQ roteia, pricing consome](assets/rabbitmq-fluxo-basico.svg)

### E se o pricing cair?
A fila é durável: as cartas ficam guardadas (`Ready`) até o serviço voltar e processá-las uma a uma, na ordem de chegada. Nada se perde.

![Pricing fora do ar: as cartas ficam guardadas na fila](assets/rabbitmq-pricing-fora-do-ar.svg)

### E se uma carta vier com defeito?
O pricing tenta 3 vezes (1s, 2s, 4s). Se continuar falhando, a carta vai para a DLQ e a fila principal segue.

![Carta com defeito: três tentativas e depois a DLQ](assets/rabbitmq-carta-com-defeito-dlq.svg)

### Idempotência e falhas
- Entrega é *at-least-once*: a mesma carta pode chegar duas vezes (retry, reconexão). O pricing registra o `messageId` em `tb_processed_event` (inbox) antes de processar; se já existe, a mensagem é descartada em silêncio.
- Mensagem que falha repetidamente após as 3 tentativas vai para a dead-letter queue `pricing.catalog-events.dlq`, para não travar a fila principal.
- SKU removido do produto (ou produto deletado) fecha o preço vigente daquele SKU — ele some de `tb_known_sku` e não pode mais receber um novo preço até reaparecer num evento futuro.

## Roadmap: busca inteligente

Planejado depois do fluxo de compra. A ideia é construir sobre a mensageria que já existe, sem acoplar os serviços:

- **search-service**: novo microsserviço com Elasticsearch. Consome `product.*` (catálogo) e `price.changed` (pricing) e mantém um índice de busca por tenant. Nada de consulta direta aos bancos dos outros serviços.
- **Busca de texto**: título, marca, descrição e categoria, com sinônimos, correção de digitação e filtros por preço e atributos (ordenar por preço já é possível no catálogo; o Elasticsearch passa a cuidar de relevância).
- **Sugestão com IA**: autocomplete e "produtos parecidos" a partir de embeddings dos produtos (busca vetorial no próprio Elasticsearch). A IA é um recurso de relevância: o índice continua sendo alimentado por eventos, e o catálogo segue como fonte da verdade.
- **Isolamento por tenant**: cada documento do índice leva o `tenantId`, e toda consulta filtra por ele, o mesmo princípio do `@TenantId` nos bancos.

O que prepara o terreno hoje: eventos de produto e de preço em estado completo (*snapshot*), consumidores idempotentes e o read model de preço no catálogo.