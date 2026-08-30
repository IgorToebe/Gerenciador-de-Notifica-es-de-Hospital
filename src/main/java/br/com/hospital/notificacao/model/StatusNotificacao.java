package br.com.hospital.notificacao.model;

/**
 * Fluxo cronologico obrigatorio de uma notificacao (Item 4 dos criterios de aceitacao):
 * Aberto -> Em Analise (triagem) -> Investigacao da Causa Raiz -> Plano de Acao -> Concluido.
 */
public enum StatusNotificacao {
    ABERTO("Aberto", "aberto", 1),
    TRIAGEM("Em triagem", "analise", 2),
    INVESTIGACAO("Em investigação", "invest", 3),
    PLANO("Plano em execução", "plano", 4),
    CONCLUIDO("Concluído", "concl", 5);

    private final String rotulo;
    private final String classeCss;
    private final int passo;

    StatusNotificacao(String rotulo, String classeCss, int passo) {
        this.rotulo = rotulo;
        this.classeCss = classeCss;
        this.passo = passo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getClasseCss() {
        return classeCss;
    }

    public int getPasso() {
        return passo;
    }
}
