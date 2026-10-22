package br.com.hospital.notificacao.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.Connection;
import java.sql.SQLException;

public final class DataSourceFactory {

    private static final Logger LOGGER = Logger.getLogger(DataSourceFactory.class.getName());
    private static volatile HikariDataSource dataSource;

    private DataSourceFactory() { }

    private static String env(String nome, String padrao) {
        String valor = System.getenv(nome);
        return (valor != null && !valor.trim().isEmpty()) ? valor.trim() : padrao;
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
        // IBM Db2 on Cloud exige SSL (porta 50001). Defina DB2_SSL=true para habilitar.
        boolean ssl = "true".equalsIgnoreCase(env("DB2_SSL", "false"));

        if (senha == null || senha.trim().isEmpty()) {
            LOGGER.log(Level.SEVERE, "ATENÇÃO: A variável de ambiente DB2_PASSWORD não foi informada!");
            // Evita dar crash no boot permitindo que a aplicação suba e os logs mostrem o aviso
            senha = ""; 
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:db2://" + host + ":" + porta + "/" + database
                + (ssl ? ":sslConnection=true;" : ""));
        config.setUsername(usuario);
        config.setPassword(senha);
        config.setDriverClassName("com.ibm.db2.jcc.DB2Driver");
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(10);
        
        // Timeout de conexão máximo de 5s para não travar threads
        config.setConnectionTimeout(5000); 
        
        // CRUCIAL PARA CODE ENGINE / SERVERLESS:
        // Impede que o HikariCP derrube a aplicação caso a primeira conexão com o DB2 falhe no boot.
        // O pool continuará tentando conectar em segundo plano.
        config.setInitializationFailTimeout(-1);
        
        config.setConnectionTestQuery("VALUES 1");
        config.setPoolName("nsp-db2-pool");

        try {
            dataSource = new HikariDataSource(config);
            LOGGER.info("Pool de conexões HikariCP inicializado com sucesso.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao instanciar o HikariDataSource", e);
        }
    }

    public static Connection obterConexao() throws SQLException {
        if (dataSource == null) {
            inicializar();
        }
        if (dataSource == null) {
            throw new SQLException("Não foi possível obter conexão: O DataSource não pôde ser inicializado.");
        }
        return dataSource.getConnection();
    }

    public static synchronized void encerrar() {
        if (dataSource != null) {
            try {
                dataSource.close();
                dataSource = null;
                LOGGER.info("Pool HikariCP fechado com sucesso.");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erro ao fechar o HikariDataSource", e);
            }
        }

        // Desregistra drivers JDBC para evitar vazamento de classloader no redeploy
        java.util.Enumeration<java.sql.Driver> drivers = java.sql.DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            java.sql.Driver driver = drivers.nextElement();
            try {
                java.sql.DriverManager.deregisterDriver(driver);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erro ao desregistrar driver JDBC: " + driver, e);
            }
        }
    }
}
