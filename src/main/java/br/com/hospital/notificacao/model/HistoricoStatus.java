package br.com.hospital.notificacao.model;

import java.sql.Timestamp;

/** Trilha cronologica de mudancas de status de uma notificacao (Item 4 dos criterios de aceitacao). */
public class HistoricoStatus {
    private long id;
    private long idNotificacao;
    private StatusNotificacao statusAnterior;
    private StatusNotificacao statusNovo;
    private Long idUsuario;
    private String observacao;
    private Timestamp criadoEm;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getIdNotificacao() { return idNotificacao; }
    public void setIdNotificacao(long idNotificacao) { this.idNotificacao = idNotificacao; }
    public StatusNotificacao getStatusAnterior() { return statusAnterior; }
    public void setStatusAnterior(StatusNotificacao statusAnterior) { this.statusAnterior = statusAnterior; }
    public StatusNotificacao getStatusNovo() { return statusNovo; }
    public void setStatusNovo(StatusNotificacao statusNovo) { this.statusNovo = statusNovo; }
    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public Timestamp getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Timestamp criadoEm) { this.criadoEm = criadoEm; }
}
