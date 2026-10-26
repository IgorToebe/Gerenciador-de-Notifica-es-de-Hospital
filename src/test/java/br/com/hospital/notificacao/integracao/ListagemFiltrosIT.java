package br.com.hospital.notificacao.integracao;

import br.com.hospital.notificacao.dao.NotificacaoDAO;
import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.model.TipoNotificacao;
import br.com.hospital.notificacao.service.NovaNotificacaoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lista de notificacoes: cada filtro, a ordenacao, a paginacao e os contadores do painel. */
class ListagemFiltrosIT extends IntegracaoBase {

    private long queda, flebite, erroMedicacao, reacao, bomba;

    /**
     * Cenario fixo (prontuario 111 se repete em duas notificacoes):
     * <pre>
     * queda          Assist   Queda              Dano grave          Extremo(80)  111  UTI Adulto   INVESTIGACAO  2026-01-10
     * flebite        Assist   Flebite            Dano leve           Baixo(2)     222  Enfermaria   ABERTO        2026-02-15
     * erroMedicacao  Farmaco  Erro de medicação  Near miss           Alto(16)     111  Farmácia     TRIAGEM       2026-03-20
     * reacao         Farmaco  Reação adversa     Óbito               Medio(8)     ---  UTI Adulto   CONCLUIDO     2026-03-25
     * bomba          Tecno    Equipamento médico Incidente sem dano  Medio(8)     333  UTI Neonatal ABERTO        hoje
     * </pre>
     */
    @BeforeEach
    void montarCenario() {
        queda = criarCom(TipoNotificacao.PROBLEMAS_ASSISTENCIAIS, "Queda", "Dano grave", "111", "UTI Adulto",
                "Paciente caiu do leito");
        flebite = criarCom(TipoNotificacao.PROBLEMAS_ASSISTENCIAIS, "Flebite", "Dano leve", "222", "Enfermaria",
                "Acesso venoso com sinais flogísticos");
        erroMedicacao = criarCom(TipoNotificacao.FARMACOVIGILANCIA, "Erro de medicação", "Near miss (quase erro)", "111",
                "Farmácia", "Dose dobrada interceptada");
        reacao = criarCom(TipoNotificacao.FARMACOVIGILANCIA, "Reação adversa", "Óbito", "", "UTI Adulto",
                "Choque anafilático");
        bomba = criarCom(TipoNotificacao.TECNOVIGILANCIA, "Equipamento médico", "Incidente sem dano", "333",
                "UTI Neonatal", "Incubadora sem controle de temperatura");

        risco(queda, 5, 16);
        risco(flebite, 1, 2);
        risco(erroMedicacao, 2, 8);
        status(queda, StatusNotificacao.INVESTIGACAO);
        status(erroMedicacao, StatusNotificacao.TRIAGEM);
        status(reacao, StatusNotificacao.CONCLUIDO);
        recebidaEm(queda, "2026-01-10 09:00:00");
        recebidaEm(flebite, "2026-02-15 23:59:59");
        recebidaEm(erroMedicacao, "2026-03-20 00:00:00");
        recebidaEm(reacao, "2026-03-25 12:00:00");
        executar("UPDATE NOTIFICACAO SET DESCRICAO_REVISADA = 'Queda por grade abaixada' WHERE ID = ?", queda);
    }

    private long criarCom(TipoNotificacao tipo, String subgrupo, String gravidade, String prontuario, String setor,
                          String descricao) {
        NovaNotificacaoRequest req = requisicao(tipo);
        req.setSubgrupo(subgrupo);
        req.setGravidade(gravidade);
        req.setProntuario(prontuario);
        req.setSetor(setor);
        req.setDescricao(descricao);
        return criarId(req);
    }

    private void risco(long id, int prob, int imp) {
        int score = prob * imp;
        executar("UPDATE NOTIFICACAO SET PROBABILIDADE = ?, IMPACTO = ?, SCORE_RISCO = ?, NIVEL_RISCO = ? WHERE ID = ?",
                prob, imp, score, NivelRisco.doScore(score).name(), id);
    }

