package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.integracao.IntegracaoBase;
import br.com.hospital.notificacao.model.Anexo;
import br.com.hospital.notificacao.model.TipoNotificacao;
import br.com.hospital.notificacao.service.AnexoUpload;
import br.com.hospital.notificacao.service.NovaNotificacaoRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Servlets fora do ZK: exportacao em PDF, download de anexos e QR Codes. */
class ServletsIT extends IntegracaoBase {

    private final ExportacaoServlet exportacao = new ExportacaoServlet();
    private final AnexoServlet anexo = new AnexoServlet();

    private static String textoDoPdf(byte[] pdf) throws IOException {
        try (PDDocument doc = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private long notificacaoComContato(TipoNotificacao tipo, String descricao) {
        NovaNotificacaoRequest req = requisicao(tipo);
        req.setDescricao(descricao);
        req.setContatoEmail("denunciante@email.com");
        req.setContatoTelefone("44999998888");
        return criarId(req);
    }

    // ---- /export/* ----

    @Test
    void exportacaoSemLoginRedirecionaParaOLogin() throws Exception {
        long id = criarId(requisicao(TipoNotificacao.TECNOVIGILANCIA));
        RespostaCapturada r = new RespostaCapturada();
        exportacao.doGet(RespostaCapturada.requisicao("/notificacao/" + id + ".pdf", false), r.resposta);

        verify(r.resposta).sendRedirect("/login.zul");
        assertEquals(0, r.bytes().length, "nenhum dado vaza sem autenticação");
    }

    @Test
    void usuarioDesativadoNaoExportaNemBaixaAnexo() throws Exception {
        Anexo a = notificacaoComAnexo(new byte[]{1, 2, 3});
        RespostaCapturada pdf = new RespostaCapturada();
        exportacao.doGet(RespostaCapturada.requisicao("/notificacao/" + a.getIdNotificacao() + ".pdf", true, false),
                pdf.resposta);
        verify(pdf.resposta).sendRedirect("/login.zul");
        assertEquals(0, pdf.bytes().length);

        RespostaCapturada arquivo = new RespostaCapturada();
        anexo.doGet(RespostaCapturada.requisicao("/" + a.getId(), true, false), arquivo.resposta);
        verify(arquivo.resposta).sendRedirect("/login.zul");
        assertEquals(0, arquivo.bytes().length);
    }

    @Test
    void exportaUmaNotificacaoEmPdfSemDadosDeContato() throws Exception {
        long id = notificacaoComContato(TipoNotificacao.TECNOVIGILANCIA, "Bomba de infusão parou sem alarme");
        String protocolo = (String) valor("SELECT PROTOCOLO FROM NOTIFICACAO WHERE ID = ?", id);
        RespostaCapturada r = new RespostaCapturada();

        exportacao.doGet(RespostaCapturada.requisicao("/notificacao/" + id + ".pdf", true), r.resposta);

        verify(r.resposta).setContentType("application/pdf");
        verify(r.resposta).setHeader("Content-Disposition",
                "inline; filename=\"notificacao-" + protocolo.substring(1) + ".pdf\"");
        String texto = textoDoPdf(r.bytes());
        assertTrue(texto.contains(protocolo));
        assertTrue(texto.contains("Bomba de infusão parou sem alarme"));
        assertTrue(texto.contains("Contato vinculado (não exportado)"));
        assertFalse(texto.contains("denunciante@email.com"), "LGPD: e-mail não vai para o PDF");
        assertFalse(texto.contains("44999998888"), "LGPD: telefone não vai para o PDF");
    }

    @Test
    void exportaAListaRespeitandoOFiltro() throws Exception {
        notificacaoComContato(TipoNotificacao.FARMACOVIGILANCIA, "Dose dobrada de heparina");
        notificacaoComContato(TipoNotificacao.TECNOVIGILANCIA, "Monitor desligou sozinho");
        HttpServletRequest req = RespostaCapturada.requisicao("/notificacoes.pdf", true);
        when(req.getParameter("tipo")).thenReturn("farmaco");
        RespostaCapturada r = new RespostaCapturada();

        exportacao.doGet(req, r.resposta);

        String texto = textoDoPdf(r.bytes());
        assertTrue(texto.contains("Dose dobrada de heparina"));
        assertFalse(texto.contains("Monitor desligou sozinho"));
        assertTrue(texto.contains("1 notificação(ões)"));
    }

    @Test
    void exportacaoDeListaVaziaAindaGeraUmDocumento() throws Exception {
        HttpServletRequest req = RespostaCapturada.requisicao("/notificacoes.pdf", true);
        when(req.getParameter("prontuario")).thenReturn("nao-existe");
        RespostaCapturada r = new RespostaCapturada();

        exportacao.doGet(req, r.resposta);

        assertTrue(textoDoPdf(r.bytes()).contains("Nenhuma notificação encontrada"));
    }

    @Test
    void exportacaoComCaminhoOuIdInvalidoDa404() throws Exception {
        for (String caminho : new String[]{"/notificacao/999999.pdf", "/notificacao/abc.pdf", "/outra-coisa", null}) {
            RespostaCapturada r = new RespostaCapturada();
            exportacao.doGet(RespostaCapturada.requisicao(caminho, true), r.resposta);
            verify(r.resposta).sendError(HttpServletResponse.SC_NOT_FOUND);
            verify(r.resposta, never()).setContentType(anyString());
        }
    }

    // ---- /anexo/{id} ----

    private Anexo notificacaoComAnexo(byte[] conteudo) {
        NovaNotificacaoRequest req = requisicao(TipoNotificacao.TECNOVIGILANCIA);
        req.getAnexos().add(new AnexoUpload("foto.png", "image/png", conteudo));
        return notificacaoService.carregarCompleta(criarId(req)).getAnexos().get(0);
    }

    @Test
    void downloadDeAnexoExigeLogin() throws Exception {
        Anexo a = notificacaoComAnexo(new byte[]{1, 2, 3});
        RespostaCapturada r = new RespostaCapturada();
        anexo.doGet(RespostaCapturada.requisicao("/" + a.getId(), false), r.resposta);

        verify(r.resposta).sendRedirect("/login.zul");
        assertEquals(0, r.bytes().length);
    }

    @Test
    void downloadDeAnexoDevolveOArquivoOriginal() throws Exception {
        byte[] conteudo = "\u0089PNG imagem".getBytes(StandardCharsets.ISO_8859_1);
        Anexo a = notificacaoComAnexo(conteudo);
        RespostaCapturada r = new RespostaCapturada();
        anexo.doGet(RespostaCapturada.requisicao("/" + a.getId(), true), r.resposta);

        verify(r.resposta).setContentType("image/png");
        assertArrayEquals(conteudo, r.bytes());
    }

    @Test
    void downloadDeAnexoInvalido() throws Exception {
        RespostaCapturada inexistente = new RespostaCapturada();
        anexo.doGet(RespostaCapturada.requisicao("/999999", true), inexistente.resposta);
        verify(inexistente.resposta).sendError(HttpServletResponse.SC_NOT_FOUND);

        RespostaCapturada naoNumerico = new RespostaCapturada();
        anexo.doGet(RespostaCapturada.requisicao("/abc", true), naoNumerico.resposta);
        verify(naoNumerico.resposta).sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    // ---- QR Codes (/qr e /n/{tipo}) ----

    @Test
    void qrCodeDeCadaCanalEhUmPng() throws Exception {
        for (TipoNotificacao tipo : TipoNotificacao.values()) {
            HttpServletRequest req = RespostaCapturada.requisicao(null, false);
            when(req.getParameter("tipo")).thenReturn(tipo.getChaveUrl());
            when(req.getScheme()).thenReturn("http");
            when(req.getServerName()).thenReturn("localhost");
            when(req.getServerPort()).thenReturn(8080);
            RespostaCapturada r = new RespostaCapturada();

            new QrCodeServlet().doGet(req, r.resposta);

            verify(r.resposta).setContentType("image/png");
            byte[] png = r.bytes();
            assertTrue(png.length > 100, tipo.name());
            assertEquals((byte) 0x89, png[0]);
            assertEquals("PNG", new String(png, 1, 3, StandardCharsets.US_ASCII));
        }
    }

    @Test
    void qrCodeDeTipoInvalidoDa404() throws Exception {
        HttpServletRequest req = RespostaCapturada.requisicao(null, false);
        when(req.getParameter("tipo")).thenReturn("hack");
        RespostaCapturada r = new RespostaCapturada();
        new QrCodeServlet().doGet(req, r.resposta);
        verify(r.resposta).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    void urlCurtaDoCartazLevaAoWizardDoTipo() throws Exception {
        RespostaCapturada r = new RespostaCapturada();
        new QrRedirectServlet().doGet(RespostaCapturada.requisicao("/farmaco", false), r.resposta);
        verify(r.resposta).sendRedirect("/notificar.zul?tipo=farmaco");

        RespostaCapturada invalida = new RespostaCapturada();
        new QrRedirectServlet().doGet(RespostaCapturada.requisicao("/xyz", false), invalida.resposta);
        verify(invalida.resposta).sendRedirect("/index.zul");
        verify(invalida.resposta, never()).setStatus(anyInt());
    }
}
