package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.model.Usuario;
import org.zkoss.zk.ui.Sessions;

import javax.servlet.http.HttpSession;

/** Acesso ao usuario autenticado guardado na Session HTTP (via ZK Sessions). */
public final class SessaoUtil {

    // ZK grava atributos de Session diretamente na HttpSession nativa (mesmo escopo),
    // entao a mesma chave e' usada tanto pelos Composers ZK quanto por servlets puros
    // (ver AnexoServlet), que nao tem um Execution ZK ativo para chamar Sessions.getCurrent().
    private static final String CHAVE_USUARIO = "usuarioLogado";

    private SessaoUtil() { }

    public static void autenticar(Usuario usuario) {
        Sessions.getCurrent().setAttribute(CHAVE_USUARIO, usuario);
    }

    public static Usuario usuarioAtual() {
        return (Usuario) Sessions.getCurrent().getAttribute(CHAVE_USUARIO);
    }

    public static boolean estaAutenticado() {
        return usuarioAtual() != null;
    }

    public static void encerrarSessao() {
        Sessions.getCurrent().removeAttribute(CHAVE_USUARIO);
        Sessions.getCurrent().invalidate();
    }

    /** Para uso em servlets puros (sem Execution ZK ativo), como o AnexoServlet. */
    public static Usuario usuarioDaSessaoHttp(HttpSession sessaoHttp) {
        if (sessaoHttp == null) return null;
        return (Usuario) sessaoHttp.getAttribute(CHAVE_USUARIO);
    }
}
