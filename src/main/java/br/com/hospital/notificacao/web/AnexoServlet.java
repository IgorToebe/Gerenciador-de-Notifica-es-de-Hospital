package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.dao.AnexoDAO;
import br.com.hospital.notificacao.model.Anexo;
import br.com.hospital.notificacao.service.AnexoService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Download de anexos: /anexo/{id}. Restrito a usuarios autenticados do NSP — os anexos
 * podem conter fotos/documentos sensiveis do incidente.
 */
public class AnexoServlet extends HttpServlet {

    private final AnexoDAO anexoDAO = new AnexoDAO();
    private final AnexoService anexoService = new AnexoService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (SessaoUtil.usuarioDaSessaoHttp(req.getSession(false)) == null) {
            resp.sendRedirect(req.getContextPath() + "/login.zul");
            return;
        }

        String pathInfo = req.getPathInfo();
        String idParam = (pathInfo != null && pathInfo.length() > 1) ? pathInfo.substring(1) : null;
        if (idParam == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Optional<Anexo> anexo;
        try {
            anexo = anexoDAO.buscarPorId(Long.parseLong(idParam));
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (!anexo.isPresent()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Optional<byte[]> conteudo = anexoService.lerConteudo(anexo.get().getNomeFisico());
        if (!conteudo.isPresent()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Arquivo não encontrado no volume de anexos");
            return;
        }

        resp.setContentType(anexo.get().getTipoArquivo());
        resp.setHeader("Content-Disposition", "inline; filename=\"" + anexo.get().getNomeOriginal() + "\"");
        resp.getOutputStream().write(conteudo.get());
    }
}
