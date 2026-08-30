package br.com.hospital.notificacao.model;

/**
 * Niveis da matriz de risco Probabilidade x Impacto (Base_Classificacao / ANVISA).
 * score = probabilidade(1-5) x impacto(1,2,4,8,16); faixas identicas ao protótipo funcional.
 */
public enum NivelRisco {
    BAIXO("Baixo", "baixo"),
    MEDIO("Médio", "medio"),
    ALTO("Alto", "alto"),
    EXTREMO("Extremo", "extremo");

    private final String rotulo;
    private final String chave;

    NivelRisco(String rotulo, String chave) {
        this.rotulo = rotulo;
        this.chave = chave;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getChave() {
        return chave;
    }

    public static NivelRisco doScore(int score) {
        if (score <= 4) return BAIXO;
        if (score <= 10) return MEDIO;
        if (score <= 24) return ALTO;
        return EXTREMO;
    }
}
