package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.dao.NotificacaoDAO;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.Usuario;
import br.com.hospital.notificacao.service.DetalheNotificacao;
import br.com.hospital.notificacao.service.NotificacaoService;
import br.com.hospital.notificacao.service.RelatorioNotificacaoService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exportacao em PDF do que foi preenchido nas notificacoes, restrita a usuarios
 * autenticados do NSP (mesmo padrao do AnexoServlet):
 * <ul>
 *   <li>/export/notificacao/{id}.pdf — uma notificacao (tela de Detalhes / Plano)</li>
 *   <li>/export/notificacoes.pdf?{filtro} — o resultado filtrado da lista (FiltroParametros)</li>
 * </ul>
 */
public class ExportacaoServlet extends HttpServlet {

    /** Teto da exportacao em lote: cada notificacao custa algumas consultas e paginas de PDF. */
    static final int LIMITE_LOTE = 200;

    private static final Pattern UMA_NOTIFICACAO = Pattern.compile("^/notificacao/(\\d+)\\.pdf$");

    private final NotificacaoService notificacaoService = new NotificacaoService();
    private final RelatorioNotificacaoService relatorioService = new RelatorioNotificacaoService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Mesmo criterio do AnexoServlet: sessao com usuario ativo do NSP.
        Usuario usuario = SessaoUtil.usuarioDaSessaoHttp(req.getSession(false));
        if (usuario == null || !usuario.isAtivo()) {
            resp.sendRedirect(req.getContextPath() + "/login.zul");
            return;
        }

        String pathInfo = req.getPathInfo() != null ? req.getPathInfo() : "";
        Matcher uma = UMA_NOTIFICACAO.matcher(pathInfo);
        String html;
        String nomeArquivo;
        if (uma.matches()) {
            DetalheNotificacao detalhe;
            try {
                detalhe = notificacaoService.carregarDetalhe(Long.parseLong(uma.group(1)));
            } catch (IllegalArgumentException e) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String protocolo = detalhe.getNotificacao().getProtocolo();
            html = relatorioService.montarHtml("Notificação " + protocolo, Collections.singletonList(detalhe), null);
            nomeArquivo = "notificacao-" + protocolo.replaceAll("[^A-Za-z0-9-]", "") + ".pdf";
        } else if ("/notificacoes.pdf".equals(pathInfo)) {
            NotificacaoDAO.Filtro filtro = FiltroParametros.deParametros(req::getParameter);
            filtro.pagina = 1;
            filtro.tamanhoPagina = LIMITE_LOTE;
            NotificacaoDAO.ResultadoPagina pagina = notificacaoService.listar(filtro);
            List<DetalheNotificacao> detalhes = new ArrayList<>();
            for (Notificacao n : pagina.itens) {
                detalhes.add(notificacaoService.carregarDetalhe(n.getId()));
            }
            String aviso = pagina.total > LIMITE_LOTE
                    ? "O filtro retornou " + pagina.total + " notificações; este documento traz apenas as "
                      + LIMITE_LOTE + " primeiras. Refine os filtros para exportar as demais."
                    : null;
            html = relatorioService.montarHtml("Relatório de notificações", detalhes, aviso);
            nomeArquivo = "notificacoes.pdf";
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        byte[] pdf = relatorioService.gerarPdf(html);
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "inline; filename=\"" + nomeArquivo + "\"");
        resp.setHeader("Cache-Control", "no-store");
        resp.setContentLength(pdf.length);
        resp.getOutputStream().write(pdf);
    }
}
