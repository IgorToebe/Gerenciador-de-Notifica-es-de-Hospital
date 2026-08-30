package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.model.Usuario;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.A;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

/**
 * Base das telas administrativas do NSP: monta a sidebar comum (6 telas + usuario logado)
 * e garante que cada subclasse só precisa implementar {@link #paginaAtiva()} e
 * {@link #renderConteudo(Div)}. A autorizacao em si é feita pelo {@link AutenticacaoInit}
 * declarado em cada .zul administrativo.
 */
public abstract class AdminComposer extends SelectorComposer<Component> {

    @Wire protected Div sidebar;
    @Wire protected Div conteudo;

    private static final String[][] ITENS_NAV = {
            {"painel", "▦ Painel", "/admin/painel.zul"},
            {"lista", "▤ Notificações", "/admin/notificacoes.zul"},
            {"triagem", "⚖ Triagem & classificação", "/admin/triagem.zul"},
            {"invest", "🔍 Investigação", "/admin/investigacao.zul"},
            {"plano", "✓ Plano de ação 5W2H", "/admin/plano.zul"},
            {"qrcodes", "▧ QR Codes", "/admin/qrcodes.zul"}
    };

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        if (!SessaoUtil.estaAutenticado()) {
            return; // AutenticacaoInit já redirecionou; evita NPE ao montar a sidebar
        }
        montarSidebar();
        renderConteudo(conteudo);
    }

    private void montarSidebar() {
        Div logo = new Div();
        logo.setStyle("padding:0 18px 14px");
        Label b = new Label("HU · NSP");
        b.setStyle("display:block;color:#fff;font-size:15px;font-weight:700");
        Label span = new Label("Núcleo de Segurança do Paciente");
        span.setStyle("font-size:10.5px;color:#8a847a");
        logo.appendChild(b);
        logo.appendChild(span);
        sidebar.appendChild(logo);

        for (String[] item : ITENS_NAV) {
            A link = new A(item[1]);
            link.setSclass("navlink" + (item[0].equals(paginaAtiva()) ? " on" : ""));
            String href = item[2];
            link.addEventListener("onClick", e -> Executions.getCurrent().sendRedirect(href));
            sidebar.appendChild(link);
        }

        Usuario usuario = SessaoUtil.usuarioAtual();
        Div who = new Div();
        who.setSclass("who");
        Label nome = new Label(usuario.getNome());
        nome.setStyle("display:block");
        Label perfil = new Label(usuario.getPerfil().getRotulo());
        who.appendChild(nome);
        who.appendChild(perfil);
        A sair = new A("Sair");
        sair.setStyle("display:block;margin-top:8px;color:#cfc9bf;text-decoration:underline;font-size:11px");
        sair.addEventListener("onClick", e -> {
            SessaoUtil.encerrarSessao();
            Executions.getCurrent().sendRedirect("/login.zul");
        });
        who.appendChild(sair);
        sidebar.appendChild(who);
    }

    protected abstract String paginaAtiva();

    protected abstract void renderConteudo(Div conteudo) throws Exception;
}
