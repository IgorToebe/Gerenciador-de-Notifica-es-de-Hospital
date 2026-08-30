package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.model.Usuario;
import br.com.hospital.notificacao.service.AutenticacaoService;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

import java.util.Optional;

/** Login do painel administrativo do NSP (hash + salt em USUARIO). */
public class LoginComposer extends SelectorComposer<Component> {

    @Wire private Textbox txtLogin;
    @Wire private Textbox txtSenha;
    @Wire private Label lblErro;

    private final AutenticacaoService autenticacaoService = new AutenticacaoService();
    private int tentativas = 0;

    @Listen("onClick = #btnEntrar")
    public void entrar() {
        if (tentativas >= 5) {
            lblErro.setValue("Muitas tentativas nesta sessão. Recarregue a página e tente novamente.");
            return;
        }
        Optional<Usuario> usuario = autenticacaoService.autenticar(txtLogin.getValue(), txtSenha.getValue());
        if (!usuario.isPresent()) {
            tentativas++;
            lblErro.setValue("Login ou senha inválidos.");
            return;
        }
        SessaoUtil.autenticar(usuario.get());
        Executions.getCurrent().sendRedirect("/admin/painel.zul");
    }
}
