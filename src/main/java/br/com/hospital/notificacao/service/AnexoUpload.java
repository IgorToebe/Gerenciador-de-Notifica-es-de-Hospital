package br.com.hospital.notificacao.service;

/** Dados brutos de um arquivo anexado no passo 3 do wizard, antes de ser persistido. */
public class AnexoUpload {
    private final String nomeOriginal;
    private final String mime;
    private final byte[] conteudo;

    public AnexoUpload(String nomeOriginal, String mime, byte[] conteudo) {
        this.nomeOriginal = nomeOriginal;
        this.mime = mime;
        this.conteudo = conteudo;
    }

    public String getNomeOriginal() { return nomeOriginal; }
    public String getMime() { return mime; }
    public byte[] getConteudo() { return conteudo; }
}
