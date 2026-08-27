# Gerenciador de Notificações de Hospital

Aplicação web Java para cadastro e listagem de notificações hospitalares. Interface construída com ZK Framework (renderização server-side, sem JavaScript/HTML manual), persistência em DB2, empacotada como WAR e executada em Tomcat via Docker.

## Stack

- **Linguagem/Build**: Java 11, Maven (`pom.xml`), empacotamento `war` (finalName `ROOT`)
- **UI**: [ZK Framework](https://www.zkoss.org/) 10.3.0.1 (`zul` + `zkbind`) — componentes definidos em arquivos `.zul` (XML), com um `Composer` Java controlando eventos
- **Banco de dados**: IBM DB2 (driver `com.ibm.db2.jcc:jcc:12.1.4.0`), acesso via JDBC puro (sem ORM, sem framework de persistência)
- **Servidor de aplicação**: Tomcat 9 (JDK 11), conforme `Dockerfile`
- **Orquestração local**: Docker Compose (`docker-compose.yml`) sobe a aplicação + um container DB2 (`icr.io/db2_community/db2:11.5.9.0`)
- **Servlet API**: javax.servlet 4.0.1 (Java EE, não Jakarta EE — atenção nesse detalhe se houver migração futura)

## Arquitetura

Camadas simples, sem separação em pacotes por responsabilidade (tudo em `br.com.hospital.notificacao`):

```
src/main/webapp/index.zul              → View (ZK), página única, welcome-file
src/main/webapp/WEB-INF/web.xml        → Configuração de servlets/listeners do ZK e da app

src/main/java/br/com/hospital/notificacao/
├── AppInitListener.java               → ServletContextListener: cria a tabela no start da app
├── NotificacaoController.java         → Composer ZK: liga a UI (.zul) aos eventos e ao DAO
├── NotificacaoDAO.java                → Acesso a dados: CREATE TABLE, INSERT, SELECT via JDBC puro
├── Notificacao.java                   → POJO/model (id, mensagem, dataCriacao)
└── ConexaoDB2.java                    → Fábrica de Connection JDBC, lê configuração de variáveis de ambiente
```

### Fluxo de execução

1. **Startup**: Tomcat carrega o WAR → `web.xml` registra `AppInitListener` → `contextInitialized` chama `NotificacaoDAO.criarTabelaSeNaoExiste()`, que executa `CREATE TABLE notificacao (...)` e ignora o erro se a tabela já existir (SQLSTATE `42710`).
2. **Acesso à página**: `index.zul` é o welcome-file, servido pelo `DHtmlLayoutServlet` do ZK (mapeado para `*.zul`).
3. **Composer**: `NotificacaoController` (`apply="..."` no `<window>` do zul) é instanciado, faz `@Wire` dos componentes (`txtMensagem`, `listNotificacoes`) e, em `doAfterCompose`, chama `carregarNotificacoes()` para popular a listbox.
4. **Interação**: clique em `btnAdicionar` (`@Listen("onClick = #btnAdicionar")`) → lê o texto da textbox → `dao.inserir(mensagem)` → limpa o campo → recarrega a lista.
5. **Persistência**: cada operação do DAO abre uma nova `Connection` via `ConexaoDB2.obterConexao()` (try-with-resources, sem pool de conexões) e fecha ao final.

### Modelo de dados

Tabela única `notificacao` (criada automaticamente no startup, não há migrations/Flyway/Liquibase):

| Coluna        | Tipo                          | Observação                          |
|---------------|--------------------------------|--------------------------------------|
| id            | INTEGER GENERATED ALWAYS AS IDENTITY | PK |
| mensagem      | VARCHAR(255) NOT NULL          |                                       |
| data_criacao  | TIMESTAMP NOT NULL DEFAULT CURRENT TIMESTAMP | preenchido pelo banco |

Listagem sempre ordenada por `id DESC` (mais recentes primeiro).

### Schema SQL (DDL)

Executado automaticamente por `NotificacaoDAO.criarTabelaSeNaoExiste()` no startup da aplicação (não há arquivo `.sql` separado nem ferramenta de migration — o DDL vive embutido no código Java):

```sql
CREATE TABLE notificacao (
    id           INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    mensagem     VARCHAR(255) NOT NULL,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT TIMESTAMP
);
```

Queries usadas pela aplicação (todas em `NotificacaoDAO`):

```sql
-- inserir (PreparedStatement)
INSERT INTO notificacao (mensagem) VALUES (?);

-- listar
SELECT id, mensagem, data_criacao
FROM notificacao
ORDER BY id DESC;
```

## Configuração / variáveis de ambiente

`ConexaoDB2` lê (com fallback padrão exceto para a senha, que é obrigatória):

| Variável       | Padrão       | Obrigatória |
|----------------|--------------|-------------|
| DB2_HOST       | localhost    | não |
| DB2_PORT       | 50000        | não |
| DB2_DATABASE   | HOSPITAL     | não |
| DB2_USER       | db2inst1     | não |
| DB2_PASSWORD   | —            | **sim** (lança `IllegalStateException` se ausente) |

No Docker Compose, além dessas, o container DB2 usa `DB2INST1_PASSWORD` para criar o usuário/instância. Copiar `.env.example` para `.env` antes de rodar (`.env` não é versionado).

## Como rodar

```bash
cp .env.example .env   # ajustar senhas se necessário
docker compose up --build
```

App exposta em `http://localhost:8080` (porta 8080), DB2 exposto em `localhost:50000`. O healthcheck do DB2 (`db2 connect to HOSPITAL`) garante que a app só sobe depois que o banco estiver pronto (`start_period: 5m`, pois a criação da instância DB2 é lenta).

## Pontos de atenção / limitações conhecidas

- Sem testes automatizados no repositório.
- Sem pool de conexões (cada operação abre/fecha uma `Connection` nova) — não ideal para produção.
- Sem camada de serviço/validação além da checagem de string vazia no controller.
- Sem autenticação/autorização — página única, sem controle de acesso.
- `javax.servlet-api` (não `jakarta.servlet`) — projeto ainda em Java EE, relevante para qualquer upgrade de Tomcat/Spring futuro.
- Há uma pasta `graphify-out/` na raiz com um grafo de conhecimento já gerado sobre este repositório (cache/AST, `graph.json`, `graph.html`) — útil para navegação assistida por IA, não faz parte do build da aplicação.
