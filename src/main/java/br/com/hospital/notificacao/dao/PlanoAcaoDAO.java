package br.com.hospital.notificacao.dao;

import br.com.hospital.notificacao.config.DataSourceFactory;
import br.com.hospital.notificacao.model.AcaoPlano;
import br.com.hospital.notificacao.model.StatusAcao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PlanoAcaoDAO {

    public boolean existePlano(long idNotificacao) {
        String sql = "SELECT COUNT(*) FROM PLANO_ACAO WHERE ID_NOTIFICACAO = ?";
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idNotificacao);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar existencia de plano de acao", e);
        }
    }

    public long inserir(AcaoPlano a) {
        String sql = "INSERT INTO PLANO_ACAO (ID_NOTIFICACAO, CODIGO, O_QUE, QUEM, ONDE, PORQUE, COMO, QUANTO, " +
                "DATA_INICIO, DATA_FIM, STATUS, EVIDENCIA, ORDEM) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, a.getIdNotificacao());
            ps.setString(2, a.getCodigo());
            ps.setString(3, a.getoQue());
            ps.setString(4, a.getQuem());
            ps.setString(5, a.getOnde());
            ps.setString(6, a.getPorque());
            ps.setString(7, a.getComo());
            ps.setString(8, a.getQuanto());
            ps.setString(9, a.getDataInicio());
            ps.setString(10, a.getDataFim());
            ps.setString(11, a.getStatus().name());
            ps.setString(12, a.getEvidencia());
            ps.setInt(13, a.getOrdem());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir acao do plano 5W2H", e);
        }
    }

    public List<AcaoPlano> listarPorNotificacao(long idNotificacao) {
        String sql = "SELECT ID, ID_NOTIFICACAO, CODIGO, O_QUE, QUEM, ONDE, PORQUE, COMO, QUANTO, DATA_INICIO, " +
                "DATA_FIM, STATUS, EVIDENCIA, ORDEM FROM PLANO_ACAO WHERE ID_NOTIFICACAO = ? ORDER BY ORDEM";
        List<AcaoPlano> lista = new ArrayList<>();
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idNotificacao);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar plano de acao da notificacao", e);
        }
    }

    public void atualizarStatus(long id, StatusAcao status) {
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement("UPDATE PLANO_ACAO SET STATUS = ? WHERE ID = ?")) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar status da acao", e);
        }
    }

    public void atualizarEvidencia(long id, String evidencia) {
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement("UPDATE PLANO_ACAO SET EVIDENCIA = ? WHERE ID = ?")) {
            ps.setString(1, evidencia);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar evidencia da acao", e);
        }
    }

    public boolean todasFinalizadas(long idNotificacao) {
        String sql = "SELECT COUNT(*) FROM PLANO_ACAO WHERE ID_NOTIFICACAO = ? AND STATUS NOT IN ('CONCLUIDO','CANCELADO')";
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idNotificacao);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar conclusao do plano de acao", e);
        }
    }

    private AcaoPlano mapear(ResultSet rs) throws SQLException {
        AcaoPlano a = new AcaoPlano();
        a.setId(rs.getLong("ID"));
        a.setIdNotificacao(rs.getLong("ID_NOTIFICACAO"));
        a.setCodigo(rs.getString("CODIGO"));
        a.setoQue(rs.getString("O_QUE"));
        a.setQuem(rs.getString("QUEM"));
        a.setOnde(rs.getString("ONDE"));
        a.setPorque(rs.getString("PORQUE"));
        a.setComo(rs.getString("COMO"));
        a.setQuanto(rs.getString("QUANTO"));
        a.setDataInicio(rs.getString("DATA_INICIO"));
        a.setDataFim(rs.getString("DATA_FIM"));
        a.setStatus(StatusAcao.valueOf(rs.getString("STATUS")));
        a.setEvidencia(rs.getString("EVIDENCIA"));
        a.setOrdem(rs.getInt("ORDEM"));
        return a;
    }
}
