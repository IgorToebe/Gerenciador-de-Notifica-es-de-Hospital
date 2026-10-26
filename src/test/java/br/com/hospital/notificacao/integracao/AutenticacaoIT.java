package br.com.hospital.notificacao.integracao;

import br.com.hospital.notificacao.config.SeedData;
import br.com.hospital.notificacao.model.Perfil;
import br.com.hospital.notificacao.model.Usuario;
import br.com.hospital.notificacao.service.AutenticacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Login do painel administrativo com os usuarios semeados no primeiro start. */
class AutenticacaoIT extends IntegracaoBase {

    private final AutenticacaoService service = new AutenticacaoService();
    private String senhaInicial;

    @BeforeEach
    void semear() {
        SeedData.aplicar();
        String env = System.getenv("ADMIN_SENHA_INICIAL");
        senhaInicial = env != null && !env.isEmpty() ? env : "TrocarSenha@2026";
    }

    @Test
    void seedCriaGestorEAnalista() {
        Optional<Usuario> ana = service.autenticar("ana.qualidade", senhaInicial);
        Optional<Usuario> carlos = service.autenticar("carlos.nsp", senhaInicial);
        assertTrue(ana.isPresent());
        assertEquals(Perfil.NSP_GESTOR, ana.get().getPerfil());
        assertTrue(carlos.isPresent());
        assertEquals(Perfil.NSP_ANALISTA, carlos.get().getPerfil());
    }

    @Test
    void seedNaoDuplicaUsuariosNosProximosStarts() {
        SeedData.aplicar();
        SeedData.aplicar();
        assertEquals(2, contar("SELECT COUNT(*) FROM USUARIO"));
    }

    @Test
    void senhaFicaSomenteEmHash() {
        assertEquals(0, contar("SELECT COUNT(*) FROM USUARIO WHERE SENHA_HASH = ?", senhaInicial));
    }

    @Test
    void loginToleraEspacosNoUsuarioMasNaoNaSenha() {
        assertTrue(service.autenticar("  ana.qualidade ", senhaInicial).isPresent());
        assertFalse(service.autenticar("ana.qualidade", " " + senhaInicial).isPresent());
    }

    @Test
    void recusaSenhaErradaUsuarioInexistenteECamposNulos() {
        assertFalse(service.autenticar("ana.qualidade", "errada").isPresent());
        assertFalse(service.autenticar("ANA.QUALIDADE", senhaInicial).isPresent(), "login diferencia maiúsculas");
        assertFalse(service.autenticar("ninguem", senhaInicial).isPresent());
        assertFalse(service.autenticar(null, senhaInicial).isPresent());
        assertFalse(service.autenticar("ana.qualidade", null).isPresent());
    }

    @Test
    void usuarioDesativadoNaoEntra() {
        executar("UPDATE USUARIO SET ATIVO = 0 WHERE LOGIN = 'carlos.nsp'");
        assertFalse(service.autenticar("carlos.nsp", senhaInicial).isPresent());
    }
}
