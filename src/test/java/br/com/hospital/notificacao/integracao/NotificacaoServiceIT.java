package br.com.hospital.notificacao.integracao;

import br.com.hospital.notificacao.model.Anexo;
import br.com.hospital.notificacao.model.CampoNotificacao;
import br.com.hospital.notificacao.model.HistoricoStatus;
import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.model.TipoNotificacao;
import br.com.hospital.notificacao.service.AnexoService;
import br.com.hospital.notificacao.service.AnexoUpload;
import br.com.hospital.notificacao.service.DetalheNotificacao;
import br.com.hospital.notificacao.service.HashSenha;
import br.com.hospital.notificacao.service.NovaNotificacaoRequest;
import br.com.hospital.notificacao.service.ResultadoNotificacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Abertura de notificacoes pelo wizard (NotificacaoService.criar) e acompanhamento publico. */
class NotificacaoServiceIT extends IntegracaoBase {

    private final AnexoService anexoService = new AnexoService();

    @ParameterizedTest
    @EnumSource(TipoNotificacao.class)
    void cadaUmDosCincoCanaisRegistraUmaNotificacaoCompleta(TipoNotificacao tipo) {
        NovaNotificacaoRequest req = requisicao(tipo);
        ResultadoNotificacao r = criar(req);

        assertTrue(r.getProtocolo().matches("#" + tipo.getPrefixoProtocolo() + "-\\d{4}-\\d{6}"), r.getProtocolo());
        Notificacao n = notificacaoService.carregarCompleta(idPorProtocolo(r.getProtocolo()));
        assertEquals(tipo, n.getTipo());
        assertEquals(StatusNotificacao.ABERTO, n.getStatus());
        assertEquals(req.getSubgrupo(), n.getSubgrupo());
        assertEquals(req.getGravidade(), n.getGravidade());
        assertEquals("UTI Adulto", n.getSetor());
        assertEquals("Descrição do incidente de teste", n.getDescricao());
        assertNull(n.getDescricaoRevisada());
        assertEquals(tipo.getCamposDinamicos().stream().filter(c -> c.isObrigatorio()).count(), n.getCampos().size());
    }

    @Test
    void novaNotificacaoRecebePreClassificacaoDeRiscoMedio() {
        long id = criarId(requisicao(TipoNotificacao.FARMACOVIGILANCIA));
        Notificacao n = notificacaoService.carregarCompleta(id);
        assertEquals(Integer.valueOf(2), n.getProbabilidade());
        assertEquals(Integer.valueOf(4), n.getImpacto());
        assertEquals(Integer.valueOf(8), n.getScoreRisco());
        assertEquals(NivelRisco.MEDIO, n.getNivelRisco());
    }

    @Test
    void historicoRegistraAAbertura() {
        long id = criarId(requisicao(TipoNotificacao.NEAR_MISS_MATERNO));
        List<HistoricoStatus> historico = notificacaoService.carregarDetalhe(id).getHistorico();
        assertEquals(1, historico.size());
        assertNull(historico.get(0).getStatusAnterior());
        assertEquals(StatusNotificacao.ABERTO, historico.get(0).getStatusNovo());
        assertNull(historico.get(0).getIdUsuario(), "abertura via QR Code não tem usuário (anonimato)");
    }

    @Test
    void camposDinamicosSaoGravadosNaOrdemComRotulo() {
        NovaNotificacaoRequest req = requisicao(TipoNotificacao.TECNOVIGILANCIA);
        req.getCamposDinamicos().put("produto", "Bomba de infusão");
        req.getCamposDinamicos().put("registroAnvisa", "80146170001");
        req.getCamposDinamicos().put("lote", "  "); // em branco: nao grava
        List<CampoNotificacao> campos = notificacaoService.carregarCompleta(criarId(req)).getCampos();

        assertEquals(2, campos.size());
        assertEquals("Produto / equipamento afetado", campos.get(0).getRotulo());
        assertEquals("Bomba de infusão", campos.get(0).getValor());
        assertEquals("Registro ANVISA", campos.get(1).getRotulo());
        assertEquals("80146170001", campos.get(1).getValor());
    }

    @Test
    void semContatoANotificacaoEhAnonimaENadaEhGravado() {
        NovaNotificacaoRequest req = requisicao(TipoNotificacao.NOTIFICACAO_PACIENTE);
        req.setContatoEmail("   ");
        ResultadoNotificacao r = criar(req);

        assertFalse(r.isComContato());
        long id = idPorProtocolo(r.getProtocolo());
        assertEquals(1, contar("SELECT ANONIMO FROM NOTIFICACAO WHERE ID = ?", id));
        assertNull(valor("SELECT CONTATO_EMAIL FROM NOTIFICACAO WHERE ID = ?", id));
        assertNull(valor("SELECT CONTATO_TELEFONE FROM NOTIFICACAO WHERE ID = ?", id));
    }

    @Test
    void comContatoDeixaDeSerAnonimaEOContatoVemMascarado() {
        NovaNotificacaoRequest req = requisicao(TipoNotificacao.NOTIFICACAO_PACIENTE);
        req.setContatoEmail("maria.silva@email.com");
        ResultadoNotificacao r = criar(req);

        assertTrue(r.isComContato());
        assertEquals("ma**@email.com", r.getContatoMascarado());
        long id = idPorProtocolo(r.getProtocolo());
        assertEquals(0, contar("SELECT ANONIMO FROM NOTIFICACAO WHERE ID = ?", id));
        assertEquals("maria.silva@email.com", valor("SELECT CONTATO_EMAIL FROM NOTIFICACAO WHERE ID = ?", id));
    }