    private void status(long id, StatusNotificacao s) {
        executar("UPDATE NOTIFICACAO SET STATUS = ? WHERE ID = ?", s.name(), id);
    }

    private void recebidaEm(long id, String quando) {
        executar("UPDATE NOTIFICACAO SET CRIADO_EM = ? WHERE ID = ?", Timestamp.valueOf(quando), id);
    }

    private List<Long> ids(NotificacaoDAO.Filtro f) {
        f.tamanhoPagina = 50;
        return notificacaoService.listar(f).itens.stream().map(Notificacao::getId).collect(Collectors.toList());
    }

    private List<Long> idsOrdenados(Long... ids) {
        return Arrays.asList(ids);
    }

    private static NotificacaoDAO.Filtro filtro() {
        return new NotificacaoDAO.Filtro();
    }

    @Test
    void semFiltroTrazTodasDaMaisRecenteParaAMaisAntiga() {
        assertEquals(idsOrdenados(bomba, reacao, erroMedicacao, flebite, queda), ids(filtro()));
    }

    @Test
    void filtraPorTipo() {
        NotificacaoDAO.Filtro f = filtro();
        f.tipo = TipoNotificacao.FARMACOVIGILANCIA;
        assertEquals(idsOrdenados(reacao, erroMedicacao), ids(f));
    }

    @Test
    void filtraPorProblema() {
        NotificacaoDAO.Filtro f = filtro();
        f.subgrupo = "Queda";
        assertEquals(idsOrdenados(queda), ids(f));
    }

    @Test
    void filtraPorClassificacao() {
        NotificacaoDAO.Filtro f = filtro();
        f.gravidade = "Óbito";
        assertEquals(idsOrdenados(reacao), ids(f));
    }

    @Test
    void filtraPorRisco() {
        NotificacaoDAO.Filtro f = filtro();
        f.nivelRisco = NivelRisco.MEDIO; // pre-classificacao padrao de quem ainda nao foi triado
        assertEquals(idsOrdenados(bomba, reacao), ids(f));
        f.nivelRisco = NivelRisco.EXTREMO;
        assertEquals(idsOrdenados(queda), ids(f));
    }

    @Test
    void filtraPorPacienteComBuscaParcialDoProntuario() {
        NotificacaoDAO.Filtro f = filtro();
        f.prontuario = "111";
        assertEquals(idsOrdenados(erroMedicacao, queda), ids(f));
        f.prontuario = "3";
        assertEquals(idsOrdenados(bomba), ids(f));
    }

    @Test
    void filtraPorSetor() {
        NotificacaoDAO.Filtro f = filtro();
        f.setor = "UTI Adulto";
        assertEquals(idsOrdenados(reacao, queda), ids(f));
    }

    @Test
    void filtraPorStatus() {
        NotificacaoDAO.Filtro f = filtro();
        f.status = StatusNotificacao.ABERTO;
        assertEquals(idsOrdenados(bomba, flebite), ids(f));
    }

    @Test
    void filtraSomenteDuplicatasPorProntuario() {
        NotificacaoDAO.Filtro f = filtro();
        f.apenasDuplicatas = true;
        assertEquals(idsOrdenados(erroMedicacao, queda), ids(f));
        Map<String, Integer> dup = notificacaoService.contarDuplicatasPorProntuario();
        assertEquals(1, dup.size());
        assertEquals(Integer.valueOf(2), dup.get("111"));
    }

    @Test
    void periodoIncluiOsDiasInteirosDasDuasPontas() {
        NotificacaoDAO.Filtro f = filtro();
        f.dataDe = LocalDate.of(2026, 2, 15);
        f.dataAte = LocalDate.of(2026, 3, 20);
        assertEquals(idsOrdenados(erroMedicacao, flebite), ids(f));

        NotificacaoDAO.Filtro so = filtro();
        so.dataAte = LocalDate.of(2026, 1, 31);
        assertEquals(idsOrdenados(queda), ids(so));
    }

