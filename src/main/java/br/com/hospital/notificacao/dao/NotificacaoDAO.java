package br.com.hospital.notificacao.dao;

import br.com.hospital.notificacao.config.DataSourceFactory;
import br.com.hospital.notificacao.model.ClassificacaoIncidente;
import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.model.TipoNotificacao;
import br.com.hospital.notificacao.service.MatrizRisco;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NotificacaoDAO {

    private static final String COLUNAS =
            "ID, PROTOCOLO, TIPO, SENHA_HASH, SENHA_SALT, DATA_INCIDENTE, HORA_INCIDENTE, SETOR, " +
            "PRONTUARIO, SUBGRUPO, GRAVIDADE, GEROU_DANO, DESCRICAO, DESCRICAO_REVISADA, ANONIMO, CONTATO_EMAIL, " +
            "CONTATO_TELEFONE, STATUS, PROBABILIDADE, IMPACTO, SCORE_RISCO, NIVEL_RISCO, CRIADO_EM, ATUALIZADO_EM";

    /** Insere a notificacao (cabecalho) dentro de uma transacao controlada pelo chamador. */
    public long inserir(Connection con, Notificacao n) throws SQLException {
        String sql = "INSERT INTO NOTIFICACAO (PROTOCOLO, TIPO, SENHA_HASH, SENHA_SALT, DATA_INCIDENTE, " +
                "HORA_INCIDENTE, SETOR, PRONTUARIO, GRAVIDADE, GEROU_DANO, DESCRICAO, ANONIMO, " +
                "CONTATO_EMAIL, CONTATO_TELEFONE, STATUS, SUBGRUPO) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
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
            ps.setString(16, n.getSubgrupo());
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
        // Usado para compor o protocolo (#PREFIXO-ANO-004821): maior ID atual + 4821, sem uma
        // sequence dedicada. O deslocamento vale para TODAS as notificacoes (antes so' a
        // primeira recebia 4821 e as seguintes voltavam para 000002, 000003... ate colidir).
        String sql = "SELECT COALESCE(MAX(ID), 0) + 4821 FROM NOTIFICACAO";
        try (Connection con = DataSourceFactory.obterConexao();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao calcular proximo sequencial", e);
        }
    }

    /** Ordenacoes permitidas na lista (whitelist: o texto SQL nunca vem do usuario). */
    public enum Ordenacao {
        RECENTES("Mais recentes", "ID DESC"),
        MAIOR_RISCO("Maior risco", "COALESCE(SCORE_RISCO, -1) DESC, ID DESC"),
        PROBLEMA("Problema (A-Z)", "SUBGRUPO, ID DESC"),
        CLASSIFICACAO("Classificação (mais grave)", ordemPorClassificacao() + " DESC, ID DESC");

        private final String rotulo;
        private final String sql;

        Ordenacao(String rotulo, String sql) {
            this.rotulo = rotulo;
            this.sql = sql;
        }

        public String getRotulo() { return rotulo; }
        public String getSql() { return sql; }

        /** CASE que transforma a classificacao no seu grau (Circunstancia de risco=1 ... Obito=7). */
        private static String ordemPorClassificacao() {
            StringBuilder sb = new StringBuilder("CASE GRAVIDADE");
            List<String> todas = ClassificacaoIncidente.TODAS;
            for (int i = 0; i < todas.size(); i++) {
                sb.append(" WHEN '").append(todas.get(i).replace("'", "''")).append("' THEN ").append(i + 1);
            }
            return sb.append(" ELSE 0 END").toString();
        }
    }

    /** Filtros de listagem do painel de notificacoes (e da exportacao em PDF). */
    public static class Filtro {
        public StatusNotificacao status;
        public boolean apenasDuplicatas;
        public String texto;
        public TipoNotificacao tipo;
        public String subgrupo;
        public String gravidade;
        public NivelRisco nivelRisco;
        public String prontuario;
        public String setor;
        /** Data do incidente (dd/MM/aaaa), informada pelo notificante. */
        public String dataInicial;
        public String dataFinal;
        /** Data de recebimento da notificacao (CRIADO_EM). */
        public LocalDate dataDe;
        public LocalDate dataAte;
        public Ordenacao ordenacao = Ordenacao.RECENTES;
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

    /**
     * DATA_INCIDENTE (texto dd/MM/aaaa) reescrita como aaaaMMdd, que ordena como data.
     * Linhas fora do formato (texto livre gravado antes da validacao do wizard) viram NULL
     * e simplesmente nao entram no filtro, em vez de derrubar a consulta inteira — o que
     * acontecia com TIMESTAMP_FORMAT. Tambem e' SQL portavel (roda no H2 dos testes).
     */
    private static final String DATA_INCIDENTE_ORDENAVEL =
            "(CASE WHEN LENGTH(DATA_INCIDENTE) = 10 AND SUBSTR(DATA_INCIDENTE, 3, 1) = '/' " +
            "AND SUBSTR(DATA_INCIDENTE, 6, 1) = '/' THEN SUBSTR(DATA_INCIDENTE, 7, 4) || " +
            "SUBSTR(DATA_INCIDENTE, 4, 2) || SUBSTR(DATA_INCIDENTE, 1, 2) END)";

    /** "dd/MM/aaaa" -> "aaaaMMdd"; null se o texto nao estiver nesse formato. */
    static String dataOrdenavel(String ddMMaaaa) {
        if (ddMMaaaa == null) return null;
        String v = ddMMaaaa.trim();
        if (!v.matches("\\d{2}/\\d{2}/\\d{4}")) return null;
        return v.substring(6, 10) + v.substring(3, 5) + v.substring(0, 2);
    }

    /**
     * Monta o WHERE da listagem e preenche os parametros na mesma ordem dos "?".
     * Separado de {@link #listar(Filtro)} para ser testavel sem DB2.
     */
    static String montarWhere(Filtro filtro, List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");

        if (filtro.status != null) {
            where.append(" AND STATUS = ? ");
            params.add(filtro.status.name());
        }
        if (filtro.apenasDuplicatas) {
            // Duplicata = mesmo prontuario E mesma gravidade. Notificacoes sem prontuario nunca
            // sao duplicatas entre si (mesmo criterio de contarDuplicatasPorProntuario).
            where.append(" AND NOTIFICACAO.PRONTUARIO IS NOT NULL AND NOTIFICACAO.PRONTUARIO <> '' ")
                 .append(" AND EXISTS (SELECT 1 FROM NOTIFICACAO n2 WHERE n2.PRONTUARIO = NOTIFICACAO.PRONTUARIO " +
                    "AND COALESCE(n2.GRAVIDADE, '') = COALESCE(NOTIFICACAO.GRAVIDADE, '') " +
                    "GROUP BY n2.PRONTUARIO, n2.GRAVIDADE HAVING COUNT(*) > 1) ");
        }
        String incidenteDe = dataOrdenavel(filtro.dataInicial);
        if (incidenteDe != null) {
            where.append(" AND ").append(DATA_INCIDENTE_ORDENAVEL).append(" >= ? ");
            params.add(incidenteDe);
        }
        String incidenteAte = dataOrdenavel(filtro.dataFinal);
        if (incidenteAte != null) {
            where.append(" AND ").append(DATA_INCIDENTE_ORDENAVEL).append(" <= ? ");
            params.add(incidenteAte);
        }
        if (filtro.tipo != null) {
            where.append(" AND TIPO = ? ");
            params.add(filtro.tipo.name());
        }
        if (naoVazio(filtro.subgrupo)) {
            where.append(" AND SUBGRUPO = ? ");
            params.add(filtro.subgrupo);
        }
        if (naoVazio(filtro.gravidade)) {
            where.append(" AND GRAVIDADE = ? ");
            params.add(filtro.gravidade);
        }
        if (filtro.nivelRisco != null) {
            where.append(" AND NIVEL_RISCO = ? ");
            params.add(filtro.nivelRisco.name());
        }
        if (naoVazio(filtro.prontuario)) {
            where.append(" AND UPPER(PRONTUARIO) LIKE ? ");
            params.add("%" + filtro.prontuario.trim().toUpperCase() + "%");
        }
        if (naoVazio(filtro.setor)) {
            where.append(" AND SETOR = ? ");
            params.add(filtro.setor);
        }
        if (filtro.dataDe != null) {
            where.append(" AND CRIADO_EM >= ? ");
            params.add(Timestamp.valueOf(filtro.dataDe.atStartOfDay()));
        }
        if (filtro.dataAte != null) {
            where.append(" AND CRIADO_EM < ? ");
            params.add(Timestamp.valueOf(filtro.dataAte.plusDays(1).atStartOfDay()));
        }
        if (naoVazio(filtro.texto)) {
            String like = "%" + filtro.texto.trim().toUpperCase() + "%";
            where.append(" AND (UPPER(PRONTUARIO) LIKE ? OR UPPER(GRAVIDADE) LIKE ? OR UPPER(DESCRICAO) LIKE ? " +
                    "OR UPPER(PROTOCOLO) LIKE ? OR UPPER(TIPO) LIKE ? OR UPPER(SUBGRUPO) LIKE ? " +
                    "OR UPPER(DESCRICAO_REVISADA) LIKE ?) ");
            for (int i = 0; i < 7; i++) params.add(like);
        }
        return where.toString();
    }

    private static boolean naoVazio(String s) {
        return s != null && !s.trim().isEmpty();
    }

    public ResultadoPagina listar(Filtro filtro) {
        List<Object> params = new ArrayList<>();
        String where = montarWhere(filtro, params);

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
                    " ORDER BY " + (filtro.ordenacao != null ? filtro.ordenacao : Ordenacao.RECENTES).getSql() +
                    " OFFSET " + offset + " ROWS FETCH FIRST " + tamanho + " ROWS ONLY";
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
        String sql = "SELECT PRONTUARIO, GRAVIDADE, COUNT(*) AS QTD FROM NOTIFICACAO " +
            "WHERE PRONTUARIO IS NOT NULL AND PRONTUARIO <> '' GROUP BY PRONTUARIO, GRAVIDADE HAVING COUNT(*) > 1";
        Map<String, Integer> resultado = new HashMap<>();
        try (Connection con = DataSourceFactory.obterConexao();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                resultado.put(chaveDuplicidade(rs.getString("PRONTUARIO"), rs.getString("GRAVIDADE")), rs.getInt("QTD"));
            }
            return resultado;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar duplicatas por prontuario", e);
        }
    }

    public static String chaveDuplicidade(String prontuario, String gravidade) {
        return prontuario + "\u0000" + (gravidade == null ? "" : gravidade);
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
        // Intervalo [hoje 00:00, amanha 00:00) em vez de DATE(CRIADO_EM): usa o indice
        // IX_NOTIF_CRIADO e e' o mesmo criterio do filtro de periodo da lista.
        String sql = "SELECT COUNT(*) FROM NOTIFICACAO WHERE CRIADO_EM >= ? AND CRIADO_EM < ?";
        LocalDate hoje = LocalDate.now();
        try (Connection con = DataSourceFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(hoje.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hoje.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar notificacoes de hoje", e);
        }
    }

    /**
     * Aplica reclassificacao de risco/gravidade e muda o status. Recebe sempre a conexao do
     * chamador: com uma conexao propria, uma notificacao recem-inserida (ainda nao
     * commitada) nao seria encontrada e o risco nao seria gravado.
     */
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

    /**
     * Reclassificacao feita pelo NSP: tipo, subgrupo, classificacao, descricao revisada e,
     * opcionalmente (probabilidade/impacto nao nulos), o risco. A DESCRICAO original nunca e' alterada.
     */
    public void atualizarClassificacao(Connection con, long id, TipoNotificacao tipo, String subgrupo,
                                       String gravidade, String descricaoRevisada,
                                       Integer probabilidade, Integer impacto) throws SQLException {
        boolean comRisco = probabilidade != null && impacto != null;
        String sql = "UPDATE NOTIFICACAO SET TIPO = ?, SUBGRUPO = ?, GRAVIDADE = ?, DESCRICAO_REVISADA = ?, " +
                (comRisco ? "PROBABILIDADE = ?, IMPACTO = ?, SCORE_RISCO = ?, NIVEL_RISCO = ?, " : "") +
                "ATUALIZADO_EM = CURRENT TIMESTAMP WHERE ID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, tipo.name());
            ps.setString(i++, subgrupo);
            ps.setString(i++, gravidade);
            ps.setString(i++, descricaoRevisada);
            if (comRisco) {
                int score = MatrizRisco.calcularScore(probabilidade, impacto);
                ps.setInt(i++, probabilidade);
                ps.setInt(i++, impacto);
                ps.setInt(i++, score);
                ps.setString(i++, NivelRisco.doScore(score).name());
            }
            ps.setLong(i, id);
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
        n.setSubgrupo(rs.getString("SUBGRUPO"));
        n.setGravidade(rs.getString("GRAVIDADE"));
        n.setGerouDano(rs.getInt("GEROU_DANO") == 1);
        n.setDescricao(rs.getString("DESCRICAO"));
        n.setDescricaoRevisada(rs.getString("DESCRICAO_REVISADA"));
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
