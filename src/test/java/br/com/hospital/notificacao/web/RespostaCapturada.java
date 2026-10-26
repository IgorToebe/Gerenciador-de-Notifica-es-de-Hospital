package br.com.hospital.notificacao.web;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import br.com.hospital.notificacao.model.Perfil;
import br.com.hospital.notificacao.model.Usuario;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Apoio aos testes de servlet: request/sessao simulados e corpo da resposta capturado. */
final class RespostaCapturada {

    final HttpServletResponse resposta = mock(HttpServletResponse.class);
    private final ByteArrayOutputStream corpo = new ByteArrayOutputStream();

    RespostaCapturada() throws IOException {
        when(resposta.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(WriteListener writeListener) { }
            @Override public void write(int b) { corpo.write(b); }
        });
    }

    byte[] bytes() {
        return corpo.toByteArray();
    }

    /** Requisicao GET com pathInfo e, se {@code logado}, um usuario ativo do NSP na sessao. */
    static HttpServletRequest requisicao(String pathInfo, boolean logado) {
        return requisicao(pathInfo, logado, true);
    }

    static HttpServletRequest requisicao(String pathInfo, boolean logado, boolean ativo) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getPathInfo()).thenReturn(pathInfo);
        when(req.getContextPath()).thenReturn("");
        if (logado) {
            Usuario u = new Usuario();
            u.setId(1);
            u.setLogin("ana.qualidade");
            u.setPerfil(Perfil.NSP_GESTOR);
            u.setAtivo(ativo);
            HttpSession sessao = mock(HttpSession.class);
            when(sessao.getAttribute(anyString())).thenReturn(u);
            when(req.getSession(false)).thenReturn(sessao);
        }
        return req;
    }
}
