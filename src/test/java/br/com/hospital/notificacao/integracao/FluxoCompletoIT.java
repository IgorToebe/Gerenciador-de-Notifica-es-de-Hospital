package br.com.hospital.notificacao.integracao;

import br.com.hospital.notificacao.model.AcaoPlano;
import br.com.hospital.notificacao.model.HistoricoStatus;
import br.com.hospital.notificacao.model.Investigacao;
import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.StatusAcao;
import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.model.TipoNotificacao;
import br.com.hospital.notificacao.service.DetalheNotificacao;
import br.com.hospital.notificacao.service.InvestigacaoService;
import br.com.hospital.notificacao.service.PlanoAcaoService;
import br.com.hospital.notificacao.service.RelatorioNotificacaoService;
import br.com.hospital.notificacao.service.TriagemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fluxo obrigatorio de ponta a ponta (Item 4 dos criterios de aceitacao):
 * Aberto -> Em triagem -> Em investigacao -> Plano em execucao -> Concluido.
 */
class FluxoCompletoIT extends IntegracaoBase {

    private final TriagemService triagem = new TriagemService();
    private final InvestigacaoService investigacao = new InvestigacaoService();
    private final PlanoAcaoService plano = new PlanoAcaoService();

    private long id;

    @BeforeEach
    void abrir() {
        id = criarId(requisicao(TipoNotificacao.PROBLEMAS_ASSISTENCIAIS));
    }

    private StatusNotificacao status() {
        return notificacaoService.carregarCompleta(id).getStatus();
    }

    private Investigacao investigacaoComCausaRaiz() {
        Investigacao inv = investigacao.carregarOuCriar(id);
        inv.setParecerista("Ana");
        inv.setPorque1("Paciente caiu");
        inv.setCausaRaiz("Grade da cama sem manutenção");
        inv.setParecer("Evento evitável");
        return inv;
    }