    @Test
    void buscaLivreProcuraTambemNoProblemaENaDescricaoRevisada() {
        NotificacaoDAO.Filtro f = filtro();
        f.texto = "grade abaixada";
        assertEquals(idsOrdenados(queda), ids(f));
        f.texto = "reação";
        assertEquals(idsOrdenados(reacao), ids(f), "busca no problema (subgrupo)");
        f.texto = "anafil";
        assertEquals(idsOrdenados(reacao), ids(f), "busca na descrição original, sem diferenciar maiúsculas");
    }

    @Test
    void combinaFiltros() {
        NotificacaoDAO.Filtro f = filtro();
        f.setor = "UTI Adulto";
        f.tipo = TipoNotificacao.PROBLEMAS_ASSISTENCIAIS;
        f.nivelRisco = NivelRisco.EXTREMO;
        assertEquals(idsOrdenados(queda), ids(f));
        f.nivelRisco = NivelRisco.BAIXO;
        assertTrue(ids(f).isEmpty());
    }

    @Test
    void ordenaPorMaiorRisco() {
        NotificacaoDAO.Filtro f = filtro();
        f.ordenacao = NotificacaoDAO.Ordenacao.MAIOR_RISCO;
        List<Long> ids = ids(f);
        assertEquals(queda, ids.get(0));        // 80
        assertEquals(erroMedicacao, ids.get(1)); // 16
        assertEquals(flebite, ids.get(4));      // 2
    }

    @Test
    void ordenaPorClassificacaoDoMaisGraveParaOMenosGrave() {
        NotificacaoDAO.Filtro f = filtro();
        f.ordenacao = NotificacaoDAO.Ordenacao.CLASSIFICACAO;
        assertEquals(idsOrdenados(reacao, queda, flebite, bomba, erroMedicacao), ids(f));
    }

    @Test
    void ordenaPorProblemaEmOrdemAlfabetica() {
        NotificacaoDAO.Filtro f = filtro();
        f.ordenacao = NotificacaoDAO.Ordenacao.PROBLEMA;
        assertEquals(idsOrdenados(bomba, erroMedicacao, flebite, queda, reacao), ids(f));
    }

    @Test
    void paginaOResultado() {
        NotificacaoDAO.Filtro f = filtro();
        f.tamanhoPagina = 2;
        f.pagina = 1;
        NotificacaoDAO.ResultadoPagina p1 = notificacaoService.listar(f);
        f.pagina = 3;
        NotificacaoDAO.ResultadoPagina p3 = notificacaoService.listar(f);

        assertEquals(5, p1.total);
        assertEquals(2, p1.itens.size());
        assertEquals(5, p3.total);
        assertEquals(1, p3.itens.size());
        assertEquals(queda, p3.itens.get(0).getId());
    }

    @Test
    void contadoresDoPainel() {
        Map<StatusNotificacao, Integer> porStatus = notificacaoService.contarPorStatus();
        assertEquals(Integer.valueOf(2), porStatus.get(StatusNotificacao.ABERTO));
        assertEquals(Integer.valueOf(1), porStatus.get(StatusNotificacao.CONCLUIDO));
        assertEquals(Integer.valueOf(0), porStatus.get(StatusNotificacao.PLANO));

        Map<TipoNotificacao, Integer> porTipo = notificacaoService.contarPorTipo();
        assertEquals(Integer.valueOf(2), porTipo.get(TipoNotificacao.FARMACOVIGILANCIA));
        assertEquals(Integer.valueOf(0), porTipo.get(TipoNotificacao.NEAR_MISS_MATERNO));

        assertEquals(1, notificacaoService.contarCriadasHoje());
    }
}
