package br.com.hospital.notificacao.model;

/** Um marco da timeline do incidente (secao A da investigacao FOR-NSP-014). */
public class MarcoInvestigacao {
    private long id;
    private long idInvestigacao;
    private String quando;
    private String descricao;
    private int ordem;

    public MarcoInvestigacao() { }

    public MarcoInvestigacao(String quando, String descricao, int ordem) {
        this.quando = quando;
        this.descricao = descricao;
        this.ordem = ordem;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getIdInvestigacao() { return idInvestigacao; }
    public void setIdInvestigacao(long idInvestigacao) { this.idInvestigacao = idInvestigacao; }
    public String getQuando() { return quando; }
    public void setQuando(String quando) { this.quando = quando; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
}
