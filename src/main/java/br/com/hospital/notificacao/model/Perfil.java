package br.com.hospital.notificacao.model;

/**
 * Nivel de acesso ao painel administrativo do NSP.
 * NSP_ANALISTA opera triagem/investigacao; NSP_GESTOR tem acesso total
 * (inclui conclusao de notificacoes e telas de configuracao).
 */
public enum Perfil {
    NSP_ANALISTA("Analista NSP"),
    NSP_GESTOR("Gestor da Qualidade / NSP");

    private final String rotulo;

    Perfil(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