    @Test
    void senhaDeAcompanhamentoNuncaEhGravadaEmTextoPuro() {
        ResultadoNotificacao r = criar(requisicao(TipoNotificacao.TECNOVIGILANCIA));
        long id = idPorProtocolo(r.getProtocolo());
        String hash = (String) valor("SELECT SENHA_HASH FROM NOTIFICACAO WHERE ID = ?", id);
        String salt = (String) valor("SELECT SENHA_SALT FROM NOTIFICACAO WHERE ID = ?", id);

        assertNotEquals(r.getSenha(), hash);
        assertTrue(HashSenha.conferir(r.getSenha(), salt, hash));
    }

    @Test
    void numeroDoProtocoloSempreCresce() {
        long anterior = 0;
        for (int i = 0; i < 4; i++) {
            String protocolo = criar(requisicao(TipoNotificacao.TECNOVIGILANCIA)).getProtocolo();
            long numero = Long.parseLong(protocolo.substring(protocolo.lastIndexOf('-') + 1));
            assertTrue(numero >= 4821, protocolo);
            assertTrue(numero > anterior, "protocolo voltou: " + protocolo);
            anterior = numero;
        }
    }

    @Test
    void acompanhamentoExigeASenhaCorreta() {
        ResultadoNotificacao r = criar(requisicao(TipoNotificacao.PROBLEMAS_ASSISTENCIAIS));

        assertTrue(notificacaoService.buscarParaAcompanhamento(r.getProtocolo(), r.getSenha()).isPresent());
        assertTrue(notificacaoService.buscarParaAcompanhamento("  " + r.getProtocolo() + " ", r.getSenha()).isPresent());
        assertFalse(notificacaoService.buscarParaAcompanhamento(r.getProtocolo(), "senhaErrada").isPresent());
        assertFalse(notificacaoService.buscarParaAcompanhamento("#PA-2026-999999", r.getSenha()).isPresent());
    }

    @Test
    void subgrupoDeOutroTipoEhRejeitadoENadaEhGravado() {
        NovaNotificacaoRequest req = requisicao(TipoNotificacao.TECNOVIGILANCIA);
        req.setSubgrupo("Erro de medicação"); // e' da Farmacovigilancia
        assertThrows(IllegalArgumentException.class, () -> criar(req));

        NovaNotificacaoRequest semSubgrupo = requisicao(TipoNotificacao.TECNOVIGILANCIA);
        semSubgrupo.setSubgrupo(null);
        assertThrows(IllegalArgumentException.class, () -> criar(semSubgrupo));

        assertEquals(0, contar("SELECT COUNT(*) FROM NOTIFICACAO"));
    }

    @Test
    void anexoValidoGravaArquivoFisicoEMetadados() {
        byte[] conteudo = "%PDF-1.4 laudo".getBytes(StandardCharsets.US_ASCII);
        NovaNotificacaoRequest req = requisicao(TipoNotificacao.TECNOVIGILANCIA);
        req.getAnexos().add(new AnexoUpload("laudo técnico.pdf", "application/pdf", conteudo));

        Notificacao n = notificacaoService.carregarCompleta(criarId(req));

        assertEquals(1, n.getAnexos().size());
        Anexo a = n.getAnexos().get(0);
        assertEquals("laudo técnico.pdf", a.getNomeOriginal());
        assertEquals("application/pdf", a.getTipoArquivo());
        assertEquals(conteudo.length, a.getTamanhoBytes());
        assertTrue(a.getNomeFisico().matches("[0-9a-f-]{36}\\.pdf"), "nome físico é um UUID: " + a.getNomeFisico());
        assertArrayEquals(conteudo, anexoService.lerConteudo(a.getNomeFisico()).get());
    }

    @Test
    void anexoInvalidoDesfazTodaANotificacao() {
        NovaNotificacaoRequest req = requisicao(TipoNotificacao.TECNOVIGILANCIA);
        req.getAnexos().add(new AnexoUpload("virus.exe", "application/x-msdownload", new byte[]{1, 2, 3}));

        assertThrows(IllegalArgumentException.class, () -> criar(req));
        assertEquals(0, contar("SELECT COUNT(*) FROM NOTIFICACAO"), "transação deve ser revertida");
        assertEquals(0, contar("SELECT COUNT(*) FROM NOTIFICACAO_HISTORICO"));
    }

    @Test
    void anexoAcimaDe10MbEhRejeitado() {
        NovaNotificacaoRequest req = requisicao(TipoNotificacao.TECNOVIGILANCIA);
        req.getAnexos().add(new AnexoUpload("grande.png", "image/png", new byte[(int) AnexoService.TAMANHO_MAXIMO_BYTES + 1]));
        assertThrows(IllegalArgumentException.class, () -> criar(req));
        assertEquals(0, contar("SELECT COUNT(*) FROM NOTIFICACAO"));
    }

    @Test
    void detalheDeNotificacaoRecemAbertaNaoTemInvestigacaoNemPlano() {
        DetalheNotificacao d = notificacaoService.carregarDetalhe(criarId(requisicao(TipoNotificacao.TECNOVIGILANCIA)));
        assertNull(d.getInvestigacao());
        assertTrue(d.getAcoes().isEmpty());
        assertEquals(0, contar("SELECT COUNT(*) FROM INVESTIGACAO"), "carregar o detalhe não cria investigação");
    }

    @Test
    void notificacaoInexistenteGeraErroClaro() {
        assertThrows(IllegalArgumentException.class, () -> notificacaoService.carregarCompleta(999_999));
        assertThrows(IllegalArgumentException.class, () -> notificacaoService.carregarDetalhe(999_999));
    }
}
