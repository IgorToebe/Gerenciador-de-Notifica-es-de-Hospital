package br.com.hospital.notificacao.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.logging.Logger;

@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Inicializando o contexto da aplicação e o pool de conexões...");
        try {
            DataSourceFactory.inicializar();
        } catch (Exception e) {
            LOGGER.severe("Erro ao carregar DataSourceFactory no boot: " + e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Encerrando o contexto da aplicação e fechando o pool HikariCP...");
        DataSourceFactory.encerrar();
    }
}
