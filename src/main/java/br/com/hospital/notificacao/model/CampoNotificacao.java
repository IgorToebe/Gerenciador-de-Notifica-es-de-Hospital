package br.com.hospital.notificacao.model;

/** Um campo dinamico preenchido (chave/rotulo/valor), persistido em NOTIFICACAO_CAMPO. */
public class CampoNotificacao {
    private long id;
    private long idNotificacao;
    private String chave;
    private String rotulo;
    private String valor;
    private int ordem;

    public CampoNotificacao() { }

    public CampoNotificacao(String chave, String rotulo, String valor, int ordem) {
        this.chave = chave;
        this.rotulo = rotulo;
        this.valor = valor;
        this.ordem = ordem;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getIdNotificacao() { return idNotificacao; }
    public void setIdNotificacao(long idNotificacao) { this.idNotificacao = idNotificacao; }
    public String getChave() { return chave; }
    public void setChave(String chave) { this.chave = chave; }
    public String getRotulo() { return rotulo; }
    public void setRotulo(String rotulo) { this.rotulo = rotulo; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
}
