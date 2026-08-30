package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.model.TipoNotificacao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dados coletados nos 4 passos do wizard de notificacao, antes de persistir. */
public class NovaNotificacaoRequest {
    private TipoNotificacao tipo;
    private String dataIncidente;
    private String horaIncidente;
    private String setor;
    private String prontuario;
    private String gravidade;
    private boolean gerouDano;
    private String descricao;
    private final Map<String, String> camposDinamicos = new LinkedHashMap<>();
    private final List<AnexoUpload> anexos = new ArrayList<>();
    private String contatoEmail;
    private String contatoTelefone;

    public TipoNotificacao getTipo() { return tipo; }
    public void setTipo(TipoNotificacao tipo) { this.tipo = tipo; }
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
    public Map<String, String> getCamposDinamicos() { return camposDinamicos; }
    public List<AnexoUpload> getAnexos() { return anexos; }
    public String getContatoEmail() { return contatoEmail; }
    public void setContatoEmail(String contatoEmail) { this.contatoEmail = contatoEmail; }
    public String getContatoTelefone() { return contatoTelefone; }
    public void setContatoTelefone(String contatoTelefone) { this.contatoTelefone = contatoTelefone; }

    public boolean temContato() {
        return (contatoEmail != null && !contatoEmail.trim().isEmpty())
                || (contatoTelefone != null && !contatoTelefone.trim().isEmpty());
    }
}
