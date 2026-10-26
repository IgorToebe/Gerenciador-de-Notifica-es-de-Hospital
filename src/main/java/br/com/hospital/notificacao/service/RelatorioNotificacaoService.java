package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.model.AcaoPlano;
import br.com.hospital.notificacao.model.Anexo;
import br.com.hospital.notificacao.model.CampoNotificacao;
import br.com.hospital.notificacao.model.HistoricoStatus;
import br.com.hospital.notificacao.model.Investigacao;
import br.com.hospital.notificacao.model.MarcoInvestigacao;
import br.com.hospital.notificacao.model.Notificacao;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Documento (PDF) com tudo o que foi preenchido nas notificacoes: dados do wizard,
 * classificacao, descricao original e revisada, investigacao, plano de acao e historico.
 *
 * LGPD: o documento nunca inclui e-mail/telefone do notificante nem a senha de
 * acompanhamento — apenas se a notificacao e' anonima ou tem contato vinculado.
 *
 * O PDF usa as fontes padrao do PDFBox (Helvetica, codificacao WinAnsi); por isso todo
 * texto passa por {@link #paraWinAnsi(String)}, que troca simbolos fora dessa tabela.
 */
public class RelatorioNotificacaoService {

    private static final Charset WIN_ANSI = Charset.forName("windows-1252");

    /** Gera o XHTML do relatorio (uma secao por notificacao, com quebra de pagina entre elas). */
    public String montarHtml(String titulo, List<DetalheNotificacao> detalhes, String aviso) {
        SimpleDateFormat dataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        StringBuilder h = new StringBuilder();
        h.append("<html><head><meta charset=\"UTF-8\"/><style>")
         .append("@page{size:A4;margin:16mm 14mm 18mm;")
         .append("@bottom-right{content:\"Página \" counter(page) \" de \" counter(pages);font-family:Helvetica,sans-serif;font-size:8pt;color:#777}}")
         .append("body{font-family:Helvetica,sans-serif;font-size:9.5pt;color:#26231f}")
         .append("h1{font-size:15pt;margin:0 0 2mm}h2{font-size:12.5pt;margin:0 0 1mm}")
         .append("h3{font-size:10pt;text-transform:uppercase;letter-spacing:.5pt;color:#555;border-bottom:1px solid #ccc;")
         .append("padding-bottom:1mm;margin:5mm 0 2mm}")
         .append(".meta{color:#666;font-size:8.5pt;margin-bottom:3mm}")
         .append(".aviso{background:#fff4e0;border:1px solid #e8c88a;padding:2mm 3mm;margin:2mm 0 4mm}")
         .append("table{width:100%;border-collapse:collapse;margin-bottom:2mm}")
         .append("td,th{border:1px solid #ddd;padding:1.3mm 2mm;vertical-align:top;text-align:left}")
         .append("th{background:#f1eee8;font-size:8.5pt}td.k{width:32%;color:#555;background:#faf8f4}")
         .append(".texto{border:1px solid #ddd;background:#faf8f4;padding:2mm 3mm;white-space:pre-wrap}")
         .append(".notif{page-break-before:always}.notif.primeira{page-break-before:auto}")
         .append("</style></head><body>");

        h.append("<h1>").append(esc(titulo)).append("</h1>");
        h.append("<div class=\"meta\">NSP HU/UEM · Gerado em ").append(esc(dataHora.format(new Date())))
         .append(" · ").append(detalhes.size()).append(" notificação(ões) · Documento sigiloso (LGPD)</div>");
        if (aviso != null) {
            h.append("<div class=\"aviso\">").append(esc(aviso)).append("</div>");
        }
        if (detalhes.isEmpty()) {
            h.append("<p>Nenhuma notificação encontrada para os filtros informados.</p>");
        }

        boolean primeira = true;
        for (DetalheNotificacao d : detalhes) {
            secaoNotificacao(h, d, dataHora, primeira);
            primeira = false;
        }
        h.append("</body></html>");
        return h.toString();
    }

    private void secaoNotificacao(StringBuilder h, DetalheNotificacao d, SimpleDateFormat dataHora, boolean primeira) {
        Notificacao n = d.getNotificacao();
        h.append("<div class=\"notif").append(primeira ? " primeira" : "").append("\">");
        h.append("<h2>").append(esc(n.getProtocolo())).append(" · ").append(esc(n.getTipo().getTitulo())).append("</h2>");
        h.append("<div class=\"meta\">Notif. #").append(n.getId())
         .append(" · ").append(esc(n.getTipo().getCodigoFormulario()))
         .append(" · Status: ").append(esc(n.getStatus().getRotulo()))
         .append(" · Recebida em ").append(n.getCriadoEm() != null ? esc(dataHora.format(n.getCriadoEm())) : "—")
         .append(" · ").append(n.isAnonimo() ? "Anônima" : "Contato vinculado (não exportado)")
         .append("</div>");

        h.append("<h3>Classificação</h3><table>");
        linha(h, "Problema (" + n.getTipo().getRotuloSubgrupo().toLowerCase() + ")", n.getSubgrupo());
        linha(h, "Classificação do incidente", n.getGravidade());
        linha(h, "Risco", n.getNivelRisco() == null ? null
                : n.getNivelRisco().getRotulo() + " · pontuação " + n.getScoreRisco()
                  + " (probabilidade " + n.getProbabilidade() + " × impacto " + n.getImpacto() + ")");
        linha(h, "Gerou dano ao paciente", n.isGerouDano() ? "Sim" : "Não");
        h.append("</table>");

        h.append("<h3>Dados do incidente</h3><table>");
        linha(h, "Data do incidente", n.getDataIncidente());
        linha(h, "Hora aproximada", n.getHoraIncidente());
        linha(h, "Setor / unidade", n.getSetor());
        linha(h, "Prontuário", n.getProntuario());
        for (CampoNotificacao c : n.getCampos()) {
            linha(h, c.getRotulo(), c.getValor());
        }
        h.append("</table>");

        h.append("<h3>Descrição original (notificante)</h3>");
        texto(h, n.getDescricao());
        if (n.getDescricaoRevisada() != null && !n.getDescricaoRevisada().trim().isEmpty()) {
            h.append("<h3>Descrição revisada pelo NSP</h3>");
            texto(h, n.getDescricaoRevisada());
        }

        h.append("<h3>Anexos</h3>");
        if (n.getAnexos().isEmpty()) {
            h.append("<p>Nenhum anexo.</p>");
        } else {
            h.append("<ul>");
            for (Anexo a : n.getAnexos()) {
                h.append("<li>").append(esc(a.getNomeOriginal())).append(" (")
                 .append(Math.max(1, a.getTamanhoBytes() / 1024)).append(" KB)</li>");
            }
            h.append("</ul>");
        }

        Investigacao inv = d.getInvestigacao();
        if (inv != null) {
            h.append("<h3>Investigação · FOR-NSP-014</h3><table>");
            linha(h, "Parecerista", inv.getParecerista());
            linha(h, "Data da investigação", inv.getDataInvestigacao());
            linha(h, "Fontes de informação", inv.getFontes());
            linha(h, "Fator profissional", inv.getFatorProfissional());
            linha(h, "Comunicação", inv.getFatorComunicacao());
            linha(h, "Paciente / acompanhante", inv.getFatorPaciente());
            linha(h, "Trabalho / ambiente", inv.getFatorAmbiente());
            linha(h, "Organizacionais", inv.getFatorOrganizacional());
            linha(h, "Externos", inv.getFatorExterno());
            linha(h, "1º Por quê", inv.getPorque1());
            linha(h, "2º Por quê", inv.getPorque2());
            linha(h, "3º Por quê", inv.getPorque3());
            linha(h, "4º Por quê", inv.getPorque4());
            linha(h, "Causa raiz (5º Por quê)", inv.getCausaRaiz());
            linha(h, "Como foi identificado", inv.getDeteccao());
            linha(h, "Fatores atenuantes", inv.getAtenuantes());
            linha(h, "Parecer e conclusão do NSP", inv.getParecer());
            for (MarcoInvestigacao m : inv.getTimeline()) {
                linha(h, "Marco · " + nvl(m.getQuando()), m.getDescricao());
            }
            h.append("</table>");
        }

        if (!d.getAcoes().isEmpty()) {
            h.append("<h3>Plano de ação 5W2H</h3><table><tr><th>Cód.</th><th>O quê</th><th>Quem</th>")
             .append("<th>Onde</th><th>Por quê</th><th>Como</th><th>Quando</th><th>Status</th><th>Evidência</th></tr>");
            for (AcaoPlano a : d.getAcoes()) {
                h.append("<tr>");
                celula(h, a.getCodigo());
                celula(h, a.getoQue());
                celula(h, a.getQuem());
                celula(h, a.getOnde());
                celula(h, a.getPorque());
                celula(h, a.getComo());
                celula(h, nvl(a.getDataInicio()) + " a " + nvl(a.getDataFim()));
                celula(h, a.getStatus().getRotulo());
                celula(h, a.getEvidencia());
                h.append("</tr>");
            }
            h.append("</table>");
        }

        if (!d.getHistorico().isEmpty()) {
            h.append("<h3>Histórico</h3><table>");
            for (HistoricoStatus hs : d.getHistorico()) {
                String quando = hs.getCriadoEm() != null ? dataHora.format(hs.getCriadoEm()) : "";
                String status = (hs.getStatusAnterior() != null && hs.getStatusAnterior() != hs.getStatusNovo()
                        ? hs.getStatusAnterior().getRotulo() + " -> " : "") + hs.getStatusNovo().getRotulo();
                linha(h, quando, status + (hs.getObservacao() != null ? " · " + hs.getObservacao() : ""));
            }
            h.append("</table>");
        }
        h.append("</div>");
    }

    public byte[] gerarPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }

    private void linha(StringBuilder h, String chave, String valor) {
        h.append("<tr><td class=\"k\">").append(esc(chave)).append("</td><td>").append(esc(nvl(valor))).append("</td></tr>");
    }

    private void celula(StringBuilder h, String valor) {
        h.append("<td>").append(esc(nvl(valor))).append("</td>");
    }

    private void texto(StringBuilder h, String valor) {
        h.append("<div class=\"texto\">").append(esc(nvl(valor))).append("</div>");
    }

    private static String nvl(String valor) {
        return valor == null || valor.trim().isEmpty() ? "—" : valor;
    }

    /** Escape XML (o openhtmltopdf exige XHTML bem formado) + troca de simbolos fora do WinAnsi. */
    static String esc(String valor) {
        if (valor == null) return "";
        String texto = paraWinAnsi(valor);
        StringBuilder sb = new StringBuilder(texto.length());
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default:
                    // Caracteres de controle (exceto quebra de linha/tab) tornam o XML invalido.
                    if (c < 0x20 && c != '\n' && c != '\r' && c != '\t') continue;
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    static String paraWinAnsi(String valor) {
        String texto = valor.replace("→", "->").replace("←", "<-");
        CharsetEncoder encoder = WIN_ANSI.newEncoder();
        StringBuilder sb = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); ) {
            int cp = texto.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            sb.append(encoder.canEncode(ch) ? ch : "?");
            i += Character.charCount(cp);
        }
        return sb.toString();
    }
}
