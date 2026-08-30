package br.com.hospital.notificacao.dao;

import br.com.hospital.notificacao.config.DataSourceFactory;
import br.com.hospital.notificacao.model.HistoricoStatus;
import br.com.hospital.notificacao.model.StatusNotificacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class HistoricoDAO {

    public void inserir(Connection con, long idNotificacao, StatusNotificacao anterior, StatusNotificacao novo,
                         Long idUsuario, String observacao) throws SQLException {
        String sql = "INSERT INTO NOTIFICACAO_HISTORICO (ID_NOTIFICACAO, STATUS_ANTERIOR, STATUS_NOVO, " +
                "ID_USUARIO, OBSERVACAO) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idNotificacao);
            if (anterior != null) ps.setString(2, anterior.name()); else ps.setNull(2, Types.VARCHAR);
            ps.setString(3, novo.name());
            if (idUsuario != null) ps.setLong(4, idUsuario); else ps.setNull(4, Types.BIGINT);
            ps.setString(5, observacao);
            ps.executeUpdate();
        }
    }

    public List<HistoricoStatus> listarPorNotificacao(long idNotificacao) {
        String sql = "SELECT ID, ID_NOTIFICACAO, STATUS_ANTERIOR, STATUS_NOVO, ID_USUARIO, OBSERVACAO, CRIADO_EM " +
                "FROM NOTIFICACAO_HISTORICO WHERE ID_NOTIFICACAO = ? ORDER BY ID";
        List<HistoricoStatus> lista = new ArrayList<>();
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idNotificacao);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HistoricoStatus h = new HistoricoStatus();
                    h.setId(rs.getLong("ID"));
                    h.setIdNotificacao(rs.getLong("ID_NOTIFICACAO"));
                    String anterior = rs.getString("STATUS_ANTERIOR");
                    if (anterior != null) h.setStatusAnterior(StatusNotificacao.valueOf(anterior));
                    h.setStatusNovo(StatusNotificacao.valueOf(rs.getString("STATUS_NOVO")));
                    long idUsuario = rs.getLong("ID_USUARIO");
                    if (!rs.wasNull()) h.setIdUsuario(idUsuario);
                    h.setObservacao(rs.getString("OBSERVACAO"));
                    h.setCriadoEm(rs.getTimestamp("CRIADO_EM"));
                    lista.add(h);
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar historico da notificacao", e);
        }
    }
}
