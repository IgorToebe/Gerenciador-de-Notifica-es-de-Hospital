package br.com.hospital.notificacao.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TipoNotificacaoTest {

    @Test
    void todoTipoTemSubgrupos() {
        for (TipoNotificacao t : TipoNotificacao.values()) {
            assertFalse(t.getSubgrupos().isEmpty(), t.name());
            assertFalse(t.getRotuloSubgrupo().isEmpty(), t.name());
        }
    }

    @Test
    void aceitaSomenteSubgruposDoProprioTipo() {
        assertTrue(TipoNotificacao.FARMACOVIGILANCIA.aceitaSubgrupo("Erro de medicação"));
        assertFalse(TipoNotificacao.TECNOVIGILANCIA.aceitaSubgrupo("Erro de medicação"));
        assertFalse(TipoNotificacao.TECNOVIGILANCIA.aceitaSubgrupo(null));
    }

    @Test
    void registroAnvisaEhObrigatorioNaTecnovigilancia() {
        assertTrue(TipoNotificacao.TECNOVIGILANCIA.getCamposDinamicos().stream()
                .anyMatch(c -> c.getChave().equals("registroAnvisa") && c.isObrigatorio()));
    }

    @Test
    void selectsQueViraramSubgrupoNaoSaoMaisCamposDinamicos() {
        for (TipoNotificacao t : TipoNotificacao.values()) {
            assertTrue(t.getCamposDinamicos().stream()
                    .noneMatch(c -> TipoNotificacao.CHAVES_SUBGRUPO_LEGADAS.contains(c.getChave())), t.name());
        }
    }
}
