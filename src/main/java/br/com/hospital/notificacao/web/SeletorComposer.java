package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.model.TipoNotificacao;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

/**
 * Tela de entrada / simulação de leitura de QR Code: lista os 5 canais de notificação,
 * cada um com sua cor de identidade, e leva ao wizard correspondente.
 */
public class SeletorComposer extends SelectorComposer<Component> {

    @Wire
    private Div gridProfissionais;
    @Wire
    private Div gridPaciente;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        for (TipoNotificacao tipo : TipoNotificacao.values()) {
            Div alvo = tipo.isAcessoProfissional() ? gridProfissionais : gridPaciente;
            alvo.appendChild(criarCard(tipo));
        }
    }

    private Div criarCard(TipoNotificacao tipo) {
        Div card = new Div();
        card.setSclass("nsp-seltype");
        card.setStyle("cursor:pointer");

        Div icone = new Div();
        icone.setSclass("si");
        icone.setStyle("background:" + tipo.getCorSuave());
        icone.appendChild(IconeSvg.criar(tipo.getIcone(), tipo.isIconePreenchido(), tipo.getCor(), 18));
        card.appendChild(icone);

        Div texto = new Div();
        Label titulo = new Label(tipo.getTitulo());
        titulo.setPre(false);
        titulo.setSclass("titulo-card");
        titulo.setStyle("display:block;font-size:13.5px;font-weight:700;color:#23201c");
        Label sub = new Label(tipo.getSubtitulo());
        sub.setStyle("font-size:11px;color:var(--muted)");
        texto.appendChild(titulo);
        texto.appendChild(sub);
        card.appendChild(texto);

        card.addEventListener("onClick", event ->
                org.zkoss.zk.ui.Executions.getCurrent().sendRedirect("/notificar.zul?tipo=" + tipo.getChaveUrl()));
        return card;
    }

    @Listen("onClick = #btnAcompanhar")
    public void acompanhar() {
        org.zkoss.zk.ui.Executions.getCurrent().sendRedirect("/acompanhar.zul");
    }

    @Listen("onClick = #btnLogin")
    public void login() {
        org.zkoss.zk.ui.Executions.getCurrent().sendRedirect("/login.zul");
    }
}
