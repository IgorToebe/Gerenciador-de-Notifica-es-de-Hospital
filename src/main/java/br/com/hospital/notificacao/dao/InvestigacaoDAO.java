package br.com.hospital.notificacao.dao;

import br.com.hospital.notificacao.config.DataSourceFactory;
import br.com.hospital.notificacao.model.Investigacao;
import br.com.hospital.notificacao.model.MarcoInvestigacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InvestigacaoDAO {

    private static final String COLUNAS =
            "ID, ID_NOTIFICACAO, PARECERISTA, DATA_INVESTIGACAO, FONTES, FATOR_PROFISSIONAL, FATOR_COMUNICACAO, " +
            "FATOR_PACIENTE, FATOR_AMBIENTE, FATOR_ORGANIZACIONAL, FATOR_EXTERNO, PORQUE1, PORQUE2, PORQUE3, " +
            "PORQUE4, CAUSA_RAIZ, DETECCAO, ATENUANTES, PARECER, CONCLUIDA";

    public Optional<Investigacao> buscarPorNotificacao(long idNotificacao) {
        String sql = "SELECT " + COLUNAS + " FROM INVESTIGACAO WHERE ID_NOTIFICACAO = ?";
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idNotificacao);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Investigacao inv = mapear(rs);
                inv.setTimeline(listarTimeline(con, inv.getId()));
                return Optional.of(inv);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar investigacao por notificacao", e);
        }
    }

    /** Cria a investigacao se ainda nao existir para a notificacao (uma por notificacao). */
    public long criarSeNaoExiste(long idNotificacao) {
        Optional<Investigacao> existente = buscarPorNotificacao(idNotificacao);
        if (existente.isPresent()) {
            return existente.get().getId();
        }
        String sql = "INSERT INTO INVESTIGACAO (ID_NOTIFICACAO, CONCLUIDA) VALUES (?, 0)";
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, idNotificacao);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao criar investigacao", e);
        }
    }

    public void salvar(Investigacao inv) {
        String sql = "UPDATE INVESTIGACAO SET PARECERISTA=?, DATA_INVESTIGACAO=?, FONTES=?, FATOR_PROFISSIONAL=?, " +
                "FATOR_COMUNICACAO=?, FATOR_PACIENTE=?, FATOR_AMBIENTE=?, FATOR_ORGANIZACIONAL=?, FATOR_EXTERNO=?, " +
                "PORQUE1=?, PORQUE2=?, PORQUE3=?, PORQUE4=?, CAUSA_RAIZ=?, DETECCAO=?, ATENUANTES=?, PARECER=?, " +
                "CONCLUIDA=? WHERE ID=?";
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, inv.getParecerista());
            ps.setString(2, inv.getDataInvestigacao());
            ps.setString(3, inv.getFontes());
            ps.setString(4, inv.getFatorProfissional());
            ps.setString(5, inv.getFatorComunicacao());
            ps.setString(6, inv.getFatorPaciente());
            ps.setString(7, inv.getFatorAmbiente());
            ps.setString(8, inv.getFatorOrganizacional());
            ps.setString(9, inv.getFatorExterno());
            ps.setString(10, inv.getPorque1());
            ps.setString(11, inv.getPorque2());
            ps.setString(12, inv.getPorque3());
            ps.setString(13, inv.getPorque4());
            ps.setString(14, inv.getCausaRaiz());
            ps.setString(15, inv.getDeteccao());
            ps.setString(16, inv.getAtenuantes());
            ps.setString(17, inv.getParecer());
            ps.setInt(18, inv.isConcluida() ? 1 : 0);
            ps.setLong(19, inv.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar investigacao", e);
        }
    }

    public void adicionarMarco(long idInvestigacao, String quando, String descricao) {
        String sql = "INSERT INTO INVESTIGACAO_MARCO (ID_INVESTIGACAO, QUANDO, DESCRICAO, ORDEM) " +
                "VALUES (?, ?, ?, (SELECT COALESCE(MAX(ORDEM), 0) + 1 FROM INVESTIGACAO_MARCO WHERE ID_INVESTIGACAO = ?))";
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idInvestigacao);
            ps.setString(2, quando);
            ps.setString(3, descricao);
            ps.setLong(4, idInvestigacao);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao adicionar marco na timeline da investigacao", e);
        }
    }

    private List<MarcoInvestigacao> listarTimeline(Connection con, long idInvestigacao) throws SQLException {
        String sql = "SELECT ID, ID_INVESTIGACAO, QUANDO, DESCRICAO, ORDEM FROM INVESTIGACAO_MARCO " +
                "WHERE ID_INVESTIGACAO = ? ORDER BY ORDEM";
        List<MarcoInvestigacao> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idInvestigacao);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MarcoInvestigacao m = new MarcoInvestigacao();
                    m.setId(rs.getLong("ID"));
                    m.setIdInvestigacao(rs.getLong("ID_INVESTIGACAO"));
                    m.setQuando(rs.getString("QUANDO"));
                    m.setDescricao(rs.getString("DESCRICAO"));
                    m.setOrdem(rs.getInt("ORDEM"));
                    lista.add(m);
                }
            }
        }
        return lista;
    }

    private Investigacao mapear(ResultSet rs) throws SQLException {
        Investigacao inv = new Investigacao();
        inv.setId(rs.getLong("ID"));
        inv.setIdNotificacao(rs.getLong("ID_NOTIFICACAO"));
        inv.setParecerista(rs.getString("PARECERISTA"));
        inv.setDataInvestigacao(rs.getString("DATA_INVESTIGACAO"));
        inv.setFontes(rs.getString("FONTES"));
        inv.setFatorProfissional(rs.getString("FATOR_PROFISSIONAL"));
        inv.setFatorComunicacao(rs.getString("FATOR_COMUNICACAO"));
        inv.setFatorPaciente(rs.getString("FATOR_PACIENTE"));
        inv.setFatorAmbiente(rs.getString("FATOR_AMBIENTE"));
        inv.setFatorOrganizacional(rs.getString("FATOR_ORGANIZACIONAL"));
        inv.setFatorExterno(rs.getString("FATOR_EXTERNO"));
        inv.setPorque1(rs.getString("PORQUE1"));
        inv.setPorque2(rs.getString("PORQUE2"));
        inv.setPorque3(rs.getString("PORQUE3"));
        inv.setPorque4(rs.getString("PORQUE4"));
        inv.setCausaRaiz(rs.getString("CAUSA_RAIZ"));
        inv.setDeteccao(rs.getString("DETECCAO"));
        inv.setAtenuantes(rs.getString("ATENUANTES"));
        inv.setParecer(rs.getString("PARECER"));
        inv.setConcluida(rs.getInt("CONCLUIDA") == 1);
        return inv;
    }
}
