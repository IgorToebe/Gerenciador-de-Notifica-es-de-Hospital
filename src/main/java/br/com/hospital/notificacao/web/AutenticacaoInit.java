package br.com.hospital.notificacao.web;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.util.Initiator;

import java.util.Map;

/**
 * Protege as paginas de /admin/*: se nao houver usuario autenticado na sessao, redireciona
 * para o login antes da pagina ser composta. Declarado via {@code <?init class="..."?>}
 * no topo de cada .zul administrativo (padrao ZK para autorizacao de pagina, mais
 * confiavel que um Filter de servlet para requisicoes AU do proprio ZK).
 */
public class AutenticacaoInit implements Initiator {

    @Override
    public void doInit(Page page, Map<String, Object> args) {
        if (!SessaoUtil.estaAutenticado()) {
            Executions.getCurrent().sendRedirect("/login.zul");
        }
    }
}
