package br.com.hospital.notificacao.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Fabrica do pool de conexoes DB2 (HikariCP). Sem pool, cada operacao paga o
 * handshake completo do DB2 (200-500ms), o que inviabiliza o criterio de aceitacao
 * de resposta em ate 5 segundos para carregar e enviar o formulario.
 *
 * Le as mesmas variaveis de ambiente do ConexaoDB2 original, preservando compatibilidade
 * com o docker-compose.yml existente.
 */
public final class DataSourceFactory {

    private static volatile HikariDataSource dataSource;

    private DataSourceFactory() { }

    private static String env(String nome, String padrao) {
        String valor = System.getenv(nome);
        return (valor != null && !valor.isEmpty()) ? valor : padrao;
    }

    public static synchronized void inicializar() {
        if (dataSource != null) {
            return;
        }
        String host = env("DB2_HOST", "localhost");
        String porta = env("DB2_PORT", "50000");
        String database = env("DB2_DATABASE", "HOSPITAL");
        String usuario = env("DB2_USER", "db2inst1");
        String senha = System.getenv("DB2_PASSWORD");
        if (senha == null) {
            throw new IllegalStateException("Variável de ambiente DB2_PASSWORD não definida");
        }
        inicializar("jdbc:db2://" + host + ":" + porta + "/" + database, usuario, senha);
    }

    /**
     * Inicializa o pool com uma URL JDBC explicita. Usado pelos testes de integracao
     * (banco em memoria ou um DB2 de teste); a aplicacao usa {@link #inicializar()}.
     */
    public static synchronized void inicializar(String jdbcUrl, String usuario, String senha) {
        if (dataSource != null) {
            return;
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(usuario);
        config.setPassword(senha);
        if (jdbcUrl.startsWith("jdbc:db2:")) {
            config.setDriverClassName("com.ibm.db2.jcc.DB2Driver");
        }
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(15_000);
        config.setConnectionTestQuery("VALUES 1");
        config.setPoolName("nsp-db2-pool");

        dataSource = new HikariDataSource(config);
    }

    public static Connection obterConexao() throws SQLException {
        if (dataSource == null) {
            inicializar();
        }
        return dataSource.getConnection();
    }

    public static synchronized void encerrar() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }
}
