package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.model.CampoNotificacao;
import br.com.hospital.notificacao.model.HistoricoStatus;
import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.model.TipoNotificacao;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioNotificacaoServiceTest {

    private final RelatorioNotificacaoService service = new RelatorioNotificacaoService();

    private DetalheNotificacao detalhe() {
        Notificacao n = new Notificacao();
        n.setId(4821);
        n.setProtocolo("#TC-2026-004821");
        n.setTipo(TipoNotificacao.TECNOVIGILANCIA);
        n.setStatus(StatusNotificacao.TRIAGEM);
        n.setSubgrupo("Equipamento médico");
        n.setGravidade("Dano leve");
        n.setProbabilidade(2);
        n.setImpacto(4);
        n.setScoreRisco(8);
        n.setNivelRisco(NivelRisco.MEDIO);
        n.setDescricao("Bomba <script>alert(1)</script> & parou");
        n.setDescricaoRevisada("Falha elétrica na bomba de infusão");
        n.setAnonimo(false);
        n.setContatoEmail("notificante@hospital.br");
        n.setContatoTelefone("44999998888");
        n.setCampos(Arrays.asList(new CampoNotificacao("registroAnvisa", "Registro ANVISA", "80146170001", 1)));

        HistoricoStatus h = new HistoricoStatus();
        h.setStatusAnterior(StatusNotificacao.ABERTO);
        h.setStatusNovo(StatusNotificacao.TRIAGEM);
        h.setObservacao("Reclassificada: Problema — → Equipamento médico");
        return new DetalheNotificacao(n, null, Collections.emptyList(), Collections.singletonList(h));
    }

    @Test
    void htmlEscapaConteudoDoNotificante() {
        String html = service.montarHtml("Teste", Collections.singletonList(detalhe()), null);
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("&amp; parou"));
    }

    @Test
    void htmlTrazDescricaoOriginalERevisadaEOsCamposPreenchidos() {
        String html = service.montarHtml("Teste", Collections.singletonList(detalhe()), null);
        assertTrue(html.contains("Bomba &lt;script&gt;"));
        assertTrue(html.contains("Falha elétrica na bomba de infusão"));
        assertTrue(html.contains("80146170001"));
        assertTrue(html.contains("Equipamento médico"));
    }

    @Test
    void htmlNuncaIncluiContatoDoNotificante() {
        String html = service.montarHtml("Teste", Collections.singletonList(detalhe()), null);
        assertFalse(html.contains("notificante@hospital.br"));
        assertFalse(html.contains("44999998888"));
    }

    @Test
    void simbolosForaDoWinAnsiSaoSubstituidos() {
        assertEquals("A -> B", RelatorioNotificacaoService.paraWinAnsi("A → B"));
        assertEquals("ok ?", RelatorioNotificacaoService.paraWinAnsi("ok 😀"));
        assertEquals("Ação · Óbito — ×", RelatorioNotificacaoService.paraWinAnsi("Ação · Óbito — ×"));
    }

    @Test
    void geraUmPdfValido() {
        byte[] pdf = service.gerarPdf(service.montarHtml("Teste", Collections.singletonList(detalhe()), "aviso"));
        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }
}
