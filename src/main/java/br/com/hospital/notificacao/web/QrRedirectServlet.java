package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.model.TipoNotificacao;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * URL curta e imprimível para os QR Codes: /n/tecno redireciona para o wizard completo
 * (/notificar.zul?tipo=tecno). Mantém o link impresso nos cartazes estável mesmo que a
 * rota interna do wizard mude.
 */
public class QrRedirectServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo(); // ex.: "/tecno"
        String chaveTipo = (pathInfo != null && pathInfo.length() > 1) ? pathInfo.substring(1) : null;
        if (chaveTipo == null || !TipoNotificacao.existe(chaveTipo)) {
            resp.sendRedirect(req.getContextPath() + "/index.zul");
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/notificar.zul?tipo=" + chaveTipo);
    }
}
