package br.com.hospital.notificacao.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Inicializa o schema (8 tabelas) e o seed de usuarios no startup do container.
 * Substitui o AppInitListener original (que so criava a tabela "notificacao").
 */
public class AppInitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        DataSourceFactory.inicializar();
        // Se o banco/segredos estiverem incorretos, nao derruba o contexto inteiro
        // (o Tomcat responderia 404 em tudo). O erro fica visivel nos logs.
        try {
            SchemaInitializer.executar();
            SeedData.aplicar();
        } catch (RuntimeException e) {
            java.util.logging.Logger.getLogger(AppInitListener.class.getName())
                    .log(java.util.logging.Level.SEVERE,
                            "Falha ao inicializar schema/seed no DB2. Verifique DB2_HOST, DB2_PORT, DB2_DATABASE, "
                            + "DB2_USER, DB2_PASSWORD, DB2_SSL e ADMIN_SENHA_INICIAL.", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DataSourceFactory.encerrar();
    }
}
