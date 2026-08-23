package br.com.hospital.notificacao;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class AppInitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        new NotificacaoDAO().criarTabelaSeNaoExiste();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
