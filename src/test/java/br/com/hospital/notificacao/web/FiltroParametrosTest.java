package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.dao.NotificacaoDAO;
import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.model.TipoNotificacao;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FiltroParametrosTest {

    /** Simula request.getParameter a partir de uma query string. */
    private static Map<String, String> parse(String qs) throws UnsupportedEncodingException {
        Map<String, String> m = new HashMap<>();
        if (qs.isEmpty()) return m;
        for (String par : qs.split("&")) {
            String[] kv = par.split("=", 2);
            m.put(kv[0], URLDecoder.decode(kv[1], "UTF-8"));
        }
        return m;
    }

    @Test
    void idaEVoltaPreservaOFiltro() throws Exception {
        NotificacaoDAO.Filtro f = new NotificacaoDAO.Filtro();
        f.status = StatusNotificacao.TRIAGEM;
        f.texto = "bomba & cia";
        f.tipo = TipoNotificacao.FARMACOVIGILANCIA;
        f.subgrupo = "Erro de medicação";
        f.gravidade = "Dano leve";
        f.nivelRisco = NivelRisco.ALTO;
        f.prontuario = "123";
        f.setor = "UTI Adulto";
        f.dataDe = LocalDate.of(2026, 1, 1);
        f.dataAte = LocalDate.of(2026, 3, 31);
        f.ordenacao = NotificacaoDAO.Ordenacao.MAIOR_RISCO;

        Map<String, String> params = parse(FiltroParametros.paraQueryString(f));
        NotificacaoDAO.Filtro volta = FiltroParametros.deParametros(params::get);

        assertEquals(f.status, volta.status);
        assertEquals(f.texto, volta.texto);
        assertEquals(f.tipo, volta.tipo);
        assertEquals(f.subgrupo, volta.subgrupo);
        assertEquals(f.gravidade, volta.gravidade);
        assertEquals(f.nivelRisco, volta.nivelRisco);
        assertEquals(f.prontuario, volta.prontuario);
        assertEquals(f.setor, volta.setor);
        assertEquals(f.dataDe, volta.dataDe);
        assertEquals(f.dataAte, volta.dataAte);
        assertEquals(f.ordenacao, volta.ordenacao);
    }

    @Test
    void filtroPadraoSoLevaAOrdenacao() {
        assertEquals("ordem=RECENTES", FiltroParametros.paraQueryString(new NotificacaoDAO.Filtro()));
    }

    @Test
    void valoresInvalidosSaoIgnorados() {
        Map<String, String> params = new HashMap<>();
        params.put("status", "HACK");
        params.put("tipo", "inexistente");
        params.put("risco", "x");
        params.put("de", "31/02/2026");
        params.put("ordem", "ID; DROP TABLE");
        NotificacaoDAO.Filtro f = FiltroParametros.deParametros(params::get);
        assertNull(f.status);
        assertNull(f.tipo);
        assertNull(f.nivelRisco);
        assertNull(f.dataDe);
        assertEquals(NotificacaoDAO.Ordenacao.RECENTES, f.ordenacao);
    }
}
