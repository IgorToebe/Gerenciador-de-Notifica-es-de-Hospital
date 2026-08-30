package br.com.hospital.notificacao.model;

/** Status de uma acao dentro do plano 5W2H. */
public enum StatusAcao {
    INICIAR("A iniciar"),
    ANDAMENTO("Em andamento"),
    ATRASADO("Atrasado"),
    PARALISADO("Paralisado"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado");

    private final String rotulo;

    StatusAcao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public boolean finalizavel() {
        return this == CONCLUIDO || this == CANCELADO;
    }
}
