package br.com.hospital.notificacao.dao;

import br.com.hospital.notificacao.config.DataSourceFactory;
import br.com.hospital.notificacao.model.Perfil;
import br.com.hospital.notificacao.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UsuarioDAO {

    public Optional<Usuario> buscarPorLogin(String login) {
        String sql = "SELECT ID, LOGIN, NOME, SENHA_HASH, SALT, PERFIL, ATIVO FROM USUARIO WHERE LOGIN = ?";
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuario por login", e);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getLong("ID"));
        u.setLogin(rs.getString("LOGIN"));
        u.setNome(rs.getString("NOME"));
        u.setSenhaHash(rs.getString("SENHA_HASH"));
        u.setSalt(rs.getString("SALT"));
        u.setPerfil(Perfil.valueOf(rs.getString("PERFIL")));
        u.setAtivo(rs.getInt("ATIVO") == 1);
        return u;
    }
}
