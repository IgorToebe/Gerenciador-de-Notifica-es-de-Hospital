package br.com.hospital.notificacao.model;

/** Uma linha do plano de acao 5W2H (SGQ-001). */
public class AcaoPlano {
    private long id;
    private long idNotificacao;
    private String codigo;
    private String oQue;
    private String quem;
    private String onde;
    private String porque;
    private String como;
    private String quanto;
    private String dataInicio;
    private String dataFim;
    private StatusAcao status = StatusAcao.INICIAR;
    private String evidencia;
    private int ordem;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getIdNotificacao() { return idNotificacao; }
    public void setIdNotificacao(long idNotificacao) { this.idNotificacao = idNotificacao; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getoQue() { return oQue; }
    public void setoQue(String oQue) { this.oQue = oQue; }
    public String getQuem() { return quem; }
    public void setQuem(String quem) { this.quem = quem; }
    public String getOnde() { return onde; }
    public void setOnde(String onde) { this.onde = onde; }
    public String getPorque() { return porque; }
    public void setPorque(String porque) { this.porque = porque; }
    public String getComo() { return como; }
    public void setComo(String como) { this.como = como; }
    public String getQuanto() { return quanto; }
    public void setQuanto(String quanto) { this.quanto = quanto; }
    public String getDataInicio() { return dataInicio; }
    public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }
    public String getDataFim() { return dataFim; }
    public void setDataFim(String dataFim) { this.dataFim = dataFim; }
    public StatusAcao getStatus() { return status; }
    public void setStatus(StatusAcao status) { this.status = status; }
    public String getEvidencia() { return evidencia; }
    public void setEvidencia(String evidencia) { this.evidencia = evidencia; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
}
