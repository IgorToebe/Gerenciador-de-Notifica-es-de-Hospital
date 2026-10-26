package br.com.hospital.notificacao.integracao;

import br.com.hospital.notificacao.model.AcaoPlano;
import br.com.hospital.notificacao.model.HistoricoStatus;
import br.com.hospital.notificacao.model.Investigacao;
import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.Perfil;
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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
 * Aberto -> Em analise (triagem) -> Investigacao -> Plano de acao -> Concluido,
 * com as travas de etapa, de 5W2H completo e de perfil (so' gestor conclui).
 */
class FluxoCompletoIT extends IntegracaoBase {

    private final TriagemService triagem = new TriagemService();
    private final InvestigacaoService investigacao = new InvestigacaoService();
    private final PlanoAcaoService plano = new PlanoAcaoService();

    private long id;
    private long gestor;
    private long analista;

    @BeforeEach
    void abrir() {
        id = criarId(requisicao(TipoNotificacao.PROBLEMAS_ASSISTENCIAIS));
        gestor = criarUsuario("ana.qualidade", Perfil.NSP_GESTOR, "Senha@1");
        analista = criarUsuario("carlos.nsp", Perfil.NSP_ANALISTA, "Senha@1");
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

    /** Leva a notificacao ate o plano de acao, com a acao 1.0 gerada a partir da causa raiz. */
    private void ateOPlano() {
        triagem.aplicarClassificacao(id, "Dano grave", 3, 8, analista);
        triagem.iniciarInvestigacao(id, analista);
        investigacao.gerarPlanoDeAcao(investigacaoComCausaRaiz(), "UTI Adulto", analista);
    }

    /** Preenche o 5W2H da acao e a marca como concluida. */
    private void concluirAcao(AcaoPlano a) {
        a.setoQue("Trocar grades das camas da UTI");
        a.setQuem("Engenharia clínica");
        a.setOnde("UTI Adulto");
        a.setComo("Substituição preventiva");
        a.setDataInicio("01/10/2026");
        a.setDataFim("15/10/2026");
        plano.atualizarCampos(a);
        plano.atualizarStatus(a.getId(), StatusAcao.CONCLUIDO);
    }

    @Test
    void percorreTodasAsEtapasAteAConclusao() {
        NivelRisco nivel = triagem.aplicarClassificacao(id, "Dano grave", 3, 8, analista); // 24 = teto do Alto
        assertEquals(NivelRisco.ALTO, nivel);
        assertTrue(triagem.requerAlerta(nivel));
        assertEquals(StatusNotificacao.TRIAGEM, status());

        triagem.iniciarInvestigacao(id, analista);
        assertEquals(StatusNotificacao.INVESTIGACAO, status());
        assertEquals(1, contar("SELECT COUNT(*) FROM INVESTIGACAO WHERE ID_NOTIFICACAO = ?", id));

        Investigacao inv = investigacaoComCausaRaiz();
        investigacao.adicionarMarco(inv.getId(), "12/10 14:30", "Queda do leito");
        investigacao.adicionarMarco(inv.getId(), "12/10 14:35", "Equipe acionada");
        investigacao.gerarPlanoDeAcao(inv, "UTI Adulto", analista);
        assertEquals(StatusNotificacao.PLANO, status());

        // A 1a acao nasce da causa raiz
        List<AcaoPlano> acoes = plano.listar(id);
        assertEquals(1, acoes.size());
        assertEquals("Grade da cama sem manutenção", acoes.get(0).getPorque());
        assertEquals("UTI Adulto", acoes.get(0).getOnde());
        assertEquals(StatusAcao.INICIAR, acoes.get(0).getStatus());

        plano.adicionarAcao(id, "Grade da cama sem manutenção", "UTI Adulto");
        acoes = plano.listar(id);
        assertEquals("2.0", acoes.get(1).getCodigo());

        concluirAcao(acoes.get(0));
        plano.atualizarEvidencia(acoes.get(0).getId(), "OS 1234");
        assertFalse(plano.podeConcluir(id), "ainda há ação em aberto");
        assertThrows(IllegalStateException.class, () -> plano.concluirNotificacao(id, gestor));

        plano.atualizarStatus(acoes.get(1).getId(), StatusAcao.CANCELADO);
        assertTrue(plano.podeConcluir(id), "cancelada conta como finalizada, mesmo sem 5W2H");
        plano.concluirNotificacao(id, gestor);
        assertEquals(StatusNotificacao.CONCLUIDO, status());

        List<StatusNotificacao> sequencia = notificacaoService.carregarDetalhe(id).getHistorico().stream()
                .map(HistoricoStatus::getStatusNovo).collect(Collectors.toList());
        assertEquals(Arrays.asList(StatusNotificacao.ABERTO, StatusNotificacao.TRIAGEM,
                StatusNotificacao.INVESTIGACAO, StatusNotificacao.PLANO, StatusNotificacao.CONCLUIDO), sequencia);

        // Detalhe/PDF reunem tudo
        DetalheNotificacao d = notificacaoService.carregarDetalhe(id);
        assertNotNull(d.getInvestigacao());
        assertTrue(d.getInvestigacao().isConcluida());
        assertEquals(2, d.getInvestigacao().getTimeline().size());
        assertEquals("Equipe acionada", d.getInvestigacao().getTimeline().get(1).getDescricao());
        assertEquals("OS 1234", d.getAcoes().get(0).getEvidencia());
        assertEquals("Trocar grades das camas da UTI", d.getAcoes().get(0).getoQue());

        RelatorioNotificacaoService relatorio = new RelatorioNotificacaoService();
        String html = relatorio.montarHtml("Teste", Collections.singletonList(d), null);
        assertTrue(html.contains("Grade da cama sem manutenção"));
        assertTrue(html.contains("OS 1234"));
        assertTrue(html.contains("Equipe acionada"));
        byte[] pdf = relatorio.gerarPdf(html);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    // ---- travas de etapa ----

    @Test
    void matrizDeRiscoSoPodeSerAplicadaAntesDaInvestigacao() {
        triagem.aplicarClassificacao(id, "Dano leve", 1, 1, analista);
        triagem.iniciarInvestigacao(id, analista);

        assertThrows(IllegalStateException.class, () -> triagem.aplicarClassificacao(id, "Dano leve", 5, 16, analista));
        assertEquals(NivelRisco.BAIXO, notificacaoService.carregarCompleta(id).getNivelRisco());
        assertEquals(StatusNotificacao.INVESTIGACAO, status());
    }

    @Test
    void investigacaoNaoPodeSerIniciadaDuasVezes() {
        triagem.iniciarInvestigacao(id, analista);
        long primeira = investigacao.carregarOuCriar(id).getId();

        assertThrows(IllegalStateException.class, () -> triagem.iniciarInvestigacao(id, analista));
        assertEquals(primeira, investigacao.carregarOuCriar(id).getId());
        assertEquals(1, contar("SELECT COUNT(*) FROM INVESTIGACAO WHERE ID_NOTIFICACAO = ?", id));
    }

    @Test
    void planoSoEhGeradoAPartirDaInvestigacao() {
        // ainda em ABERTO
        assertThrows(IllegalStateException.class,
                () -> investigacao.gerarPlanoDeAcao(investigacaoComCausaRaiz(), "UTI Adulto", analista));

        ateOPlano();
        assertThrows(IllegalStateException.class,
                () -> investigacao.gerarPlanoDeAcao(investigacaoComCausaRaiz(), "UTI Adulto", analista),
                "gerar de novo não duplica a ação inicial");
        assertEquals(1, plano.listar(id).size());
    }

    @Test
    void semCausaRaizNaoGeraPlano() {
        triagem.iniciarInvestigacao(id, analista);
        Investigacao inv = investigacao.carregarOuCriar(id);
        inv.setCausaRaiz("   ");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> investigacao.gerarPlanoDeAcao(inv, "UTI Adulto", analista));
        assertTrue(e.getMessage().contains("causa raiz"));
        assertEquals(StatusNotificacao.INVESTIGACAO, status());
        assertTrue(plano.listar(id).isEmpty());
    }

    @Test
    void rascunhoDaInvestigacaoEhSalvoSemAvancarOFluxo() {
        triagem.iniciarInvestigacao(id, analista);
        Investigacao inv = investigacaoComCausaRaiz();
        inv.setFatorAmbiente("Iluminação noturna insuficiente");
        investigacao.salvarRascunho(inv);

        Investigacao lida = investigacao.carregarOuCriar(id);
        assertEquals("Iluminação noturna insuficiente", lida.getFatorAmbiente());
        assertEquals("Grade da cama sem manutenção", lida.getCausaRaiz());
        assertFalse(lida.isConcluida());
        assertEquals(StatusNotificacao.INVESTIGACAO, status());
    }

    // ---- conclusao: 5W2H completo e perfil gestor ----

    @Test
    void acaoConcluidaComCincoW2hIncompletoImpedeAConclusao() {
        ateOPlano();
        AcaoPlano a = plano.listar(id).get(0); // "Como?" nasce vazio
        plano.atualizarStatus(a.getId(), StatusAcao.CONCLUIDO);

        assertFalse(plano.podeConcluir(id));
        assertThrows(IllegalStateException.class, () -> plano.concluirNotificacao(id, gestor));
        assertEquals(StatusNotificacao.PLANO, status());
    }

    @Test
    void somenteGestorAtivoConcluiANotificacao() {
        ateOPlano();
        concluirAcao(plano.listar(id).get(0));
        assertTrue(plano.podeConcluir(id));

        assertThrows(SecurityException.class, () -> plano.concluirNotificacao(id, analista));
        assertThrows(SecurityException.class, () -> plano.concluirNotificacao(id, null));
        executar("UPDATE USUARIO SET ATIVO = 0 WHERE ID = ?", gestor);
        assertThrows(SecurityException.class, () -> plano.concluirNotificacao(id, gestor));
        assertEquals(StatusNotificacao.PLANO, status());

        executar("UPDATE USUARIO SET ATIVO = 1 WHERE ID = ?", gestor);
        plano.concluirNotificacao(id, gestor);
        assertEquals(StatusNotificacao.CONCLUIDO, status());
    }

    @Test
    void semAcoesNaoDaParaConcluir() {
        assertFalse(plano.podeConcluir(id));
        assertThrows(IllegalStateException.class, () -> plano.concluirNotificacao(id, gestor));
    }
}
