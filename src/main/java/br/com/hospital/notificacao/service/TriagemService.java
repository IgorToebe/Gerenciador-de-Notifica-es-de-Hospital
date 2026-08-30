package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.config.DataSourceFactory;
import br.com.hospital.notificacao.dao.HistoricoDAO;
import br.com.hospital.notificacao.dao.InvestigacaoDAO;
import br.com.hospital.notificacao.dao.NotificacaoDAO;
import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.StatusNotificacao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Triagem e classificacao de risco (Item 3 dos criterios de aceitacao): reclassificacao
 * de gravidade, aplicacao da matriz de risco Probabilidade x Impacto e avanco para
 * investigacao.
 */
public class TriagemService {

    private final NotificacaoDAO notificacaoDAO = new NotificacaoDAO();
    private final HistoricoDAO historicoDAO = new HistoricoDAO();
    private final InvestigacaoDAO investigacaoDAO = new InvestigacaoDAO();

    /** Nivel Alto ou Extremo dispara alerta visual ao parecerista, conforme o prototipo. */
    public boolean requerAlerta(NivelRisco nivel) {
        return nivel == NivelRisco.ALTO || nivel == NivelRisco.EXTREMO;
    }

    public NivelRisco aplicarClassificacao(long idNotificacao, String gravidade, int probabilidade, int impacto,
                                            Long idUsuario) {
        Notificacao atual = notificacaoDAO.buscarPorId(idNotificacao)
                .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada: " + idNotificacao));
        int score = MatrizRisco.calcularScore(probabilidade, impacto);
        NivelRisco nivel = NivelRisco.doScore(score);
        StatusNotificacao statusAnterior = atual.getStatus();
        StatusNotificacao novoStatus = statusAnterior == StatusNotificacao.ABERTO
                ? StatusNotificacao.TRIAGEM : statusAnterior;

        try (Connection con = DataSourceFactory.obterConexao()) {
            con.setAutoCommit(false);
            try {
                notificacaoDAO.atualizarRisco(con, idNotificacao, gravidade, probabilidade, impacto, score, nivel, novoStatus);
                if (novoStatus != statusAnterior) {
                    historicoDAO.inserir(con, idNotificacao, statusAnterior, novoStatus, idUsuario,
                            "Triagem iniciada · risco reclassificado para " + nivel.getRotulo());
                } else {
                    historicoDAO.inserir(con, idNotificacao, statusAnterior, statusAnterior, idUsuario,
                            "Risco reclassificado para " + nivel.getRotulo() + " (" + score + ")");
                }
                con.commit();
            } catch (RuntimeException | SQLException e) {
                con.rollback();
                throw (e instanceof RuntimeException) ? (RuntimeException) e
                        : new RuntimeException("Erro ao aplicar classificação de risco", e);
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao abrir conexão para triagem", e);
        }
        return nivel;
    }

    /** Envia a notificacao para investigacao formal (FOR-NSP-014), criando o registro se necessario. */
    public void iniciarInvestigacao(long idNotificacao, Long idUsuario) {
        Notificacao atual = notificacaoDAO.buscarPorId(idNotificacao)
                .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada: " + idNotificacao));
        StatusNotificacao statusAnterior = atual.getStatus();

        try (Connection con = DataSourceFactory.obterConexao()) {
            con.setAutoCommit(false);
            try {
                notificacaoDAO.atualizarStatus(con, idNotificacao, StatusNotificacao.INVESTIGACAO);
                historicoDAO.inserir(con, idNotificacao, statusAnterior, StatusNotificacao.INVESTIGACAO, idUsuario,
                        "Notificação enviada para investigação (FOR-NSP-014)");
                con.commit();
            } catch (RuntimeException | SQLException e) {
                con.rollback();
                throw (e instanceof RuntimeException) ? (RuntimeException) e
                        : new RuntimeException("Erro ao iniciar investigação", e);
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao abrir conexão para iniciar investigação", e);
        }
        investigacaoDAO.criarSeNaoExiste(idNotificacao);
    }
}
