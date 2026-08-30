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
        SchemaInitializer.executar();
        SeedData.aplicar();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DataSourceFactory.encerrar();
    }
}
