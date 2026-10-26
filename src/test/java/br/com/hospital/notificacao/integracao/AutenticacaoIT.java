package br.com.hospital.notificacao.integracao;

import br.com.hospital.notificacao.config.SeedData;
import br.com.hospital.notificacao.model.Perfil;
import br.com.hospital.notificacao.model.Usuario;
import br.com.hospital.notificacao.service.AutenticacaoService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Login do painel administrativo e seed dos usuarios iniciais. */
class AutenticacaoIT extends IntegracaoBase {

    private static final String SENHA = "Senha@Teste1";
    private final AutenticacaoService service = new AutenticacaoService();

    private static String senhaInicialDoAmbiente() {
        String env = System.getenv("ADMIN_SENHA_INICIAL");
        return env == null || env.isEmpty() ? null : env;
    }

    @Test
    void autenticaComOPerfilCorreto() {
        criarUsuario("ana.qualidade", Perfil.NSP_GESTOR, SENHA);
        criarUsuario("carlos.nsp", Perfil.NSP_ANALISTA, SENHA);

        Optional<Usuario> ana = service.autenticar("ana.qualidade", SENHA);
        Optional<Usuario> carlos = service.autenticar("carlos.nsp", SENHA);
        assertTrue(ana.isPresent());
        assertEquals(Perfil.NSP_GESTOR, ana.get().getPerfil());
        assertTrue(carlos.isPresent());
        assertEquals(Perfil.NSP_ANALISTA, carlos.get().getPerfil());
    }

    @Test
    void senhaFicaSomenteEmHash() {
        criarUsuario("ana.qualidade", Perfil.NSP_GESTOR, SENHA);
        assertEquals(0, contar("SELECT COUNT(*) FROM USUARIO WHERE SENHA_HASH = ? OR SALT = ?", SENHA, SENHA));
    }

    @Test
    void loginToleraEspacosNoUsuarioMasNaoNaSenha() {
        criarUsuario("ana.qualidade", Perfil.NSP_GESTOR, SENHA);
        assertTrue(service.autenticar("  ana.qualidade ", SENHA).isPresent());
        assertFalse(service.autenticar("ana.qualidade", " " + SENHA).isPresent());
    }

    @Test
    void recusaSenhaErradaUsuarioInexistenteECamposNulos() {
        criarUsuario("ana.qualidade", Perfil.NSP_GESTOR, SENHA);
        assertFalse(service.autenticar("ana.qualidade", "errada").isPresent());
        assertFalse(service.autenticar("ANA.QUALIDADE", SENHA).isPresent(), "login diferencia maiúsculas");
        assertFalse(service.autenticar("ninguem", SENHA).isPresent());
        assertFalse(service.autenticar(null, SENHA).isPresent());
        assertFalse(service.autenticar("ana.qualidade", null).isPresent());
    }

    @Test
    void usuarioDesativadoNaoEntra() {
        criarUsuario("carlos.nsp", Perfil.NSP_ANALISTA, SENHA);
        executar("UPDATE USUARIO SET ATIVO = 0 WHERE LOGIN = 'carlos.nsp'");
        assertFalse(service.autenticar("carlos.nsp", SENHA).isPresent());
    }

    // ---- SeedData ----

    @Test
    void seedSemSenhaInicialDefinidaFalhaEmVezDeUsarSenhaPadrao() {
        assumeTrue(senhaInicialDoAmbiente() == null, "ADMIN_SENHA_INICIAL está definida neste ambiente");
        assertThrows(IllegalStateException.class, SeedData::aplicar);
        assertEquals(0, contar("SELECT COUNT(*) FROM USUARIO"));
    }

    @Test
    void seedNaoFazNadaQuandoJaExistemUsuarios() {
        criarUsuario("outro.gestor", Perfil.NSP_GESTOR, SENHA);
        assertDoesNotThrow(SeedData::aplicar);
        assertEquals(1, contar("SELECT COUNT(*) FROM USUARIO"));
    }

    @Test
    void seedCriaGestorEAnalistaUmaUnicaVez() {
        String senha = senhaInicialDoAmbiente();
        assumeTrue(senha != null, "requer ADMIN_SENHA_INICIAL definida no ambiente");
        SeedData.aplicar();
        SeedData.aplicar();

        assertEquals(2, contar("SELECT COUNT(*) FROM USUARIO"));
        assertEquals(Perfil.NSP_GESTOR, service.autenticar("ana.qualidade", senha).get().getPerfil());
        assertEquals(Perfil.NSP_ANALISTA, service.autenticar("carlos.nsp", senha).get().getPerfil());
    }
}
