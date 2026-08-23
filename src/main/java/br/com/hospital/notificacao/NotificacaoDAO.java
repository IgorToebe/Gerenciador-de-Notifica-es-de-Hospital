package br.com.hospital.notificacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class NotificacaoDAO {

    private static final String SQLSTATE_OBJETO_JA_EXISTE = "42710";

    public void criarTabelaSeNaoExiste() {
        String sql = "CREATE TABLE notificacao (" +
                "id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                "mensagem VARCHAR(255) NOT NULL, " +
                "data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT TIMESTAMP)";
        try (Connection con = ConexaoDB2.obterConexao(); Statement st = con.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            if (!SQLSTATE_OBJETO_JA_EXISTE.equals(e.getSQLState())) {
                throw new RuntimeException("Erro ao criar tabela notificacao", e);
            }
        }
    }

    public void inserir(String mensagem) {
        String sql = "INSERT INTO notificacao (mensagem) VALUES (?)";
        try (Connection con = ConexaoDB2.obterConexao(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, mensagem);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir notificação", e);
        }
    }

    public List<Notificacao> listarTodas() {
        String sql = "SELECT id, mensagem, data_criacao FROM notificacao ORDER BY id DESC";
        List<Notificacao> notificacoes = new ArrayList<>();
        try (Connection con = ConexaoDB2.obterConexao();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Notificacao n = new Notificacao();
                n.setId(rs.getInt("id"));
                n.setMensagem(rs.getString("mensagem"));
                n.setDataCriacao(rs.getTimestamp("data_criacao"));
                notificacoes.add(n);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar notificações", e);
        }
        return notificacoes;
    }
}
