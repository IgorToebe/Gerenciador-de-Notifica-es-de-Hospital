package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.config.DataSourceFactory;
import br.com.hospital.notificacao.dao.HistoricoDAO;
import br.com.hospital.notificacao.dao.InvestigacaoDAO;
import br.com.hospital.notificacao.dao.NotificacaoDAO;
import br.com.hospital.notificacao.dao.PlanoAcaoDAO;
import br.com.hospital.notificacao.model.AcaoPlano;
import br.com.hospital.notificacao.model.Investigacao;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.StatusAcao;
import br.com.hospital.notificacao.model.StatusNotificacao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Investigacao e analise de causa raiz (FOR-NSP-014): timeline, fatores contribuintes
 * (Protocolo de Londres) e 5 Porques. O Item 4 dos criterios de aceitacao exige que a
 * causa raiz seja obrigatoria para encerrar a investigacao e gerar o plano de acao.
 */
public class InvestigacaoService {

    private final InvestigacaoDAO investigacaoDAO = new InvestigacaoDAO();
    private final NotificacaoDAO notificacaoDAO = new NotificacaoDAO();
    private final HistoricoDAO historicoDAO = new HistoricoDAO();
    private final PlanoAcaoDAO planoAcaoDAO = new PlanoAcaoDAO();

    public Investigacao carregarOuCriar(long idNotificacao) {
        return investigacaoDAO.buscarPorNotificacao(idNotificacao)
                .orElseGet(() -> {
                    long id = investigacaoDAO.criarSeNaoExiste(idNotificacao);
                    Investigacao inv = new Investigacao();
                    inv.setId(id);
                    inv.setIdNotificacao(idNotificacao);
                    return inv;
                });
    }

    public void salvarRascunho(Investigacao inv) {
        investigacaoDAO.salvar(inv);
    }

    public void adicionarMarco(long idInvestigacao, String quando, String descricao) {
        investigacaoDAO.adicionarMarco(idInvestigacao, quando, descricao);
    }

    /**
     * Encerra a investigacao e gera o plano de acao 5W2H a partir da causa raiz.
     * Bloqueia se a causa raiz (5o porque) nao estiver preenchida.
     */
    public void gerarPlanoDeAcao(Investigacao inv, String setorNotificacao, Long idUsuario) {
        if (inv.getCausaRaiz() == null || inv.getCausaRaiz().trim().isEmpty()) {
            throw new IllegalStateException("É obrigatório registrar a causa raiz (5º Porquê) para gerar o plano de ação");
        }
        inv.setConcluida(true);
        investigacaoDAO.salvar(inv);

        Notificacao n = notificacaoDAO.buscarPorId(inv.getIdNotificacao())
                .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada: " + inv.getIdNotificacao()));
        StatusNotificacao statusAnterior = n.getStatus();

        if (!planoAcaoDAO.existePlano(inv.getIdNotificacao())) {
            AcaoPlano primeira = new AcaoPlano();
            primeira.setIdNotificacao(inv.getIdNotificacao());
            primeira.setCodigo("1.0");
            primeira.setoQue("Definir ação corretiva para a causa raiz identificada");
            primeira.setQuem("Coordenação da unidade");
            primeira.setOnde(setorNotificacao);
            primeira.setPorque(inv.getCausaRaiz());
            primeira.setComo("");
            primeira.setQuanto("R$ 0,00");
            primeira.setDataInicio("—");
            primeira.setDataFim("—");
            primeira.setStatus(StatusAcao.INICIAR);
            primeira.setOrdem(1);
            planoAcaoDAO.inserir(primeira);
        }

        try (Connection con = DataSourceFactory.obterConexao()) {
            con.setAutoCommit(false);
            try {
                notificacaoDAO.atualizarStatus(con, n.getId(), StatusNotificacao.PLANO);
                historicoDAO.inserir(con, n.getId(), statusAnterior, StatusNotificacao.PLANO, idUsuario,
                        "Plano de ação gerado a partir da causa raiz identificada");
                con.commit();
            } catch (RuntimeException | SQLException e) {
                con.rollback();
                throw (e instanceof RuntimeException) ? (RuntimeException) e
                        : new RuntimeException("Erro ao avançar para plano de ação", e);
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao abrir conexão para gerar plano de ação", e);
        }
    }
}
