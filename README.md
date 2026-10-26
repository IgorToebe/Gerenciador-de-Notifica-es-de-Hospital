# Sistema de Gestão de Notificações de Segurança do Paciente — NSP HU/UEM

Substitui o processo manual do Hospital Universitário (QR Code → Google Forms → planilhas
Excel) por um fluxo único: **QR Code → notificação → triagem → investigação → plano de
ação → conclusão**, sem reentrada de dados, em conformidade com a LGPD, a Lei 13.608/2018
(sigilo do denunciante), a ANVISA e o programa Hospital Sentinela.

Este documento cobre o sistema completo. Para o histórico curto de arquitetura original
(anterior à V1), ver [GEMINI.md](GEMINI.md). Para o que ficou fora do escopo desta versão
e como implementar depois, ver [ROADMAP-V2.md](ROADMAP-V2.md).

## Stack

- **Linguagem/Build**: Java 11, Maven (`pom.xml`), empacotamento `war` (finalName `ROOT`)
- **UI**: [ZK Framework](https://www.zkoss.org/) 10.3.0.1 — `.zul`/`.zhtml` (XML) + `Composer` Java (padrão MVC, `SelectorComposer`/`@Wire`/`@Listen`)
- **Banco de dados**: IBM DB2 (driver `com.ibm.db2.jcc:jcc`), JDBC puro (sem ORM) com pool **HikariCP**
- **QR Codes**: [ZXing](https://github.com/zxing/zxing) (`core` + `javase`), gerados sob demanda
- **Servidor de aplicação**: Tomcat 9 (JDK 11)
- **Orquestração local**: Docker Compose — app + DB2 (`icr.io/db2_community/db2:11.5.9.0`) + volume nomeado para anexos
- **Testes**: JUnit 5, para as regras de negócio puras (matriz de risco, protocolo, hash de senha)

## Os 5 canais de notificação (QR Codes)

Cada canal tem sua própria identidade de cor — pedido explícito do cliente para que o
usuário reconheça de forma intuitiva qual tipo de notificação está preenchendo. As cores
são aplicadas via classes CSS (`.t-tecno`, `.t-farmaco`, ...) definidas em
[css/nsp.css](src/main/webapp/css/nsp.css), que redefinem variáveis (`--c`, `--soft`, ...)
herdadas por toda a folha de estilo.

| Canal | Público | Código do formulário | Prefixo do protocolo | Cor |
|---|---|---|---|---|
| Tecnovigilância | Profissionais | FOR-NSP-005 | TC | Azul `#2f80d6` |
| Farmacovigilância | Profissionais | FOR-NSP-002 | FV | Laranja `#dd6f1e` |
| Near Miss Materno | Profissionais | FOR-NSP-004 | NM | Rosa `#d6336c` |
| Problemas Assistenciais | Profissionais | FOR-NSP-001 | PA | Verde `#4e9c3e` |
| Notificação do Paciente | Pacientes/acompanhantes | Ouvidoria | NP | Dourado `#b07a1c` |

Os QR Codes ficam disponíveis para impressão em **Admin → QR Codes**
(`/admin/qrcodes.zul`), cada um apontando para a URL curta `/n/{tipo}` (ex.: `/n/tecno`),
que redireciona para o wizard completo (`/notificar.zul?tipo=tecno`). Definidos em
[TipoNotificacao.java](src/main/java/br/com/hospital/notificacao/model/TipoNotificacao.java).

## Arquitetura

```
config/   DataSourceFactory (pool HikariCP), SchemaInitializer (DDL idempotente),
          SeedData (usuários iniciais), AppInitListener (startup do WAR)
model/    Notificacao, Investigacao, AcaoPlano, Usuario, TipoNotificacao (+ enums)
dao/      Acesso a dados via JDBC puro (um DAO por tabela)
service/  Regras de negócio: NotificacaoService, TriagemService, InvestigacaoService,
          PlanoAcaoService, AutenticacaoService, AnexoService, MatrizRisco, HashSenha,
          ProtocoloService
web/      Composers ZK (um por tela) + servlets auxiliares (QR Code, download de anexo)
```

### Fluxo de uma notificação

```
Aberto → Em triagem → Em investigação → Plano em execução → Concluído
```

1. **Abertura** ([WizardComposer](src/main/java/br/com/hospital/notificacao/web/WizardComposer.java)): wizard de 4 passos (sigilo/LGPD → dados do incidente → descrição/anexos → contato opcional). No passo 2 o notificante escolhe o **problema (subgrupo)** do tipo, obrigatório. Na Tecnovigilância o **Registro ANVISA é obrigatório**. Gera protocolo (`#TC-2026-004821`) e senha de acompanhamento, exibidos uma única vez.
2. **Triagem** ([TriagemComposer](src/main/java/br/com/hospital/notificacao/web/TriagemComposer.java)): resumo com link para os detalhes completos, painel de reclassificação, matriz de risco Probabilidade × Impacto (5×5) e detecção de duplicatas por prontuário. "Iniciar investigação" exige problema, classificação e risco definidos.
3. **Investigação** ([InvestigacaoComposer](src/main/java/br/com/hospital/notificacao/web/InvestigacaoComposer.java)): FOR-NSP-014. Começa pela seção **0 · Classificação do incidente** (problema, classificação e risco, editáveis), seguida da timeline do incidente, dos 6 fatores contribuintes (Protocolo de Londres) e dos 5 Porquês. A causa raiz (5º porquê) é **obrigatória** para gerar o plano de ação.
4. **Plano de ação** ([PlanoComposer](src/main/java/br/com/hospital/notificacao/web/PlanoComposer.java)): 5W2H editável (SGQ-001). A notificação só pode ser concluída quando todas as ações estiverem Concluídas ou Canceladas.

### Problema, classificação e risco

Toda notificação é descrita em três eixos, que aparecem como colunas na lista, servem de filtro e entram no PDF:

| Eixo | Campo | Valores |
|---|---|---|
| **Problema** | `SUBGRUPO` | Lista própria de cada tipo, definida em [TipoNotificacao](src/main/java/br/com/hospital/notificacao/model/TipoNotificacao.java) (`getSubgrupos()`). Ex.: Tecnovigilância → Equipamento médico, Material médico-hospitalar…; Farmacovigilância → Reação adversa, Erro de medicação… |
| **Classificação do incidente** | `GRAVIDADE` | Circunstância de risco … Óbito ([ClassificacaoIncidente](src/main/java/br/com/hospital/notificacao/model/ClassificacaoIncidente.java), fonte única para o wizard, a triagem e os filtros) |
| **Risco** | `NIVEL_RISCO` / `SCORE_RISCO` | Matriz Probabilidade × Impacto ([MatrizRisco](src/main/java/br/com/hospital/notificacao/service/MatrizRisco.java)) |

Para mudar as listas de subgrupos, basta editar os construtores do enum `TipoNotificacao`.

### Reclassificação

O [ReclassificacaoPainel](src/main/java/br/com/hospital/notificacao/web/ReclassificacaoPainel.java) é um painel único, reaproveitado na Triagem, na Investigação e na tela de Detalhes. Nele o NSP pode alterar:

- o **tipo/canal**, para corrigir uma notificação aberta pelo QR Code errado (o protocolo original é mantido);
- o problema, a classificação e o risco;
- a **descrição revisada**. A descrição original do notificante (`DESCRICAO`) nunca é sobrescrita: a revisão fica em `DESCRICAO_REVISADA`, e as duas aparecem lado a lado.

Cada reclassificação grava em `NOTIFICACAO_HISTORICO` o que mudou (ex.: "Tipo Tecnovigilância → Farmacovigilância; Problema …"). Isso é feito por [ReclassificacaoService](src/main/java/br/com/hospital/notificacao/service/ReclassificacaoService.java).

### Lista, filtros e detalhes

- **Notificações** ([ListaComposer](src/main/java/br/com/hospital/notificacao/web/ListaComposer.java)) tem chips de status, busca livre e **Filtros avançados**: tipo → problema, classificação, risco, setor, **paciente (prontuário)**, período de recebimento e ordenação (mais recentes, maior risco, problema, classificação mais grave). Todos os filtros são resolvidos em SQL (`NotificacaoDAO.montarWhere`).
- **Detalhes** (`/admin/detalhe.zul?id=`, [DetalheComposer](src/main/java/br/com/hospital/notificacao/web/DetalheComposer.java)) mostra tudo o que foi registrado: classificação, dados do incidente e campos do tipo, descrição original e revisada, anexos, investigação, plano de ação e histórico. Dados de contato do notificante nunca são exibidos.

### Exportação em PDF

O [ExportacaoServlet](src/main/java/br/com/hospital/notificacao/web/ExportacaoServlet.java) exige sessão administrativa e gera documentos com
[openhtmltopdf](https://github.com/danfickle/openhtmltopdf) ([RelatorioNotificacaoService](src/main/java/br/com/hospital/notificacao/service/RelatorioNotificacaoService.java)):

- `/export/notificacao/{id}.pdf`: uma notificação, a partir do botão "Exportar PDF" em Detalhes ou no Plano de ação.
- `/export/notificacoes.pdf?{filtro}`: o resultado filtrado da lista, com uma notificação por página e limite de 200 por arquivo.

Por LGPD, o PDF não inclui e-mail, telefone nem senha, apenas se a notificação é anônima.

### Anonimato

Nenhum dado técnico (IP, user-agent, usuário logado) é gravado junto da notificação. O
campo de contato (e-mail/telefone) é estritamente opcional; quando vazio, `ANONIMO=1` e
os campos de contato ficam `NULL`. A senha de acompanhamento é armazenada apenas como
hash + salt (SHA-256) — nunca em texto puro; ela só aparece uma vez, na tela de conclusão.

## Modelo de dados

8 tabelas, criadas de forma idempotente no startup por
[SchemaInitializer](src/main/java/br/com/hospital/notificacao/config/SchemaInitializer.java)
(sem Flyway/Liquibase, mesmo padrão do protótipo original). O DDL "de verdade" é o Java;
[schema.sql](schema.sql) é só o espelho documental.

| Tabela | Papel |
|---|---|
| `USUARIO` | Login administrativo (hash + salt + perfil) |
| `NOTIFICACAO` | Cabeçalho da notificação (status, problema/subgrupo, classificação, risco, descrição original e revisada, anonimato) |
| `NOTIFICACAO_CAMPO` | Campos dinâmicos por tipo (produto/lote, medicamento/via, ...) |
| `NOTIFICACAO_ANEXO` | Metadados dos arquivos anexados |
| `NOTIFICACAO_HISTORICO` | Trilha cronológica de mudança de status |
| `INVESTIGACAO` | FOR-NSP-014 (fatores contribuintes, 5 Porquês, causa raiz) |
| `INVESTIGACAO_MARCO` | Timeline da investigação |
| `PLANO_ACAO` | Linhas do plano 5W2H |

## Anexos: como são armazenados

Fotos e documentos anexados na notificação (Item 1 dos critérios de aceitação) são
gravados em **dois lugares**, nunca só no banco:

1. **Arquivo físico**: gravado em `ANEXOS_DIR` (padrão `/app/anexos`, montado como o
   volume Docker nomeado `anexos-data` — ver [docker-compose.yml](docker-compose.yml)),
   com um **nome físico UUID** (`a1b2c3d4-....jpg`) para nunca expor o nome original do
   arquivo no sistema de arquivos.
2. **Metadados no DB2**: tabela `NOTIFICACAO_ANEXO` guarda o nome original, o nome
   físico, o tipo MIME e o tamanho em bytes.

Regras aplicadas em
[AnexoService](src/main/java/br/com/hospital/notificacao/service/AnexoService.java):
apenas `image/jpeg`, `image/png` e `application/pdf`; até 10 MB por arquivo (limite também
reforçado no lado do cliente ZK via `WEB-INF/zk.xml`). O download
(`/anexo/{id}`, [AnexoServlet](src/main/java/br/com/hospital/notificacao/web/AnexoServlet.java))
exige sessão administrativa autenticada — anexos podem conter material sensível do
incidente.

Como o volume é nomeado (não bind-mount), ele sobrevive a `docker compose down` e a
rebuilds da imagem; só é perdido com `docker compose down -v`.

## Autenticação e níveis de acesso

Tabela `USUARIO` com senha em hash SHA-256 + salt aleatório por usuário
([HashSenha](src/main/java/br/com/hospital/notificacao/service/HashSenha.java)). Dois
perfis:

- **NSP_ANALISTA**: opera triagem e investigação.
- **NSP_GESTOR**: acesso completo, incluindo conclusão de notificações.

Todas as páginas de `/admin/*` são protegidas por um
[AutenticacaoInit](src/main/java/br/com/hospital/notificacao/web/AutenticacaoInit.java)
(`<?init?>` do ZK) que redireciona para `/login.zul` quando não há usuário autenticado na
sessão — é o mecanismo correto no ZK para proteger páginas (mais confiável que um
`Filter` de servlet, que não intercepta bem as requisições internas do ZK/`zkau`).

### Credenciais iniciais

No primeiro start (tabela `USUARIO` vazia), o sistema semeia 2 usuários
([SeedData](src/main/java/br/com/hospital/notificacao/config/SeedData.java)):

| Login | Perfil |
|---|---|
| `ana.qualidade` | NSP_GESTOR |
| `carlos.nsp` | NSP_ANALISTA |

A senha inicial dos dois vem da variável de ambiente `ADMIN_SENHA_INICIAL` (padrão
`TrocarSenha@2026` se a variável não for definida). **Troque essa senha antes de qualquer
uso real** — a V1 não tem tela de troca de senha pela UI (ver `ROADMAP-V2.md`); trocar
exige atualizar o hash diretamente no banco ou reimplantar com um novo seed.

## Configuração / variáveis de ambiente

| Variável | Padrão | Obrigatória | Descrição |
|---|---|---|---|
| `DB2_HOST` | `localhost` | não | Host do DB2 |
| `DB2_PORT` | `50000` | não | Porta do DB2 |
| `DB2_DATABASE` | `HOSPITAL` | não | Nome do banco |
| `DB2_USER` | `db2inst1` | não | Usuário do DB2 |
| `DB2_PASSWORD` | — | **sim** | Senha do DB2 |
| `DB2INST1_PASSWORD` | — | sim (no Compose) | Senha de criação da instância DB2 |
| `ANEXOS_DIR` | `/app/anexos` (dev: pasta temp do SO) | não | Diretório dos anexos |
| `ADMIN_SENHA_INICIAL` | `TrocarSenha@2026` | não | Senha dos 2 usuários seed |

Copie `.env.example` para `.env` (não versionado) antes de rodar.

## Como rodar

```bash
cp .env.example .env   # ajustar senhas
docker compose up --build
```

- App: `http://localhost:8080`
- DB2: `localhost:50000`
- O healthcheck do DB2 (`start_period: 20m`, `retries: 40`) garante que a app só sobe
  depois do banco pronto — a criação da instância DB2 é lenta na primeira vez, e ainda
  mais lenta em máquinas com pouca RAM livre (o processo passa a depender de swap).

### Testes

Os testes não dependem de Docker nem de DB2:

```bash
./mvnw test        # Linux/macOS
.\mvnw.cmd test    # Windows
```

A suíte tem duas camadas:

- **Unitários (`*Test`)**: regras puras, como matriz de risco, protocolo, hash de senha, montagem do
  `WHERE` dos filtros, mensagens de reclassificação, HTML/PDF do relatório e conversão do filtro em URL.
- **Integração (`*IT`)**: sobem o schema real (`SchemaInitializer`) num **H2 em memória no modo DB2**
  e exercitam DAOs, services e servlets com SQL de verdade. Cobrem:

  | Classe | O que cobre |
  |---|---|
  | `NotificacaoServiceIT` | Abertura de notificações nos 5 canais, anonimato, senha só em hash, anexos e rollback, acompanhamento |
  | `ListagemFiltrosIT` | Filtros da lista, ordenação, paginação, duplicatas e contadores |
  | `ReclassificacaoIT` | Reclassificação, preservação da descrição original e histórico |
  | `FluxoCompletoIT` | Fluxo completo, de Aberto a Concluído |
  | `AutenticacaoIT` | Login e seed de usuários |
  | `SchemaInitializerIT` | Migração de dados |
  | `ServletsIT` | Exportação PDF, download de anexo e QR Codes, incluindo bloqueio sem login e ausência de contato no PDF |

O H2 cobre a lógica e o SQL comum, mas não reproduz 100% o DB2. Para rodar **a mesma suíte
contra um DB2 real** (ex.: o do Compose), use um banco **de teste**, porque cada teste apaga
as tabelas:

```bash
./mvnw test -Dnsp.teste.jdbcUrl=jdbc:db2://localhost:50000/HOSPITAL \
  -Dnsp.teste.usuario=db2inst1 -Dnsp.teste.senha=SUA_SENHA -Dnsp.teste.apagarDados=true
```

Nesse modo também roda o teste de idempotência do `SchemaInitializer` (executar o DDL duas
vezes), que só faz sentido no DB2.

O projeto usa o Maven Wrapper (`.mvn/`, `mvnw`, `mvnw.cmd`) — não é preciso ter o
Maven instalado, o wrapper baixa a versão fixada (3.9.16) na primeira execução.

### Fluxo de teste manual (ponta a ponta)

1. Abrir `http://localhost:8080/` → escolher um dos 5 tipos → confirmar que a cor de
   fundo muda conforme o tipo → preencher os 4 passos → anotar protocolo e senha.
2. `http://localhost:8080/acompanhar.zul` → consultar com o protocolo/senha → ver status
   "Aberto".
3. Login em `http://localhost:8080/login.zul` (`ana.qualidade` / senha do
   `ADMIN_SENHA_INICIAL`) → **Painel** → **Notificações** → **Triagem** (classificar risco
   na matriz, aplicar) → **Investigação** (preencher os 5 Porquês, causa raiz obrigatória)
   → **Plano de ação** (marcar ações como Concluídas) → **Concluir notificação**.
4. Repetir o passo 1 com o mesmo número de prontuário duas vezes → conferir o destaque de
   duplicata em **Notificações**.
5. Na Tecnovigilância, tentar avançar o passo 2 sem Registro ANVISA ou sem o tipo de
   produto → o wizard deve bloquear.
6. **Notificações → Filtros avançados**: filtrar por prontuário, tipo → problema,
   classificação, risco e período; ordenar por "Maior risco".
7. Na **Triagem**, reclassificar o tipo (ex.: Tecnovigilância → Farmacovigilância) e
   escrever uma descrição revisada → em **Detalhes**, conferir a descrição original intacta
   e a mudança registrada no histórico.
8. **Exportar PDF** em Detalhes e na lista filtrada → conferir o conteúdo e a ausência de
   dados de contato.

## Pontos de atenção / limitações conhecidas da V1

- Os testes de integração rodam em H2 (modo DB2) por padrão; contra o DB2 real só quando apontados explicitamente (ver "Testes").
- Sem troca de senha pela UI administrativa.
- Sem Indicadores & BI, exportação `.xlsx` e integração com Google Sheets/ANVISA. Estão classificados como baixa prioridade/opcional nos critérios de aceitação e documentados em `ROADMAP-V2.md`. A exportação em PDF das notificações já existe (ver "Exportação em PDF").
- Ao trocar o tipo de uma notificação na reclassificação, o protocolo mantém o prefixo original (ex.: `#TC-…` passa a ser Farmacovigilância), e os campos dinâmicos do tipo antigo continuam como foram preenchidos. Isso é intencional, para não invalidar o protocolo que o notificante guardou.
- Notificações de Tecnovigilância e Farmacovigilância abertas antes desta versão ficam sem problema (subgrupo). A Triagem pede que ele seja definido antes de iniciar a investigação.
- Data/hora do incidente são campos de texto livre (`dd/mm/aaaa`), não um seletor de calendário — simplificação deliberada para manter o formulário rápido em celular.
- `javax.servlet-api` (Java EE, não Jakarta EE) — relevante para qualquer upgrade futuro de Tomcat.
- Há uma pasta `graphify-out/` na raiz com um grafo de conhecimento do repositório, não faz parte do build.
