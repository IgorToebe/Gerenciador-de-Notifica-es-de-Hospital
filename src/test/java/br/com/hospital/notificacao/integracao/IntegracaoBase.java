package br.com.hospital.notificacao.integracao;

import br.com.hospital.notificacao.config.DataSourceFactory;
import br.com.hospital.notificacao.config.SchemaInitializer;
import br.com.hospital.notificacao.model.ClassificacaoIncidente;
import br.com.hospital.notificacao.model.Perfil;
import br.com.hospital.notificacao.model.TipoNotificacao;
import br.com.hospital.notificacao.service.HashSenha;
import br.com.hospital.notificacao.service.NotificacaoService;
import br.com.hospital.notificacao.service.NovaNotificacaoRequest;
import br.com.hospital.notificacao.service.ResultadoNotificacao;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Base dos testes de integracao: sobem o schema real (SchemaInitializer) e exercitam
 * DAOs e services com SQL de verdade.
 *
 * Por padrao usam H2 em memoria no modo de compatibilidade DB2 — rapido e sem Docker.
 * Para rodar a MESMA suite contra um DB2 real (ex.: o do docker-compose), passe:
 * <pre>
 *   -Dnsp.teste.jdbcUrl=jdbc:db2://localhost:50000/HOSPITAL -Dnsp.teste.usuario=db2inst1
 *   -Dnsp.teste.senha=... -Dnsp.teste.apagarDados=true
 * </pre>
 * ATENCAO: cada teste apaga todas as notificacoes/usuarios; por isso o DB2 exige a
 * confirmacao explicita {@code nsp.teste.apagarDados=true} e deve ser um banco de teste.
 */
public abstract class IntegracaoBase {

    private static final String[] TABELAS_EM_ORDEM_DE_EXCLUSAO = {
            "PLANO_ACAO", "INVESTIGACAO_MARCO", "INVESTIGACAO", "NOTIFICACAO_HISTORICO",
            "NOTIFICACAO_ANEXO", "NOTIFICACAO_CAMPO", "NOTIFICACAO", "USUARIO"
    };

    private static boolean pronto;

    protected final NotificacaoService notificacaoService = new NotificacaoService();

    @BeforeAll
    protected static synchronized void prepararBanco() {
        if (pronto) return;
        String url = System.getProperty("nsp.teste.jdbcUrl");
        if (url == null) {
            DataSourceFactory.inicializar("jdbc:h2:mem:nsp;MODE=DB2;DB_CLOSE_DELAY=-1", "sa", "");
        } else {
            if (!"true".equals(System.getProperty("nsp.teste.apagarDados"))) {
                throw new IllegalStateException("Os testes apagam os dados do banco: confirme com -Dnsp.teste.apagarDados=true");
            }
            DataSourceFactory.inicializar(url, System.getProperty("nsp.teste.usuario", "db2inst1"),
                    System.getProperty("nsp.teste.senha"));
        }
        SchemaInitializer.executar();
        pronto = true;
    }

    protected static boolean usandoDb2Real() {
        return System.getProperty("nsp.teste.jdbcUrl") != null;
    }

    @BeforeEach
    protected void limparBanco() {
        for (String tabela : TABELAS_EM_ORDEM_DE_EXCLUSAO) {
            executar("DELETE FROM " + tabela);
        }
    }

    // ---- acesso direto ao banco (para preparar cenarios e conferir o que foi gravado) ----

    protected static int executar(String sql, Object... params) {
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
    }

    protected static int contar(String sql, Object... params) {
        Object valor = valor(sql, params);
        return valor == null ? 0 : ((Number) valor).intValue();
    }

    protected static Object valor(String sql, Object... params) {
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
    }

    // ---- fabricas de cenario ----

    /** Requisicao valida do wizard (todos os campos obrigatorios do tipo preenchidos). */
    protected static NovaNotificacaoRequest requisicao(TipoNotificacao tipo) {
        NovaNotificacaoRequest req = new NovaNotificacaoRequest();
        req.setTipo(tipo);
        req.setDataIncidente("12/10/2026");
        req.setHoraIncidente("14:30");
        req.setSetor("UTI Adulto");
        req.setProntuario("");
        req.setSubgrupo(tipo.getSubgrupos().get(0));
        req.setGravidade(ClassificacaoIncidente.TODAS.get(2));
        req.setDescricao("Descrição do incidente de teste");
        tipo.getCamposDinamicos().stream()
                .filter(c -> c.isObrigatorio())
                .forEach(c -> req.getCamposDinamicos().put(c.getChave(), "valor " + c.getChave()));
        return req;
    }

    protected ResultadoNotificacao criar(NovaNotificacaoRequest req) {
        return notificacaoService.criar(req);
    }

    protected long criarId(NovaNotificacaoRequest req) {
        return idPorProtocolo(criar(req).getProtocolo());
    }

    /** Cria um usuario do NSP direto no banco (o SeedData exige ADMIN_SENHA_INICIAL). */
    protected static long criarUsuario(String login, Perfil perfil, String senha) {
        String salt = HashSenha.gerarSalt();
        executar("INSERT INTO USUARIO (LOGIN, NOME, SENHA_HASH, SALT, PERFIL, ATIVO) VALUES (?, ?, ?, ?, ?, 1)",
                login, login, HashSenha.gerarHash(senha, salt), salt, perfil.name());
        return ((Number) valor("SELECT ID FROM USUARIO WHERE LOGIN = ?", login)).longValue();
    }

    protected static long idPorProtocolo(String protocolo) {
        return ((Number) valor("SELECT ID FROM NOTIFICACAO WHERE PROTOCOLO = ?", protocolo)).longValue();
    }
}
