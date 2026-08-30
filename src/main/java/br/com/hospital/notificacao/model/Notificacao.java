package br.com.hospital.notificacao.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Uma notificacao de seguranca do paciente, do registro via QR Code ate a conclusao. */
public class Notificacao {
    private long id;
    private String protocolo;
    private TipoNotificacao tipo;
    private String senhaHash;
    private String senhaSalt;

    private String dataIncidente;
    private String horaIncidente;
    private String setor;
    private String prontuario;
    private String gravidade;
    private boolean gerouDano;
    private String descricao;

    private boolean anonimo;
    private String contatoEmail;
    private String contatoTelefone;

    private StatusNotificacao status = StatusNotificacao.ABERTO;
    private Integer probabilidade;
    private Integer impacto;
    private Integer scoreRisco;
    private NivelRisco nivelRisco;

    private Timestamp criadoEm;
    private Timestamp atualizadoEm;

    private List<CampoNotificacao> campos = new ArrayList<>();
    private List<Anexo> anexos = new ArrayList<>();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getProtocolo() { return protocolo; }
    public void setProtocolo(String protocolo) { this.protocolo = protocolo; }
    public TipoNotificacao getTipo() { return tipo; }
    public void setTipo(TipoNotificacao tipo) { this.tipo = tipo; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public String getSenhaSalt() { return senhaSalt; }
    public void setSenhaSalt(String senhaSalt) { this.senhaSalt = senhaSalt; }
    public String getDataIncidente() { return dataIncidente; }
    public void setDataIncidente(String dataIncidente) { this.dataIncidente = dataIncidente; }
    public String getHoraIncidente() { return horaIncidente; }
    public void setHoraIncidente(String horaIncidente) { this.horaIncidente = horaIncidente; }
    public String getSetor() { return setor; }
    public void setSetor(String setor) { this.setor = setor; }
    public String getProntuario() { return prontuario; }
    public void setProntuario(String prontuario) { this.prontuario = prontuario; }
    public String getGravidade() { return gravidade; }
    public void setGravidade(String gravidade) { this.gravidade = gravidade; }
    public boolean isGerouDano() { return gerouDano; }
    public void setGerouDano(boolean gerouDano) { this.gerouDano = gerouDano; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public boolean isAnonimo() { return anonimo; }
    public void setAnonimo(boolean anonimo) { this.anonimo = anonimo; }
    public String getContatoEmail() { return contatoEmail; }
    public void setContatoEmail(String contatoEmail) { this.contatoEmail = contatoEmail; }
    public String getContatoTelefone() { return contatoTelefone; }
    public void setContatoTelefone(String contatoTelefone) { this.contatoTelefone = contatoTelefone; }
    public StatusNotificacao getStatus() { return status; }
    public void setStatus(StatusNotificacao status) { this.status = status; }
    public Integer getProbabilidade() { return probabilidade; }
    public void setProbabilidade(Integer probabilidade) { this.probabilidade = probabilidade; }
    public Integer getImpacto() { return impacto; }
    public void setImpacto(Integer impacto) { this.impacto = impacto; }
    public Integer getScoreRisco() { return scoreRisco; }
    public void setScoreRisco(Integer scoreRisco) { this.scoreRisco = scoreRisco; }
    public NivelRisco getNivelRisco() { return nivelRisco; }
    public void setNivelRisco(NivelRisco nivelRisco) { this.nivelRisco = nivelRisco; }
    public Timestamp getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Timestamp criadoEm) { this.criadoEm = criadoEm; }
    public Timestamp getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(Timestamp atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    public List<CampoNotificacao> getCampos() { return campos; }
    public void setCampos(List<CampoNotificacao> campos) { this.campos = campos; }
    public List<Anexo> getAnexos() { return anexos; }
    public void setAnexos(List<Anexo> anexos) { this.anexos = anexos; }

    public String getDetalheResumo() {
        StringBuilder sb = new StringBuilder();
        for (CampoNotificacao c : campos) {
            if (c.getValor() != null && !c.getValor().trim().isEmpty()) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(c.getValor());
            }
        }
        return sb.toString();
    }
}
