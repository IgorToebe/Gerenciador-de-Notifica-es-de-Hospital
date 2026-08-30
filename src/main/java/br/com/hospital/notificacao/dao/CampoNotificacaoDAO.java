package br.com.hospital.notificacao.dao;

import br.com.hospital.notificacao.config.DataSourceFactory;
import br.com.hospital.notificacao.model.CampoNotificacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CampoNotificacaoDAO {

    public void inserirTodos(Connection con, long idNotificacao, List<CampoNotificacao> campos) throws SQLException {
        String sql = "INSERT INTO NOTIFICACAO_CAMPO (ID_NOTIFICACAO, CHAVE, ROTULO, VALOR, ORDEM) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (CampoNotificacao c : campos) {
                if (c.getValor() == null || c.getValor().trim().isEmpty()) continue;
                ps.setLong(1, idNotificacao);
                ps.setString(2, c.getChave());
                ps.setString(3, c.getRotulo());
                ps.setString(4, c.getValor());
                ps.setInt(5, c.getOrdem());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<CampoNotificacao> listarPorNotificacao(long idNotificacao) {
        String sql = "SELECT ID, ID_NOTIFICACAO, CHAVE, ROTULO, VALOR, ORDEM FROM NOTIFICACAO_CAMPO " +
                "WHERE ID_NOTIFICACAO = ? ORDER BY ORDEM";
        List<CampoNotificacao> lista = new ArrayList<>();
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idNotificacao);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CampoNotificacao c = new CampoNotificacao();
                    c.setId(rs.getLong("ID"));
                    c.setIdNotificacao(rs.getLong("ID_NOTIFICACAO"));
                    c.setChave(rs.getString("CHAVE"));
                    c.setRotulo(rs.getString("ROTULO"));
                    c.setValor(rs.getString("VALOR"));
                    c.setOrdem(rs.getInt("ORDEM"));
                    lista.add(c);
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar campos dinamicos da notificacao", e);
        }
    }
}
