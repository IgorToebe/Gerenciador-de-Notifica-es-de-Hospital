package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.model.TipoNotificacao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidacaoNotificacaoTest {

    @Test
    void aceitaNotificacaoCompleta() {
        assertDoesNotThrow(() -> ValidacaoNotificacao.validar(base()));
    }

    @Test
    void rejeitaDataInvalida() {
        NovaNotificacaoRequest req = base();
        req.setDataIncidente("31/02/2026");
        assertThrows(IllegalArgumentException.class, () -> ValidacaoNotificacao.validar(req));
    }

    @Test
    void rejeitaHoraInvalida() {
        NovaNotificacaoRequest req = base();
        req.setHoraIncidente("27:90");
        assertThrows(IllegalArgumentException.class, () -> ValidacaoNotificacao.validar(req));
    }

    @Test
    void rejeitaContatoInvalido() {
        NovaNotificacaoRequest req = base();
        req.setContatoEmail("nao-e-mail");
        assertThrows(IllegalArgumentException.class, () -> ValidacaoNotificacao.validar(req));
    }

    @Test
    void rejeitaTelefoneSemDdd() {
        NovaNotificacaoRequest req = base();
        req.setContatoTelefone("9999-9999");
        assertThrows(IllegalArgumentException.class, () -> ValidacaoNotificacao.validar(req));
    }

    @Test
    void rejeitaCampoDinamicoObrigatorioAusente() {
        NovaNotificacaoRequest req = base();
        req.getCamposDinamicos().clear();
        assertThrows(IllegalArgumentException.class, () -> ValidacaoNotificacao.validar(req));
    }

    @Test
    void exigeRegistroAnvisaNaTecnovigilancia() {
        NovaNotificacaoRequest req = base();
        req.getCamposDinamicos().remove("registroAnvisa");
        assertThrows(IllegalArgumentException.class, () -> ValidacaoNotificacao.validar(req));
    }

    @Test
    void exigeSubgrupoDoProprioTipo() {
        NovaNotificacaoRequest semSubgrupo = base();
        semSubgrupo.setSubgrupo(null);
        assertThrows(IllegalArgumentException.class, () -> ValidacaoNotificacao.validar(semSubgrupo));

        NovaNotificacaoRequest deOutroTipo = base();
        deOutroTipo.setSubgrupo("Erro de medicação"); // e' da Farmacovigilancia
        assertThrows(IllegalArgumentException.class, () -> ValidacaoNotificacao.validar(deOutroTipo));
    }

    @Test
    void aceitaContatoOpcionalVazio() {
        NovaNotificacaoRequest req = base();
        req.setContatoEmail(" ");
        req.setContatoTelefone(" ");
        assertDoesNotThrow(() -> ValidacaoNotificacao.validar(req));
    }

    private NovaNotificacaoRequest base() {
        NovaNotificacaoRequest req = new NovaNotificacaoRequest();
        req.setTipo(TipoNotificacao.TECNOVIGILANCIA);
        req.setDataIncidente("10/09/2026");
        req.setHoraIncidente("14:30");
        req.setSetor("UTI Adulto");
        req.setGravidade("Incidente sem dano");
        req.setDescricao("Descrição válida do incidente");
        req.setSubgrupo("Equipamento médico");
        req.getCamposDinamicos().put("produto", "Monitor multiparâmetros");
        req.getCamposDinamicos().put("registroAnvisa", "80146170001");
        return req;
    }
}
