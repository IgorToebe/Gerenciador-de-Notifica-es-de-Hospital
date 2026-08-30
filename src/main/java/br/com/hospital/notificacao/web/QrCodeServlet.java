package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.model.TipoNotificacao;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Gera o PNG do QR Code de um canal de notificação: /qr?tipo=tecno&amp;size=512.
 * O conteúdo codificado é a URL curta /n/{tipo}, resolvida pelo {@link QrRedirectServlet}.
 */
public class QrCodeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String chaveTipo = req.getParameter("tipo");
        if (chaveTipo == null || !TipoNotificacao.existe(chaveTipo)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Tipo de notificação inválido");
            return;
        }
        int tamanho = 320;
        try {
            String sizeParam = req.getParameter("size");
            if (sizeParam != null) {
                tamanho = Math.max(120, Math.min(1024, Integer.parseInt(sizeParam)));
            }
        } catch (NumberFormatException ignored) { }

        String url = urlBase(req) + "/n/" + chaveTipo;

        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, tamanho, tamanho, hints);
            resp.setContentType("image/png");
            MatrixToImageWriter.writeToStream(matrix, "PNG", resp.getOutputStream());
        } catch (WriterException e) {
            throw new ServletException("Erro ao gerar QR Code", e);
        }
    }

    private String urlBase(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append(req.getScheme()).append("://").append(req.getServerName());
        boolean portaPadrao = ("http".equals(req.getScheme()) && req.getServerPort() == 80)
                || ("https".equals(req.getScheme()) && req.getServerPort() == 443);
        if (!portaPadrao) {
            sb.append(":").append(req.getServerPort());
        }
        sb.append(req.getContextPath());
        return sb.toString();
    }
}
