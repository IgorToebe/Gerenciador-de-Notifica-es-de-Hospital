package br.com.hospital.notificacao.model;

import java.util.ArrayList;
import java.util.List;

/** Investigacao e analise de causa raiz (FOR-NSP-014), protocolo de Londres + 5 Porques. */
public class Investigacao {
    private long id;
    private long idNotificacao;
    private String parecerista;
    private String dataInvestigacao;
    private String fontes;

    private String fatorProfissional;
    private String fatorComunicacao;
    private String fatorPaciente;
    private String fatorAmbiente;
    private String fatorOrganizacional;
    private String fatorExterno;

    private String porque1;
    private String porque2;
    private String porque3;
    private String porque4;
    private String causaRaiz; // 5o porque, obrigatorio para gerar o plano de acao

    private String deteccao;
    private String atenuantes;
    private String parecer;
    private boolean concluida;

    private List<MarcoInvestigacao> timeline = new ArrayList<>();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getIdNotificacao() { return idNotificacao; }
    public void setIdNotificacao(long idNotificacao) { this.idNotificacao = idNotificacao; }
    public String getParecerista() { return parecerista; }
    public void setParecerista(String parecerista) { this.parecerista = parecerista; }
    public String getDataInvestigacao() { return dataInvestigacao; }
    public void setDataInvestigacao(String dataInvestigacao) { this.dataInvestigacao = dataInvestigacao; }
    public String getFontes() { return fontes; }
    public void setFontes(String fontes) { this.fontes = fontes; }
    public String getFatorProfissional() { return fatorProfissional; }
    public void setFatorProfissional(String v) { this.fatorProfissional = v; }
    public String getFatorComunicacao() { return fatorComunicacao; }
    public void setFatorComunicacao(String v) { this.fatorComunicacao = v; }
    public String getFatorPaciente() { return fatorPaciente; }
    public void setFatorPaciente(String v) { this.fatorPaciente = v; }
    public String getFatorAmbiente() { return fatorAmbiente; }
    public void setFatorAmbiente(String v) { this.fatorAmbiente = v; }
    public String getFatorOrganizacional() { return fatorOrganizacional; }
    public void setFatorOrganizacional(String v) { this.fatorOrganizacional = v; }
    public String getFatorExterno() { return fatorExterno; }
    public void setFatorExterno(String v) { this.fatorExterno = v; }
    public String getPorque1() { return porque1; }
    public void setPorque1(String v) { this.porque1 = v; }
    public String getPorque2() { return porque2; }
    public void setPorque2(String v) { this.porque2 = v; }
    public String getPorque3() { return porque3; }
    public void setPorque3(String v) { this.porque3 = v; }
    public String getPorque4() { return porque4; }
    public void setPorque4(String v) { this.porque4 = v; }
    public String getCausaRaiz() { return causaRaiz; }
    public void setCausaRaiz(String causaRaiz) { this.causaRaiz = causaRaiz; }
    public String getDeteccao() { return deteccao; }
    public void setDeteccao(String deteccao) { this.deteccao = deteccao; }
    public String getAtenuantes() { return atenuantes; }
    public void setAtenuantes(String atenuantes) { this.atenuantes = atenuantes; }
    public String getParecer() { return parecer; }
    public void setParecer(String parecer) { this.parecer = parecer; }
    public boolean isConcluida() { return concluida; }
    public void setConcluida(boolean concluida) { this.concluida = concluida; }
    public List<MarcoInvestigacao> getTimeline() { return timeline; }
    public void setTimeline(List<MarcoInvestigacao> timeline) { this.timeline = timeline; }
}
