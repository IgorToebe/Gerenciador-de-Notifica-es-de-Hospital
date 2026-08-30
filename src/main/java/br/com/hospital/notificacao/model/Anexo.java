package br.com.hospital.notificacao.model;

import java.sql.Timestamp;

public class Anexo {
    private long id;
    private long idNotificacao;
    private String nomeOriginal;
    private String nomeFisico;
    private String tipoArquivo;
    private long tamanhoBytes;
    private Timestamp dataUpload;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getIdNotificacao() { return idNotificacao; }
    public void setIdNotificacao(long idNotificacao) { this.idNotificacao = idNotificacao; }
    public String getNomeOriginal() { return nomeOriginal; }
    public void setNomeOriginal(String nomeOriginal) { this.nomeOriginal = nomeOriginal; }
    public String getNomeFisico() { return nomeFisico; }
    public void setNomeFisico(String nomeFisico) { this.nomeFisico = nomeFisico; }
    public String getTipoArquivo() { return tipoArquivo; }
    public void setTipoArquivo(String tipoArquivo) { this.tipoArquivo = tipoArquivo; }
    public long getTamanhoBytes() { return tamanhoBytes; }
    public void setTamanhoBytes(long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }
    public Timestamp getDataUpload() { return dataUpload; }
    public void setDataUpload(Timestamp dataUpload) { this.dataUpload = dataUpload; }
}
