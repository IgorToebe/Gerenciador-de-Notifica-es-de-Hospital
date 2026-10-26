package br.com.hospital.notificacao.dao;

import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.TipoNotificacao;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacaoDAOFiltroTest {

    private static long contarPlaceholders(String sql) {
        return sql.chars().filter(c -> c == '?').count();
    }

    @Test
    void filtroVazioNaoTemParametros() {
        List<Object> params = new ArrayList<>();
        String where = NotificacaoDAO.montarWhere(new NotificacaoDAO.Filtro(), params);
        assertEquals(" WHERE 1=1 ", where);
        assertTrue(params.isEmpty());
    }

    @Test
    void filtrosAvancadosGeramCondicoesEParametrosNaMesmaOrdem() {
        NotificacaoDAO.Filtro f = new NotificacaoDAO.Filtro();
        f.tipo = TipoNotificacao.FARMACOVIGILANCIA;
        f.subgrupo = "Erro de medicação";
        f.gravidade = "Dano leve";
        f.nivelRisco = NivelRisco.ALTO;
        f.prontuario = " 123 ";
        f.setor = "UTI Adulto";
        f.dataDe = LocalDate.of(2026, 1, 1);
        f.dataAte = LocalDate.of(2026, 1, 31);

        List<Object> params = new ArrayList<>();
        String where = NotificacaoDAO.montarWhere(f, params);

        assertEquals(contarPlaceholders(where), params.size());
        assertEquals(Arrays.asList("FARMACOVIGILANCIA", "Erro de medicação", "Dano leve", "ALTO", "%123%", "UTI Adulto",
                Timestamp.valueOf("2026-01-01 00:00:00"), Timestamp.valueOf("2026-02-01 00:00:00")), params);
        assertTrue(where.contains("SUBGRUPO = ?"));
        assertTrue(where.contains("CRIADO_EM < ?"), "data final inclui o dia inteiro");
    }

    @Test
    void buscaLivreTambemProcuraNoProblemaENaDescricaoRevisada() {
        NotificacaoDAO.Filtro f = new NotificacaoDAO.Filtro();
        f.texto = "queda";
        List<Object> params = new ArrayList<>();
        String where = NotificacaoDAO.montarWhere(f, params);
        assertTrue(where.contains("UPPER(SUBGRUPO) LIKE ?"));
        assertTrue(where.contains("UPPER(DESCRICAO_REVISADA) LIKE ?"));
        assertEquals(contarPlaceholders(where), params.size());
        assertTrue(params.stream().allMatch("%QUEDA%"::equals));
    }

    @Test
    void valoresDoUsuarioNuncaEntramNoSql() {
        NotificacaoDAO.Filtro f = new NotificacaoDAO.Filtro();
        f.subgrupo = "x' OR '1'='1";
        f.prontuario = "'; DROP TABLE NOTIFICACAO --";
        List<Object> params = new ArrayList<>();
        String where = NotificacaoDAO.montarWhere(f, params);
        assertFalse(where.contains("DROP"));
        assertFalse(where.contains("OR '1'"));
    }

    @Test
    void ordenacaoPorClassificacaoUsaOGrauDeGravidade() {
        String sql = NotificacaoDAO.Ordenacao.CLASSIFICACAO.getSql();
        assertTrue(sql.contains("WHEN 'Óbito' THEN 7"));
        assertTrue(sql.contains("WHEN 'Circunstância de risco' THEN 1"));
    }
}
