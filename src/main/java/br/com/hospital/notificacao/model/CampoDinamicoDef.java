package br.com.hospital.notificacao.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Definicao de um campo dinamico do passo 2 do wizard, especifico por TipoNotificacao
 * (ex.: "Produto/equipamento" na Tecnovigilancia, "Medicamento" na Farmacovigilancia).
 * O valor preenchido e' persistido em NOTIFICACAO_CAMPO (chave/rotulo/valor), sem exigir
 * uma tabela por tipo.
 */
public class CampoDinamicoDef {

    public enum Tipo { TEXTO, SELECT, TOGGLE }

    private final String chave;
    private final String rotulo;
    private final Tipo tipo;
    private final boolean obrigatorio;
    private final List<String> opcoes;

    private CampoDinamicoDef(String chave, String rotulo, Tipo tipo, boolean obrigatorio, List<String> opcoes) {
        this.chave = chave;
        this.rotulo = rotulo;
        this.tipo = tipo;
        this.obrigatorio = obrigatorio;
        this.opcoes = opcoes;
    }

    public static CampoDinamicoDef texto(String chave, String rotulo, boolean obrigatorio) {
        return new CampoDinamicoDef(chave, rotulo, Tipo.TEXTO, obrigatorio, Collections.emptyList());
    }

    public static CampoDinamicoDef select(String chave, String rotulo, boolean obrigatorio, String... opcoes) {
        return new CampoDinamicoDef(chave, rotulo, Tipo.SELECT, obrigatorio, Arrays.asList(opcoes));
    }

    public static CampoDinamicoDef toggle(String chave, String rotulo, String opcaoA, String opcaoB) {
        return new CampoDinamicoDef(chave, rotulo, Tipo.TOGGLE, false, Arrays.asList(opcaoA, opcaoB));
    }

    public String getChave() { return chave; }
    public String getRotulo() { return rotulo; }
    public Tipo getTipo() { return tipo; }
    public boolean isObrigatorio() { return obrigatorio; }
    public List<String> getOpcoes() { return opcoes; }
}
