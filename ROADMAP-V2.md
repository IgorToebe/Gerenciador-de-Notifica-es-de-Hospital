# Roadmap V2 — o que ficou fora da V1 e como implementar

A V1 entrega o núcleo obrigatório: os 5 canais de notificação por QR Code e o fluxo
administrativo completo (Painel, Notificações, Triagem, Investigação, Plano de ação
5W2H). Este documento detalha o que ficou fora, por quê, e como implementar — para que a
V2 comece sem precisar redescobrir escopo.

Prioridade de referência: o
[Documento de Critérios de Aceitação](Documentação e especificação do projeto/Documento de Critérios de Aceitação — Sistema de Classificação de Incidentes (HU).md)
já classifica **Indicadores & BI como baixa prioridade/opcional** — por isso ficou de fora
da V1 sem abrir lacuna nos critérios de alta e média prioridade.

## 1. Indicadores & BI (Item 5 dos critérios de aceitação)

**O que é**: painel com os 7 indicadores do HU (Boas Práticas c/ Medicamentos, Cirurgia
Segura, Identificação Segura, Dispensação Segura, Prevenção LPP e Quedas, Medicação
Segura, Passagem de Plantão), cada um com formulário de preenchimento manual.

**Por que ficou fora**: classificado como baixa prioridade nos critérios de aceitação;
maior parte dos dados não vem das notificações (preenchimento manual pelos funcionários),
o que o torna um módulo relativamente independente do núcleo de notificação/triagem.

**Como implementar**:
- Nova tabela `INDICADOR_FORMULARIO` (id, tipo_indicador, periodo, status
  `RASCUNHO`/`EM_EDICAO`/`EXPORTADO`, criado_em, dados em colunas ou num
  `INDICADOR_CAMPO` genérico similar a `NOTIFICACAO_CAMPO`).
- **Salvamento intermediário e reedição em duas etapas** (decisão registrada na ata de
  validação, ver
  [Análise de Requisitos](Documentação e especificação do projeto/Sistema de Classificação de Incidentes do HU - Análise de Requisitos.md),
  seção 3): o formulário precisa de um botão "Salvar rascunho" que grava parcial sem
  validar campos obrigatórios, e permitir reabrir e completar depois.
- Tela `admin/indicadores.zul` + `IndicadoresComposer`, seguindo o mesmo padrão de
  `AdminComposer` já usado pelas 6 telas da V1.
- Reaproveitar `barChart()`/distribuição por tipo já implementado em `PainelComposer`
  como base visual.

## 2. Exportação `.xlsx` / `.pdf`

**O que é**: exportar a lista de notificações e os indicadores para planilhas do hospital
e relatórios em PDF para ANVISA/Sentinela.

**Como implementar**:
- Adicionar `org.apache.poi:poi-ooxml` (Excel) ao `pom.xml`.
- Um `ExportacaoService` que recebe `List<Notificacao>` ou `List<AcaoPlano>` e gera um
  `byte[]` via POI; servir como download por um servlet dedicado (`/export/notificacoes.xlsx`),
  seguindo o padrão de autenticação do `AnexoServlet`.
- Para PDF: `org.apache.pdfbox:pdfbox` ou renderizar um HTML simples e converter
  (`openhtmltopdf`). Escopo menor: começar só pelo `.xlsx`, que já resolve o critério de
  "alimentar as planilhas de indicadores do hospital".
- Os botões "Exportar (.xlsx)" já existem como *stub* (toast informativo) em
  `ListaComposer` e `PlanoComposer` — trocar o `Clients.showNotification(...)` pela
  chamada real ao `ExportacaoService`.

## 3. Integração com Google Sheets / envio à ANVISA

**O que é**: envio automático dos dados consolidados para Google Sheets (BI) e relatórios
periódicos à ANVISA/Hospital Sentinela.

**Como implementar**:
- Google Sheets: API do Google (`google-api-services-sheets`), com uma conta de serviço
  e um job agendado (`ScheduledExecutorService` ou um cron externo) que lê `NOTIFICACAO`
  e escreve nas abas do relatório.
- ANVISA: normalmente é upload manual de arquivo num portal — nesse caso, a integração
  real é apenas a geração do `.xlsx`/`.csv` no formato exigido (ver item 2); um envio
  automático via API só se a ANVISA disponibilizar uma.

## 4. Notificação por e-mail ao notificante

**O que é**: quando o notificante deixou e-mail, avisá-lo quando o status mudar (ex.:
"sua notificação foi concluída").

**Como implementar**:
- Adicionar `jakarta.mail`/`javax.mail` (compatível com o Tomcat 9 / Java EE do
  projeto) ao `pom.xml`.
- Um `EmailService` chamado a partir de `PlanoAcaoService.concluirNotificacao(...)` (e
  opcionalmente em `TriagemService`/`InvestigacaoService`), lendo `CONTATO_EMAIL` quando
  `ANONIMO=0`.
- Configurar SMTP via variáveis de ambiente (`SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`,
  `SMTP_PASSWORD`), seguindo o mesmo padrão de configuração de `DataSourceFactory`.

## 5. Gestão de usuários pela interface

**O que é**: hoje os usuários só existem via `SeedData` (2 usuários fixos) — não há tela
para criar, desativar ou trocar senha de usuários do NSP.

**Como implementar**:
- Tela `admin/usuarios.zul`, restrita a `NSP_GESTOR` (checar `Usuario.getPerfil()` no
  `AutenticacaoInit` ou num novo `Initiator` de perfil).
- CRUD simples sobre `UsuarioDAO` (já existe `buscarPorLogin`; falta `inserir`,
  `listarTodos`, `atualizarSenha`, `atualizarStatusAtivo`).
- Troca de senha usa o mesmo `HashSenha` já implementado.

## 6. Testes de integração com DB2

**O que é**: a V1 só tem testes JUnit de regra pura (`MatrizRisco`, `ProtocoloService`,
`HashSenha`) — sem testes que batem no banco de verdade.

**Como implementar**:
- Adicionar `org.testcontainers:testcontainers` + um container DB2 de teste (mesma
  imagem do `docker-compose.yml`) ou um `Db2ContainerTest` usando a imagem
  `icr.io/db2_community/db2`.
- Testar especificamente: `SchemaInitializer.executar()` duas vezes seguidas (idempotência),
  `NotificacaoDAO.listar()` com os filtros de duplicata, e o fluxo completo
  `NotificacaoService.criar()` → `TriagemService.aplicarClassificacao()` →
  `InvestigacaoService.gerarPlanoDeAcao()` → `PlanoAcaoService.concluirNotificacao()`.

## 7. Trilha LGPD mais completa

**O que é**: hoje o anonimato é garantido por *não gravar* dados de identificação — mas
não há um mecanismo formal de retenção/expurgo de dados (ex.: apagar notificações
concluídas após N anos) nem log de acesso aos dados sensíveis da investigação.

**Como implementar**:
- Job periódico de expurgo (`DATA_INCIDENTE` + política de retenção do hospital).
- Tabela de auditoria de acesso (`ACESSO_LOG`) para leituras de `INVESTIGACAO`/`NOTIFICACAO`
  por usuários administrativos, com quem/quando.
