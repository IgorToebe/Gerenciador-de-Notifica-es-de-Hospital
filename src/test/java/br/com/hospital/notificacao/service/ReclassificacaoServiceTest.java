package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.TipoNotificacao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReclassificacaoServiceTest {

    private Notificacao notificacaoTecno() {
        Notificacao n = new Notificacao();
        n.setTipo(TipoNotificacao.TECNOVIGILANCIA);
        n.setSubgrupo("Equipamento médico");
        n.setGravidade("Incidente sem dano");
        n.setDescricao("Bomba de infusão parou");
        n.setProbabilidade(2);
        n.setImpacto(4);
        n.setScoreRisco(8);
        n.setNivelRisco(NivelRisco.MEDIO);
        return n;
    }

    private ReclassificacaoService.Reclassificacao semMudanca(Notificacao n) {
        ReclassificacaoService.Reclassificacao r = new ReclassificacaoService.Reclassificacao();
        r.tipo = n.getTipo();
        r.subgrupo = n.getSubgrupo();
        r.gravidade = n.getGravidade();
        r.descricaoRevisada = n.getDescricaoRevisada();
        return r;
    }

    @Test
    void semMudancaGeraTextoVazio() {
        Notificacao n = notificacaoTecno();
        assertEquals("", ReclassificacaoService.descreverMudancas(n, semMudanca(n)));
    }

    @Test
    void listaSomenteOQueMudou() {
        Notificacao n = notificacaoTecno();
        ReclassificacaoService.Reclassificacao r = semMudanca(n);
        r.tipo = TipoNotificacao.FARMACOVIGILANCIA;
        r.subgrupo = "Erro de medicação";
        r.probabilidade = 4;
        r.impacto = 8;

        String texto = ReclassificacaoService.descreverMudancas(n, r);
        assertEquals("Tipo Notificação de Tecnovigilância → Notificação de Farmacovigilância; "
                + "Problema Equipamento médico → Erro de medicação; Risco Médio (8) → Extremo (32)", texto);
    }

    @Test
    void descricaoRevisadaEmBrancoNaoContaComoMudanca() {
        Notificacao n = notificacaoTecno();
        ReclassificacaoService.Reclassificacao r = semMudanca(n);
        r.descricaoRevisada = "   ";
        assertEquals("", ReclassificacaoService.descreverMudancas(n, r));

        r.descricaoRevisada = "Falha elétrica no equipamento";
        assertTrue(ReclassificacaoService.descreverMudancas(n, r).contains("original mantida"));
    }

    @Test
    void observacaoRespeitaOTamanhoDaColuna() {
        StringBuilder longo = new StringBuilder();
        for (int i = 0; i < 700; i++) longo.append('x');
        assertEquals(ReclassificacaoService.LIMITE_OBSERVACAO, ReclassificacaoService.truncar(longo.toString()).length());
        assertEquals("curto", ReclassificacaoService.truncar("curto"));
    }

    @Test
    void validaSubgrupoDoTipoEClassificacao() {
        Notificacao n = notificacaoTecno();
        ReclassificacaoService.Reclassificacao r = semMudanca(n);
        r.tipo = TipoNotificacao.FARMACOVIGILANCIA; // subgrupo continua sendo de Tecnovigilancia
        assertThrows(IllegalArgumentException.class, () -> ReclassificacaoService.validar(r));

        ReclassificacaoService.Reclassificacao r2 = semMudanca(n);
        r2.gravidade = "Inventada";
        assertThrows(IllegalArgumentException.class, () -> ReclassificacaoService.validar(r2));

        ReclassificacaoService.Reclassificacao r3 = semMudanca(n);
        r3.probabilidade = 3; // sem impacto
        assertThrows(IllegalArgumentException.class, () -> ReclassificacaoService.validar(r3));
    }
}
