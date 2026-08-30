package br.com.hospital.notificacao.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cria o schema completo (8 tabelas) de forma idempotente no startup do container,
 * seguindo o mesmo padrão do antigo NotificacaoDAO.criarTabelaSeNaoExiste(): sem
 * Flyway/Liquibase, apenas DDL Java ignorando SQLSTATE 42710 (objeto já existe).
 *
 * O DDL aqui é a fonte de verdade; schema.sql na raiz do repositório é apenas
 * o espelho documental.
 */
public final class SchemaInitializer {

    private static final Logger LOG = Logger.getLogger(SchemaInitializer.class.getName());
    private static final String SQLSTATE_OBJETO_JA_EXISTE = "42710";

    private SchemaInitializer() { }

    private static final List<String> DDL = Arrays.asList(
            "CREATE TABLE USUARIO (" +
                    "ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "LOGIN VARCHAR(60) NOT NULL, " +
                    "NOME VARCHAR(120) NOT NULL, " +
                    "SENHA_HASH VARCHAR(128) NOT NULL, " +
                    "SALT VARCHAR(64) NOT NULL, " +
                    "PERFIL VARCHAR(20) NOT NULL, " +
                    "ATIVO SMALLINT NOT NULL DEFAULT 1, " +
                    "CRIADO_EM TIMESTAMP NOT NULL DEFAULT CURRENT TIMESTAMP, " +
                    "CONSTRAINT UQ_USUARIO_LOGIN UNIQUE (LOGIN))",

            "CREATE TABLE NOTIFICACAO (" +
                    "ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "PROTOCOLO VARCHAR(30) NOT NULL, " +
                    "TIPO VARCHAR(30) NOT NULL, " +
                    "SENHA_HASH VARCHAR(128) NOT NULL, " +
                    "SENHA_SALT VARCHAR(64) NOT NULL, " +
                    "DATA_INCIDENTE VARCHAR(10), " +
                    "HORA_INCIDENTE VARCHAR(5), " +
                    "SETOR VARCHAR(120), " +
                    "PRONTUARIO VARCHAR(40), " +
                    "GRAVIDADE VARCHAR(60), " +
                    "GEROU_DANO SMALLINT NOT NULL DEFAULT 0, " +
                    "DESCRICAO VARCHAR(4000), " +
                    "ANONIMO SMALLINT NOT NULL DEFAULT 1, " +
                    "CONTATO_EMAIL VARCHAR(160), " +
                    "CONTATO_TELEFONE VARCHAR(40), " +
                    "STATUS VARCHAR(20) NOT NULL DEFAULT 'ABERTO', " +
                    "PROBABILIDADE SMALLINT, " +
                    "IMPACTO SMALLINT, " +
                    "SCORE_RISCO SMALLINT, " +
                    "NIVEL_RISCO VARCHAR(20), " +
                    "CRIADO_EM TIMESTAMP NOT NULL DEFAULT CURRENT TIMESTAMP, " +
                    "ATUALIZADO_EM TIMESTAMP NOT NULL DEFAULT CURRENT TIMESTAMP, " +
                    "CONSTRAINT UQ_NOTIFICACAO_PROTOCOLO UNIQUE (PROTOCOLO))",
            "CREATE INDEX IX_NOTIF_PRONTUARIO ON NOTIFICACAO(PRONTUARIO)",
            "CREATE INDEX IX_NOTIF_STATUS ON NOTIFICACAO(STATUS)",
            "CREATE INDEX IX_NOTIF_TIPO ON NOTIFICACAO(TIPO)",
            "CREATE INDEX IX_NOTIF_CRIADO ON NOTIFICACAO(CRIADO_EM)",

            "CREATE TABLE NOTIFICACAO_CAMPO (" +
                    "ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "ID_NOTIFICACAO BIGINT NOT NULL, " +
                    "CHAVE VARCHAR(60) NOT NULL, " +
                    "ROTULO VARCHAR(160) NOT NULL, " +
                    "VALOR VARCHAR(500), " +
                    "ORDEM SMALLINT NOT NULL DEFAULT 0, " +
                    "CONSTRAINT FK_CAMPO_NOTIFICACAO FOREIGN KEY (ID_NOTIFICACAO) REFERENCES NOTIFICACAO(ID))",

            "CREATE TABLE NOTIFICACAO_ANEXO (" +
                    "ID_ANEXO BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "ID_NOTIFICACAO BIGINT NOT NULL, " +
                    "NOME_ORIGINAL VARCHAR(255) NOT NULL, " +
                    "NOME_FISICO VARCHAR(255) NOT NULL, " +
                    "TIPO_ARQUIVO VARCHAR(50) NOT NULL, " +
                    "TAMANHO_BYTES BIGINT NOT NULL, " +
                    "DATA_UPLOAD TIMESTAMP DEFAULT CURRENT TIMESTAMP, " +
                    "CONSTRAINT FK_ANEXO_NOTIFICACAO FOREIGN KEY (ID_NOTIFICACAO) REFERENCES NOTIFICACAO(ID))",

            "CREATE TABLE NOTIFICACAO_HISTORICO (" +
                    "ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "ID_NOTIFICACAO BIGINT NOT NULL, " +
                    "STATUS_ANTERIOR VARCHAR(20), " +
                    "STATUS_NOVO VARCHAR(20) NOT NULL, " +
                    "ID_USUARIO BIGINT, " +
                    "OBSERVACAO VARCHAR(500), " +
                    "CRIADO_EM TIMESTAMP NOT NULL DEFAULT CURRENT TIMESTAMP, " +
                    "CONSTRAINT FK_HIST_NOTIFICACAO FOREIGN KEY (ID_NOTIFICACAO) REFERENCES NOTIFICACAO(ID))",

            "CREATE TABLE INVESTIGACAO (" +
                    "ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "ID_NOTIFICACAO BIGINT NOT NULL, " +
                    "PARECERISTA VARCHAR(120), " +
                    "DATA_INVESTIGACAO VARCHAR(10), " +
                    "FONTES VARCHAR(300), " +
                    "FATOR_PROFISSIONAL VARCHAR(1000), " +
                    "FATOR_COMUNICACAO VARCHAR(1000), " +
                    "FATOR_PACIENTE VARCHAR(1000), " +
                    "FATOR_AMBIENTE VARCHAR(1000), " +
                    "FATOR_ORGANIZACIONAL VARCHAR(1000), " +
                    "FATOR_EXTERNO VARCHAR(1000), " +
                    "PORQUE1 VARCHAR(500), " +
                    "PORQUE2 VARCHAR(500), " +
                    "PORQUE3 VARCHAR(500), " +
                    "PORQUE4 VARCHAR(500), " +
                    "CAUSA_RAIZ VARCHAR(500), " +
                    "DETECCAO VARCHAR(1000), " +
                    "ATENUANTES VARCHAR(1000), " +
                    "PARECER VARCHAR(2000), " +
                    "CONCLUIDA SMALLINT NOT NULL DEFAULT 0, " +
                    "CONSTRAINT UQ_INVEST_NOTIFICACAO UNIQUE (ID_NOTIFICACAO), " +
                    "CONSTRAINT FK_INVEST_NOTIFICACAO FOREIGN KEY (ID_NOTIFICACAO) REFERENCES NOTIFICACAO(ID))",

            "CREATE TABLE INVESTIGACAO_MARCO (" +
                    "ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "ID_INVESTIGACAO BIGINT NOT NULL, " +
                    "QUANDO VARCHAR(40), " +
                    "DESCRICAO VARCHAR(500), " +
                    "ORDEM SMALLINT NOT NULL DEFAULT 0, " +
                    "CONSTRAINT FK_MARCO_INVESTIGACAO FOREIGN KEY (ID_INVESTIGACAO) REFERENCES INVESTIGACAO(ID))",

            "CREATE TABLE PLANO_ACAO (" +
                    "ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "ID_NOTIFICACAO BIGINT NOT NULL, " +
                    "CODIGO VARCHAR(10), " +
                    "O_QUE VARCHAR(300), " +
                    "QUEM VARCHAR(160), " +
                    "ONDE VARCHAR(160), " +
                    "PORQUE VARCHAR(500), " +
                    "COMO VARCHAR(500), " +
                    "QUANTO VARCHAR(60), " +
                    "DATA_INICIO VARCHAR(10), " +
                    "DATA_FIM VARCHAR(10), " +
                    "STATUS VARCHAR(20) NOT NULL DEFAULT 'INICIAR', " +
                    "EVIDENCIA VARCHAR(300), " +
                    "ORDEM SMALLINT NOT NULL DEFAULT 0, " +
                    "CONSTRAINT FK_PLANO_NOTIFICACAO FOREIGN KEY (ID_NOTIFICACAO) REFERENCES NOTIFICACAO(ID))"
    );

    public static void executar() {
        try (Connection con = DataSourceFactory.obterConexao()) {
            for (String ddl : DDL) {
                try (Statement st = con.createStatement()) {
                    st.execute(ddl);
                } catch (SQLException e) {
                    if (!SQLSTATE_OBJETO_JA_EXISTE.equals(e.getSQLState())) {
                        throw new RuntimeException("Erro ao executar DDL: " + ddl, e);
                    }
                    LOG.log(Level.FINE, "Objeto ja existente, ignorando: {0}", ddl);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao conectar para inicializar o schema", e);
        }
    }
}
