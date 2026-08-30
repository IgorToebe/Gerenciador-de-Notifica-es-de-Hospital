package br.com.hospital.notificacao.dao;

import br.com.hospital.notificacao.config.DataSourceFactory;
import br.com.hospital.notificacao.model.Anexo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AnexoDAO {

    public long inserir(Connection con, Anexo a) throws SQLException {
        String sql = "INSERT INTO NOTIFICACAO_ANEXO (ID_NOTIFICACAO, NOME_ORIGINAL, NOME_FISICO, TIPO_ARQUIVO, " +
                "TAMANHO_BYTES) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, a.getIdNotificacao());
            ps.setString(2, a.getNomeOriginal());
            ps.setString(3, a.getNomeFisico());
            ps.setString(4, a.getTipoArquivo());
            ps.setLong(5, a.getTamanhoBytes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public List<Anexo> listarPorNotificacao(long idNotificacao) {
        String sql = "SELECT ID_ANEXO, ID_NOTIFICACAO, NOME_ORIGINAL, NOME_FISICO, TIPO_ARQUIVO, TAMANHO_BYTES, " +
                "DATA_UPLOAD FROM NOTIFICACAO_ANEXO WHERE ID_NOTIFICACAO = ? ORDER BY ID_ANEXO";
        List<Anexo> lista = new ArrayList<>();
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idNotificacao);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar anexos da notificacao", e);
        }
    }

    public Optional<Anexo> buscarPorId(long id) {
        String sql = "SELECT ID_ANEXO, ID_NOTIFICACAO, NOME_ORIGINAL, NOME_FISICO, TIPO_ARQUIVO, TAMANHO_BYTES, " +
                "DATA_UPLOAD FROM NOTIFICACAO_ANEXO WHERE ID_ANEXO = ?";
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar anexo por id", e);
        }
    }

    private Anexo mapear(ResultSet rs) throws SQLException {
        Anexo a = new Anexo();
        a.setId(rs.getLong("ID_ANEXO"));
        a.setIdNotificacao(rs.getLong("ID_NOTIFICACAO"));
        a.setNomeOriginal(rs.getString("NOME_ORIGINAL"));
        a.setNomeFisico(rs.getString("NOME_FISICO"));
        a.setTipoArquivo(rs.getString("TIPO_ARQUIVO"));
        a.setTamanhoBytes(rs.getLong("TAMANHO_BYTES"));
        a.setDataUpload(rs.getTimestamp("DATA_UPLOAD"));
        return a;
    }
}
