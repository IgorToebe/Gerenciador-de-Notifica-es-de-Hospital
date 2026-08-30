package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.config.DataSourceFactory;
import br.com.hospital.notificacao.dao.HistoricoDAO;
import br.com.hospital.notificacao.dao.NotificacaoDAO;
import br.com.hospital.notificacao.dao.PlanoAcaoDAO;
import br.com.hospital.notificacao.model.AcaoPlano;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.StatusAcao;
import br.com.hospital.notificacao.model.StatusNotificacao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Plano de acao 5W2H (SGQ-001): edicao das acoes corretivas e conclusao da notificacao
 * quando todas as acoes estiverem Concluidas ou Canceladas.
 */
public class PlanoAcaoService {

    private final PlanoAcaoDAO planoAcaoDAO = new PlanoAcaoDAO();
    private final NotificacaoDAO notificacaoDAO = new NotificacaoDAO();
    private final HistoricoDAO historicoDAO = new HistoricoDAO();

    public List<AcaoPlano> listar(long idNotificacao) {
        return planoAcaoDAO.listarPorNotificacao(idNotificacao);
    }

    public void adicionarAcao(long idNotificacao, String causaRaizPadrao, String setorPadrao) {
        List<AcaoPlano> existentes = planoAcaoDAO.listarPorNotificacao(idNotificacao);
        AcaoPlano a = new AcaoPlano();
        a.setIdNotificacao(idNotificacao);
        a.setCodigo((existentes.size() + 1) + ".0");
        a.setoQue("Nova ação");
        a.setQuem("");
        a.setOnde(setorPadrao);
        a.setPorque(causaRaizPadrao);
        a.setComo("");
        a.setQuanto("R$ 0,00");
        a.setDataInicio("—");
        a.setDataFim("—");
        a.setStatus(StatusAcao.INICIAR);
        a.setOrdem(existentes.size() + 1);
        planoAcaoDAO.inserir(a);
    }

    public void atualizarStatus(long idAcao, StatusAcao status) {
        planoAcaoDAO.atualizarStatus(idAcao, status);
    }

    public void atualizarEvidencia(long idAcao, String evidencia) {
        planoAcaoDAO.atualizarEvidencia(idAcao, evidencia);
    }

    public boolean podeConcluir(long idNotificacao) {
        List<AcaoPlano> acoes = planoAcaoDAO.listarPorNotificacao(idNotificacao);
        return !acoes.isEmpty() && planoAcaoDAO.todasFinalizadas(idNotificacao);
    }

    public void concluirNotificacao(long idNotificacao, Long idUsuario) {
        if (!podeConcluir(idNotificacao)) {
            throw new IllegalStateException("Conclua ou cancele todas as ações para fechar a notificação");
        }
        Notificacao n = notificacaoDAO.buscarPorId(idNotificacao)
                .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada: " + idNotificacao));
        StatusNotificacao statusAnterior = n.getStatus();

        try (Connection con = DataSourceFactory.obterConexao()) {
            con.setAutoCommit(false);
            try {
                notificacaoDAO.atualizarStatus(con, idNotificacao, StatusNotificacao.CONCLUIDO);
                historicoDAO.inserir(con, idNotificacao, statusAnterior, StatusNotificacao.CONCLUIDO, idUsuario,
                        "Notificação concluída · plano de ação executado · indicadores alimentados");
                con.commit();
            } catch (RuntimeException | SQLException e) {
                con.rollback();
                throw (e instanceof RuntimeException) ? (RuntimeException) e
                        : new RuntimeException("Erro ao concluir notificação", e);
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao abrir conexão para concluir notificação", e);
        }
    }
}
