package br.com.hospital.notificacao.integracao;

import br.com.hospital.notificacao.model.HistoricoStatus;
import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.model.TipoNotificacao;
import br.com.hospital.notificacao.service.ReclassificacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Painel unico de reclassificacao (ReclassificacaoService) gravando de verdade no banco. */
class ReclassificacaoIT extends IntegracaoBase {

    private final ReclassificacaoService service = new ReclassificacaoService();
    private long id;
    private String protocoloOriginal;

    @BeforeEach
    void abrirNotificacaoDeTecnovigilancia() {
        protocoloOriginal = criar(requisicao(TipoNotificacao.TECNOVIGILANCIA)).getProtocolo();
        id = idPorProtocolo(protocoloOriginal);
    }

    private ReclassificacaoService.Reclassificacao comoEsta() {
        Notificacao n = notificacaoService.carregarCompleta(id);
        ReclassificacaoService.Reclassificacao r = new ReclassificacaoService.Reclassificacao();
        r.tipo = n.getTipo();
        r.subgrupo = n.getSubgrupo();
        r.gravidade = n.getGravidade();
        r.descricaoRevisada = n.getDescricaoRevisada();
        return r;
    }

    private List<HistoricoStatus> historico() {
        return notificacaoService.carregarDetalhe(id).getHistorico();
    }

    @Test
    void trocaTipoProblemaEClassificacaoMantendoProtocoloEDescricaoOriginal() {
        ReclassificacaoService.Reclassificacao r = comoEsta();
        r.tipo = TipoNotificacao.FARMACOVIGILANCIA;
        r.subgrupo = "Erro de medicação";
        r.gravidade = "Dano moderado";
        r.descricaoRevisada = "  Medicamento errado administrado por troca de etiqueta  ";

        assertTrue(service.reclassificar(id, r, null));

        Notificacao n = notificacaoService.carregarCompleta(id);
        assertEquals(TipoNotificacao.FARMACOVIGILANCIA, n.getTipo());
        assertEquals("Erro de medicação", n.getSubgrupo());
        assertEquals("Dano moderado", n.getGravidade());
        assertEquals("Medicamento errado administrado por troca de etiqueta", n.getDescricaoRevisada());
        assertEquals("Descrição do incidente de teste", n.getDescricao(), "a descrição original nunca muda");
        assertEquals(protocoloOriginal, n.getProtocolo());
        assertEquals(n.getDescricaoRevisada(), n.getDescricaoVigente());
    }

    @Test
    void registraNoHistoricoOQueMudouEIniciaATriagem() {
        ReclassificacaoService.Reclassificacao r = comoEsta();
        r.subgrupo = "Implante / prótese";
        service.reclassificar(id, r, null);

        List<HistoricoStatus> h = historico();
        assertEquals(2, h.size());
        HistoricoStatus ultimo = h.get(1);
        assertEquals(StatusNotificacao.ABERTO, ultimo.getStatusAnterior());
        assertEquals(StatusNotificacao.TRIAGEM, ultimo.getStatusNovo());
        assertEquals("Reclassificada: Problema Equipamento médico → Implante / prótese", ultimo.getObservacao());
        assertEquals(StatusNotificacao.TRIAGEM, notificacaoService.carregarCompleta(id).getStatus());
    }

    @Test
    void foraDaTriagemNaoMudaOStatus() {
        executar("UPDATE NOTIFICACAO SET STATUS = 'INVESTIGACAO' WHERE ID = ?", id);
        ReclassificacaoService.Reclassificacao r = comoEsta();
        r.gravidade = "Óbito";
        service.reclassificar(id, r, null);

        assertEquals(StatusNotificacao.INVESTIGACAO, notificacaoService.carregarCompleta(id).getStatus());
        HistoricoStatus ultimo = historico().get(1);
        assertEquals(StatusNotificacao.INVESTIGACAO, ultimo.getStatusAnterior());
        assertEquals(StatusNotificacao.INVESTIGACAO, ultimo.getStatusNovo());
    }

    @Test
    void reclassificaORiscoQuandoProbabilidadeEImpactoSaoInformados() {
        ReclassificacaoService.Reclassificacao r = comoEsta();
        r.probabilidade = 5;
        r.impacto = 16;
        service.reclassificar(id, r, null);

        Notificacao n = notificacaoService.carregarCompleta(id);
        assertEquals(Integer.valueOf(80), n.getScoreRisco());
        assertEquals(NivelRisco.EXTREMO, n.getNivelRisco());
        assertTrue(historico().get(1).getObservacao().contains("Risco Médio (8) → Extremo (80)"));
    }

    @Test
    void semRiscoInformadoORiscoAtualEhPreservado() {
        ReclassificacaoService.Reclassificacao r = comoEsta();
        r.gravidade = "Dano grave";
        service.reclassificar(id, r, null);
        assertEquals(NivelRisco.MEDIO, notificacaoService.carregarCompleta(id).getNivelRisco());
    }

    @Test
    void semNenhumaMudancaNaoGravaNada() {
        assertFalse(service.reclassificar(id, comoEsta(), null));
        assertEquals(1, historico().size());
        assertEquals(StatusNotificacao.ABERTO, notificacaoService.carregarCompleta(id).getStatus());
    }

    @Test
    void apagarADescricaoRevisadaVoltaAValerAOriginal() {
        ReclassificacaoService.Reclassificacao r = comoEsta();
        r.descricaoRevisada = "Revisão";
        service.reclassificar(id, r, null);
        r = comoEsta();
        r.descricaoRevisada = "";
        assertTrue(service.reclassificar(id, r, null));

        Notificacao n = notificacaoService.carregarCompleta(id);
        assertNull(n.getDescricaoRevisada());
        assertEquals("Descrição do incidente de teste", n.getDescricaoVigente());
    }

    @Test
    void dadosInvalidosNaoAlteramANotificacao() {
        ReclassificacaoService.Reclassificacao r = comoEsta();
        r.tipo = TipoNotificacao.NEAR_MISS_MATERNO; // subgrupo continua sendo de Tecnovigilancia
        assertThrows(IllegalArgumentException.class, () -> service.reclassificar(id, r, null));

        assertEquals(TipoNotificacao.TECNOVIGILANCIA, notificacaoService.carregarCompleta(id).getTipo());
        assertEquals(1, historico().size());
    }

    @Test
    void notificacaoInexistente() {
        ReclassificacaoService.Reclassificacao r = comoEsta();
        assertThrows(IllegalArgumentException.class, () -> service.reclassificar(999_999, r, null));
    }

    @Test
    void reclassificacaoAparecaNosFiltrosDaLista() {
        ReclassificacaoService.Reclassificacao r = comoEsta();
        r.tipo = TipoNotificacao.FARMACOVIGILANCIA;
        r.subgrupo = "Desvio de qualidade";
        service.reclassificar(id, r, null);

        br.com.hospital.notificacao.dao.NotificacaoDAO.Filtro f = new br.com.hospital.notificacao.dao.NotificacaoDAO.Filtro();
        f.tipo = TipoNotificacao.FARMACOVIGILANCIA;
        f.subgrupo = "Desvio de qualidade";
        assertEquals(1, notificacaoService.listar(f).total);
        f.tipo = TipoNotificacao.TECNOVIGILANCIA;
        f.subgrupo = null;
        assertEquals(0, notificacaoService.listar(f).total);
    }
}
