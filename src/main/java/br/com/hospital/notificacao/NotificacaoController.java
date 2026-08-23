package br.com.hospital.notificacao;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;

import java.util.List;

public class NotificacaoController extends SelectorComposer<Component> {

    @Wire
    private Textbox txtMensagem;
    @Wire
    private Listbox listNotificacoes;

    private final NotificacaoDAO dao = new NotificacaoDAO();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        carregarNotificacoes();
    }

    @Listen("onClick = #btnAdicionar")
    public void adicionar() {
        String mensagem = txtMensagem.getValue();
        if (mensagem == null || mensagem.trim().isEmpty()) {
            return;
        }
        dao.inserir(mensagem.trim());
        txtMensagem.setValue("");
        carregarNotificacoes();
    }

    private void carregarNotificacoes() {
        listNotificacoes.getItems().clear();
        List<Notificacao> notificacoes = dao.listarTodas();
        for (Notificacao n : notificacoes) {
            Listitem item = new Listitem();
            item.appendChild(new Listcell(String.valueOf(n.getId())));
            item.appendChild(new Listcell(n.getMensagem()));
            item.appendChild(new Listcell(String.valueOf(n.getDataCriacao())));
            listNotificacoes.appendChild(item);
        }
    }
}