    @Test
    void percorreTodasAsEtapasAteAConclusao() {
        // Triagem: matriz de risco
        NivelRisco nivel = triagem.aplicarClassificacao(id, "Dano grave", 3, 8, null); // 24 = teto do Alto
        assertEquals(NivelRisco.ALTO, nivel);
        assertTrue(triagem.requerAlerta(nivel));
        assertEquals(StatusNotificacao.TRIAGEM, status());

        // Investigacao
        triagem.iniciarInvestigacao(id, null);
        assertEquals(StatusNotificacao.INVESTIGACAO, status());
        assertEquals(1, contar("SELECT COUNT(*) FROM INVESTIGACAO WHERE ID_NOTIFICACAO = ?", id));

        Investigacao inv = investigacaoComCausaRaiz();
        investigacao.adicionarMarco(inv.getId(), "12/10 14:30", "Queda do leito");
        investigacao.adicionarMarco(inv.getId(), "12/10 14:35", "Equipe acionada");
        investigacao.gerarPlanoDeAcao(inv, "UTI Adulto", null);
        assertEquals(StatusNotificacao.PLANO, status());

        // Plano de acao: a 1a acao nasce da causa raiz
        List<AcaoPlano> acoes = plano.listar(id);
        assertEquals(1, acoes.size());
        assertEquals("Grade da cama sem manutenção", acoes.get(0).getPorque());
        assertEquals("UTI Adulto", acoes.get(0).getOnde());
        assertEquals(StatusAcao.INICIAR, acoes.get(0).getStatus());

        plano.adicionarAcao(id, "Grade da cama sem manutenção", "UTI Adulto");
        acoes = plano.listar(id);
        assertEquals("2.0", acoes.get(1).getCodigo());

        plano.atualizarStatus(acoes.get(0).getId(), StatusAcao.CONCLUIDO);
        plano.atualizarEvidencia(acoes.get(0).getId(), "OS 1234");
        assertFalse(plano.podeConcluir(id), "ainda há ação em aberto");
        assertThrows(IllegalStateException.class, () -> plano.concluirNotificacao(id, null));

        plano.atualizarStatus(acoes.get(1).getId(), StatusAcao.CANCELADO);
        assertTrue(plano.podeConcluir(id), "cancelada também conta como finalizada");
        plano.concluirNotificacao(id, null);
        assertEquals(StatusNotificacao.CONCLUIDO, status());

        // Historico registra a sequencia completa
        List<StatusNotificacao> sequencia = notificacaoService.carregarDetalhe(id).getHistorico().stream()
                .map(HistoricoStatus::getStatusNovo).collect(Collectors.toList());
        assertEquals(java.util.Arrays.asList(StatusNotificacao.ABERTO, StatusNotificacao.TRIAGEM,
                StatusNotificacao.INVESTIGACAO, StatusNotificacao.PLANO, StatusNotificacao.CONCLUIDO), sequencia);

        // Detalhe/PDF reunem tudo
        DetalheNotificacao d = notificacaoService.carregarDetalhe(id);
        assertNotNull(d.getInvestigacao());
        assertTrue(d.getInvestigacao().isConcluida());
        assertEquals(2, d.getInvestigacao().getTimeline().size());
        assertEquals("Equipe acionada", d.getInvestigacao().getTimeline().get(1).getDescricao());
        assertEquals("OS 1234", d.getAcoes().get(0).getEvidencia());

        RelatorioNotificacaoService relatorio = new RelatorioNotificacaoService();
        String html = relatorio.montarHtml("Teste", Collections.singletonList(d), null);
        assertTrue(html.contains("Grade da cama sem manutenção"));
        assertTrue(html.contains("OS 1234"));
        assertTrue(html.contains("Equipe acionada"));
        byte[] pdf = relatorio.gerarPdf(html);
        assertEquals("%PDF", new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Test
    void reaplicarAMatrizNaTriagemNaoVoltaOStatus() {
        triagem.aplicarClassificacao(id, "Dano leve", 1, 1, null);
        triagem.iniciarInvestigacao(id, null);
        triagem.aplicarClassificacao(id, "Dano leve", 5, 16, null);

        Notificacao n = notificacaoService.carregarCompleta(id);
        assertEquals(StatusNotificacao.INVESTIGACAO, n.getStatus());
        assertEquals(NivelRisco.EXTREMO, n.getNivelRisco());
    }

    @Test
    void semCausaRaizNaoGeraPlano() {
        triagem.iniciarInvestigacao(id, null);
        Investigacao inv = investigacao.carregarOuCriar(id);
        inv.setCausaRaiz("   ");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> investigacao.gerarPlanoDeAcao(inv, "UTI Adulto", null));
        assertTrue(e.getMessage().contains("causa raiz"));
        assertEquals(StatusNotificacao.INVESTIGACAO, status());
        assertTrue(plano.listar(id).isEmpty());
    }

    @Test
    void rascunhoDaInvestigacaoEhSalvoSemAvancarOFluxo() {
        triagem.iniciarInvestigacao(id, null);
        Investigacao inv = investigacaoComCausaRaiz();
        inv.setFatorAmbiente("Iluminação noturna insuficiente");
        investigacao.salvarRascunho(inv);

        Investigacao lida = investigacao.carregarOuCriar(id);
        assertEquals("Iluminação noturna insuficiente", lida.getFatorAmbiente());
        assertEquals("Grade da cama sem manutenção", lida.getCausaRaiz());
        assertFalse(lida.isConcluida());
        assertEquals(StatusNotificacao.INVESTIGACAO, status());
    }

    @Test
    void investigacaoEhUnicaPorNotificacao() {
        triagem.iniciarInvestigacao(id, null);
        long primeira = investigacao.carregarOuCriar(id).getId();
        triagem.iniciarInvestigacao(id, null);
        assertEquals(primeira, investigacao.carregarOuCriar(id).getId());
        assertEquals(1, contar("SELECT COUNT(*) FROM INVESTIGACAO WHERE ID_NOTIFICACAO = ?", id));
    }

    @Test
    void gerarPlanoDuasVezesNaoDuplicaAPrimeiraAcao() {
        triagem.iniciarInvestigacao(id, null);
        Investigacao inv = investigacaoComCausaRaiz();
        investigacao.gerarPlanoDeAcao(inv, "UTI Adulto", null);
        investigacao.gerarPlanoDeAcao(inv, "UTI Adulto", null);
        assertEquals(1, plano.listar(id).size());
    }

    @Test
    void semAcoesNaoDaParaConcluir() {
        assertFalse(plano.podeConcluir(id));
        assertThrows(IllegalStateException.class, () -> plano.concluirNotificacao(id, null));
    }
}
