package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.model.AcaoPlano;
import br.com.hospital.notificacao.model.HistoricoStatus;
import br.com.hospital.notificacao.model.Investigacao;
import br.com.hospital.notificacao.model.Notificacao;

import java.util.List;

/**
 * Tudo o que foi registrado sobre uma notificacao (dados do wizard, investigacao, plano
 * de acao e historico). Alimenta a tela de Detalhes e a exportacao em PDF.
 */
public class DetalheNotificacao {
    private final Notificacao notificacao;
    private final Investigacao investigacao;
    private final List<AcaoPlano> acoes;
    private final List<HistoricoStatus> historico;

    public DetalheNotificacao(Notificacao notificacao, Investigacao investigacao,
                              List<AcaoPlano> acoes, List<HistoricoStatus> historico) {
        this.notificacao = notificacao;
        this.investigacao = investigacao;
        this.acoes = acoes;
        this.historico = historico;
    }

    public Notificacao getNotificacao() { return notificacao; }
    /** Nulo enquanto a notificacao nao chegou a etapa de investigacao. */
    public Investigacao getInvestigacao() { return investigacao; }
    public List<AcaoPlano> getAcoes() { return acoes; }
    public List<HistoricoStatus> getHistorico() { return historico; }
}
