package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.model.NivelRisco;

import java.util.Arrays;
import java.util.List;

/**
 * Matriz de risco Probabilidade x Impacto (Base_Classificacao / ANVISA), identica a regra
 * do protototipo funcional: score = probabilidade x impacto; <=4 Baixo, <=10 Medio,
 * <=24 Alto, >24 Extremo.
 */
public final class MatrizRisco {

    public static final List<Eixo> PROBABILIDADE = Arrays.asList(
            new Eixo("Raro", 1),
            new Eixo("Improvável", 2),
            new Eixo("Possível", 3),
            new Eixo("Provável", 4),
            new Eixo("Quase certo", 5)
    );

    public static final List<Eixo> IMPACTO = Arrays.asList(
            new Eixo("Muito baixo", 1),
            new Eixo("Baixo", 2),
            new Eixo("Médio", 4),
            new Eixo("Alto", 8),
            new Eixo("Extremo", 16)
    );

    private MatrizRisco() { }

    public static int calcularScore(int probabilidade, int impacto) {
        return probabilidade * impacto;
    }

    public static NivelRisco calcularNivel(int probabilidade, int impacto) {
        return NivelRisco.doScore(calcularScore(probabilidade, impacto));
    }

    /** Um ponto de um dos eixos da matriz (rotulo + peso numerico). */
    public static final class Eixo {
        private final String rotulo;
        private final int valor;

        public Eixo(String rotulo, int valor) {
            this.rotulo = rotulo;
            this.valor = valor;
        }

        public String getRotulo() { return rotulo; }
        public int getValor() { return valor; }
    }
}
