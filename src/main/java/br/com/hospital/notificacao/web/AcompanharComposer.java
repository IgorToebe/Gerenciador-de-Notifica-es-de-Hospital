package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.model.TipoNotificacao;
import br.com.hospital.notificacao.service.NotificacaoService;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

import java.util.Optional;

/** Consulta publica de status por protocolo + senha (Item 3: rastreamento do incidente). */
public class AcompanharComposer extends SelectorComposer<Component> {

    @Wire private Textbox txtProtocolo;
    @Wire private Textbox txtSenha;
    @Wire private Div resultado;

    private final NotificacaoService notificacaoService = new NotificacaoService();

    @Listen("onClick = #btnVoltar")
    public void voltar() {
        Executions.getCurrent().sendRedirect("/index.zul");
    }

    @Listen("onClick = #btnConsultar")
    public void consultar() {
        resultado.getChildren().clear();
        String protocolo = txtProtocolo.getValue();
        if (protocolo == null || protocolo.trim().isEmpty()) {
            resultado.appendChild(mensagem("Informe o número do protocolo.", "#a82654"));
            return;
        }

        Optional<Notificacao> encontrada = notificacaoService.buscarParaAcompanhamento(protocolo, txtSenha.getValue());
        if (!encontrada.isPresent()) {
            resultado.appendChild(mensagem(
                    "Protocolo ou senha não encontrados. Confira os dados informados na conclusão da notificação.",
                    "#a82654"));
            return;
        }

        Notificacao n = encontrada.get();
        TipoNotificacao tipo = n.getTipo();
        StatusNotificacao status = n.getStatus();

        Div card = new Div();
        card.setSclass("nsp-card-soft");

        Label protocoloLbl = new Label(n.getProtocolo());
        protocoloLbl.setStyle("font-size:15px;font-weight:800;display:block;margin-bottom:8px;color:" + tipo.getCorEscura());
        card.appendChild(protocoloLbl);

        card.appendChild(construirStepper(status));

        Label statusLbl = new Label(status.getRotulo() + (n.getNivelRisco() != null ? "  ·  Risco " + n.getNivelRisco().getRotulo() : ""));
        statusLbl.setSclass("nsp-tg");
        statusLbl.setStyle("background:#f0ece4;color:#3a352f;margin-top:6px;display:inline-block");
        card.appendChild(statusLbl);

        String mensagemFinal = status == StatusNotificacao.CONCLUIDO
                ? "Investigação concluída e plano de ação registrado."
                : "Em tratamento pelo NSP — sem necessidade de ação sua.";
        Label rodape = new Label(tipo.getTitulo() + " · aberta em " + n.getDataIncidente() + ". " + mensagemFinal);
        rodape.setStyle("display:block;margin-top:8px;font-size:12px;color:var(--muted)");
        card.appendChild(rodape);

        resultado.appendChild(card);
    }

    private Div construirStepper(StatusNotificacao atual) {
        Div stepper = new Div();
        stepper.setSclass("nsp-stepper");
        stepper.setStyle("margin:8px 0");
        for (int i = 1; i <= 5; i++) {
            Div st = new Div();
            String classe = i < atual.getPasso() ? "done" : (i == atual.getPasso() ? "act" : "");
            st.setSclass("st " + classe);
            Label numero = new Label(String.valueOf(i));
            st.appendChild(numero);
            stepper.appendChild(st);
        }
        return stepper;
    }

    private Div mensagem(String texto, String cor) {
        Div div = new Div();
        div.setSclass("nsp-card-soft");
        Label lbl = new Label(texto);
        lbl.setStyle("color:" + cor + ";font-size:12.5px;margin:0");
        div.appendChild(lbl);
        return div;
    }
}
