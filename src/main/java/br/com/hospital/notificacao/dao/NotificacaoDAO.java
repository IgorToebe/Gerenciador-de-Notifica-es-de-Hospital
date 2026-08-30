package br.com.hospital.notificacao.dao;

import br.com.hospital.notificacao.config.DataSourceFactory;
import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.model.TipoNotificacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NotificacaoDAO {

    private static final String COLUNAS =
            "ID, PROTOCOLO, TIPO, SENHA_HASH, SENHA_SALT, DATA_INCIDENTE, HORA_INCIDENTE, SETOR, " +
            "PRONTUARIO, GRAVIDADE, GEROU_DANO, DESCRICAO, ANONIMO, CONTATO_EMAIL, CONTATO_TELEFONE, " +
            "STATUS, PROBABILIDADE, IMPACTO, SCORE_RISCO, NIVEL_RISCO, CRIADO_EM, ATUALIZADO_EM";

    /** Insere a notificacao (cabecalho) dentro de uma transacao controlada pelo chamador. */
    public long inserir(Connection con, Notificacao n) throws SQLException {
        String sql = "INSERT INTO NOTIFICACAO (PROTOCOLO, TIPO, SENHA_HASH, SENHA_SALT, DATA_INCIDENTE, " +
                "HORA_INCIDENTE, SETOR, PRONTUARIO, GRAVIDADE, GEROU_DANO, DESCRICAO, ANONIMO, " +
                "CONTATO_EMAIL, CONTATO_TELEFONE, STATUS) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, n.getProtocolo());
            ps.setString(2, n.getTipo().name());
            ps.setString(3, n.getSenhaHash());
            ps.setString(4, n.getSenhaSalt());
            ps.setString(5, n.getDataIncidente());
            ps.setString(6, n.getHoraIncidente());
            ps.setString(7, n.getSetor());
            ps.setString(8, n.getProntuario());
            ps.setString(9, n.getGravidade());
            ps.setInt(10, n.isGerouDano() ? 1 : 0);
            ps.setString(11, n.getDescricao());
            ps.setInt(12, n.isAnonimo() ? 1 : 0);
            ps.setString(13, n.getContatoEmail());
            ps.setString(14, n.getContatoTelefone());
            ps.setString(15, n.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public Optional<Notificacao> buscarPorId(long id) {
        String sql = "SELECT " + COLUNAS + " FROM NOTIFICACAO WHERE ID = ?";
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar notificacao por id", e);
        }
    }

    public Optional<Notificacao> buscarPorProtocolo(String protocolo) {
        String sql = "SELECT " + COLUNAS + " FROM NOTIFICACAO WHERE PROTOCOLO = ?";
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, protocolo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar notificacao por protocolo", e);
        }
    }

    public long proximoSequencial() {
        // Usado para compor o protocolo (#PREFIXO-ANO-000123): maior ID atual + 1,
        // suficiente para gerar um numero legivel sem uma sequence dedicada.
        String sql = "SELECT COALESCE(MAX(ID), 4820) + 1 FROM NOTIFICACAO";
        try (Connection con = DataSourceFactory.obterConexao();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao calcular proximo sequencial", e);
        }
    }

    /** Filtros de listagem do painel de notificacoes. */
    public static class Filtro {
        public StatusNotificacao status;
        public boolean apenasDuplicatas;
        public String texto;
        public int pagina = 1;
        public int tamanhoPagina = 10;
    }

    public static class ResultadoPagina {
        public final List<Notificacao> itens;
        public final int total;

        public ResultadoPagina(List<Notificacao> itens, int total) {
            this.itens = itens;
            this.total = total;
        }
    }

    public ResultadoPagina listar(Filtro filtro) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (filtro.status != null) {
            where.append(" AND STATUS = ? ");
            params.add(filtro.status.name());
        }
        if (filtro.apenasDuplicatas) {
            where.append(" AND PRONTUARIO IN (SELECT PRONTUARIO FROM NOTIFICACAO ")
                 .append(" WHERE PRONTUARIO IS NOT NULL AND PRONTUARIO <> '' GROUP BY PRONTUARIO HAVING COUNT(*) > 1) ");
        }
        if (filtro.texto != null && !filtro.texto.trim().isEmpty()) {
            String like = "%" + filtro.texto.trim().toUpperCase() + "%";
            where.append(" AND (UPPER(PRONTUARIO) LIKE ? OR UPPER(GRAVIDADE) LIKE ? OR UPPER(DESCRICAO) LIKE ? " +
                    "OR UPPER(PROTOCOLO) LIKE ? OR UPPER(TIPO) LIKE ?) ");
            for (int i = 0; i < 5; i++) params.add(like);
        }

        try (Connection con = DataSourceFactory.obterConexao()) {
            int total;
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM NOTIFICACAO" + where)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }

            int pagina = Math.max(1, filtro.pagina);
            int tamanho = Math.max(1, filtro.tamanhoPagina);
            int offset = (pagina - 1) * tamanho;
            String sql = "SELECT " + COLUNAS + " FROM NOTIFICACAO" + where +
                    " ORDER BY ID DESC OFFSET " + offset + " ROWS FETCH FIRST " + tamanho + " ROWS ONLY";
            List<Notificacao> itens = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        itens.add(mapear(rs));
                    }
                }
            }
            return new ResultadoPagina(itens, total);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar notificacoes", e);
        }
    }

    /** Quantidade de notificacoes por numero de prontuario, apenas os que se repetem (duplicatas). */
    public Map<String, Integer> contarDuplicatasPorProntuario() {
        String sql = "SELECT PRONTUARIO, COUNT(*) AS QTD FROM NOTIFICACAO " +
                "WHERE PRONTUARIO IS NOT NULL AND PRONTUARIO <> '' GROUP BY PRONTUARIO HAVING COUNT(*) > 1";
        Map<String, Integer> resultado = new HashMap<>();
        try (Connection con = DataSourceFactory.obterConexao();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                resultado.put(rs.getString("PRONTUARIO"), rs.getInt("QTD"));
            }
            return resultado;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar duplicatas por prontuario", e);
        }
    }

    public Map<StatusNotificacao, Integer> contarPorStatus() {
        Map<StatusNotificacao, Integer> resultado = new HashMap<>();
        for (StatusNotificacao s : StatusNotificacao.values()) resultado.put(s, 0);
        String sql = "SELECT STATUS, COUNT(*) AS QTD FROM NOTIFICACAO GROUP BY STATUS";
        try (Connection con = DataSourceFactory.obterConexao();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                resultado.put(StatusNotificacao.valueOf(rs.getString("STATUS")), rs.getInt("QTD"));
            }
            return resultado;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar notificacoes por status", e);
        }
    }

    public Map<TipoNotificacao, Integer> contarPorTipo() {
        Map<TipoNotificacao, Integer> resultado = new HashMap<>();
        for (TipoNotificacao t : TipoNotificacao.values()) resultado.put(t, 0);
        String sql = "SELECT TIPO, COUNT(*) AS QTD FROM NOTIFICACAO GROUP BY TIPO";
        try (Connection con = DataSourceFactory.obterConexao();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                resultado.put(TipoNotificacao.valueOf(rs.getString("TIPO")), rs.getInt("QTD"));
            }
            return resultado;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar notificacoes por tipo", e);
        }
    }

    public int contarCriadasHoje() {
        String sql = "SELECT COUNT(*) FROM NOTIFICACAO WHERE DATE(CRIADO_EM) = CURRENT DATE";
        try (Connection con = DataSourceFactory.obterConexao();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar notificacoes de hoje", e);
        }
    }

    /** Aplica reclassificacao de risco/gravidade e muda o status, dentro de uma transacao propria. */
    public void atualizarRisco(long id, String gravidade, int probabilidade, int impacto,
                                int scoreRisco, NivelRisco nivelRisco, StatusNotificacao novoStatus) {
        try (Connection con = DataSourceFactory.obterConexao()) {
            atualizarRisco(con, id, gravidade, probabilidade, impacto, scoreRisco, nivelRisco, novoStatus);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar risco da notificacao", e);
        }
    }

    public void atualizarRisco(Connection con, long id, String gravidade, int probabilidade, int impacto,
                                int scoreRisco, NivelRisco nivelRisco, StatusNotificacao novoStatus) throws SQLException {
        String sql = "UPDATE NOTIFICACAO SET GRAVIDADE = ?, PROBABILIDADE = ?, IMPACTO = ?, SCORE_RISCO = ?, " +
                "NIVEL_RISCO = ?, STATUS = ?, ATUALIZADO_EM = CURRENT TIMESTAMP WHERE ID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, gravidade);
            ps.setInt(2, probabilidade);
            ps.setInt(3, impacto);
            ps.setInt(4, scoreRisco);
            ps.setString(5, nivelRisco.name());
            ps.setString(6, novoStatus.name());
            ps.setLong(7, id);
            ps.executeUpdate();
        }
    }

    public void atualizarStatus(Connection con, long id, StatusNotificacao novoStatus) throws SQLException {
        String sql = "UPDATE NOTIFICACAO SET STATUS = ?, ATUALIZADO_EM = CURRENT TIMESTAMP WHERE ID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, novoStatus.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private Notificacao mapear(ResultSet rs) throws SQLException {
        Notificacao n = new Notificacao();
        n.setId(rs.getLong("ID"));
        n.setProtocolo(rs.getString("PROTOCOLO"));
        n.setTipo(TipoNotificacao.valueOf(rs.getString("TIPO")));
        n.setSenhaHash(rs.getString("SENHA_HASH"));
        n.setSenhaSalt(rs.getString("SENHA_SALT"));
        n.setDataIncidente(rs.getString("DATA_INCIDENTE"));
        n.setHoraIncidente(rs.getString("HORA_INCIDENTE"));
        n.setSetor(rs.getString("SETOR"));
        n.setProntuario(rs.getString("PRONTUARIO"));
        n.setGravidade(rs.getString("GRAVIDADE"));
        n.setGerouDano(rs.getInt("GEROU_DANO") == 1);
        n.setDescricao(rs.getString("DESCRICAO"));
        n.setAnonimo(rs.getInt("ANONIMO") == 1);
        n.setContatoEmail(rs.getString("CONTATO_EMAIL"));
        n.setContatoTelefone(rs.getString("CONTATO_TELEFONE"));
        n.setStatus(StatusNotificacao.valueOf(rs.getString("STATUS")));
        int prob = rs.getInt("PROBABILIDADE");
        if (!rs.wasNull()) n.setProbabilidade(prob);
        int imp = rs.getInt("IMPACTO");
        if (!rs.wasNull()) n.setImpacto(imp);
        int score = rs.getInt("SCORE_RISCO");
        if (!rs.wasNull()) n.setScoreRisco(score);
        String nivel = rs.getString("NIVEL_RISCO");
        if (nivel != null) n.setNivelRisco(NivelRisco.valueOf(nivel));
        Timestamp criado = rs.getTimestamp("CRIADO_EM");
        n.setCriadoEm(criado);
        n.setAtualizadoEm(rs.getTimestamp("ATUALIZADO_EM"));
        return n;
    }
}
