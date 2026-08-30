package br.com.hospital.notificacao.config;

import br.com.hospital.notificacao.model.Perfil;
import br.com.hospital.notificacao.service.HashSenha;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Cria os usuarios iniciais do NSP no primeiro start, se a tabela USUARIO estiver vazia.
 * A senha inicial vem da variavel de ambiente ADMIN_SENHA_INICIAL (mesma senha para os
 * dois usuarios seed); se ausente, usa um valor padrao documentado no README, que deve
 * ser trocado antes de qualquer uso real.
 */
public final class SeedData {

    private SeedData() { }

    public static void aplicar() {
        try (Connection con = DataSourceFactory.obterConexao()) {
            if (existeAlgumUsuario(con)) {
                return;
            }
            String senhaInicial = System.getenv("ADMIN_SENHA_INICIAL");
            if (senhaInicial == null || senhaInicial.isEmpty()) {
                senhaInicial = "TrocarSenha@2026";
            }
            inserirUsuario(con, "ana.qualidade", "Ana - Qualidade", Perfil.NSP_GESTOR, senhaInicial);
            inserirUsuario(con, "carlos.nsp", "Carlos - NSP", Perfil.NSP_ANALISTA, senhaInicial);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao aplicar seed de usuarios", e);
        }
    }

    private static boolean existeAlgumUsuario(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM USUARIO");
             java.sql.ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static void inserirUsuario(Connection con, String login, String nome, Perfil perfil, String senha)
            throws SQLException {
        String salt = HashSenha.gerarSalt();
        String hash = HashSenha.gerarHash(senha, salt);
        String sql = "INSERT INTO USUARIO (LOGIN, NOME, SENHA_HASH, SALT, PERFIL, ATIVO) VALUES (?, ?, ?, ?, ?, 1)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, nome);
            ps.setString(3, hash);
            ps.setString(4, salt);
            ps.setString(5, perfil.name());
            ps.executeUpdate();
        }
    }
}
