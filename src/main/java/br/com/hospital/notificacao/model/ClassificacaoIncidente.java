package br.com.hospital.notificacao.model;

import java.util.Arrays;
import java.util.List;

/**
 * Classificacao do incidente (pre-classificacao de gravidade feita no wizard e
 * reclassificada pelo NSP). Fonte unica para o wizard, a triagem, os filtros da lista e
 * o painel de reclassificacao — antes cada tela tinha sua propria copia da lista.
 */
public final class ClassificacaoIncidente {

    public static final List<String> TODAS = Arrays.asList(
            "Circunstância de risco",
            "Near miss (quase erro)",
            "Incidente sem dano",
            "Dano leve",
            "Dano moderado",
            "Dano grave",
            "Óbito"
    );

    private ClassificacaoIncidente() { }

    public static boolean valida(String classificacao) {
        return classificacao != null && TODAS.contains(classificacao);
    }
}
