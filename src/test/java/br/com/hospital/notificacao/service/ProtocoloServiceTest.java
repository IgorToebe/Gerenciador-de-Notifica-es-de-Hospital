package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.model.TipoNotificacao;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocoloServiceTest {

    @Test
    void protocoloUsaOPrefixoDoTipoEOAnoAtual() {
        String protocolo = ProtocoloService.gerarProtocolo(TipoNotificacao.TECNOVIGILANCIA, 4821);
        assertEquals("#TC-" + Year.now().getValue() + "-004821", protocolo);
    }

    @Test
    void cadaTipoTemUmPrefixoDistinto() {
        assertEquals("TC", TipoNotificacao.TECNOVIGILANCIA.getPrefixoProtocolo());
        assertEquals("FV", TipoNotificacao.FARMACOVIGILANCIA.getPrefixoProtocolo());
        assertEquals("NM", TipoNotificacao.NEAR_MISS_MATERNO.getPrefixoProtocolo());
        assertEquals("PA", TipoNotificacao.PROBLEMAS_ASSISTENCIAIS.getPrefixoProtocolo());
        assertEquals("NP", TipoNotificacao.NOTIFICACAO_PACIENTE.getPrefixoProtocolo());
    }

    @Test
    void senhaGeradaTemNoveCaracteresDoAlfabetoEsperado() {
        String senha = ProtocoloService.gerarSenha();
        assertEquals(9, senha.length());
        assertTrue(Pattern.matches("[A-Za-z0-9@#]{9}", senha));
    }
}
