package br.com.hospital.notificacao.service;

/**
 * Retorno da criacao de uma notificacao: protocolo e senha em texto puro, exibidos
 * uma unica vez na tela de conclusao (nao ficam recuperaveis depois, so o hash e' salvo).
 */
public class ResultadoNotificacao {
    private final String protocolo;
    private final String senha;
    private final boolean comContato;
    private final String contatoMascarado;

    public ResultadoNotificacao(String protocolo, String senha, boolean comContato, String contatoMascarado) {
        this.protocolo = protocolo;
        this.senha = senha;
        this.comContato = comContato;
        this.contatoMascarado = contatoMascarado;
    }

    public String getProtocolo() { return protocolo; }
    public String getSenha() { return senha; }
    public boolean isComContato() { return comContato; }
    public String getContatoMascarado() { return contatoMascarado; }
}
