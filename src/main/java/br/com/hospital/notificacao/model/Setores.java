package br.com.hospital.notificacao.model;

import java.util.Arrays;
import java.util.List;

/** Setores/unidades do HU oferecidos no wizard e no filtro da lista de notificacoes. */
public final class Setores {

    public static final List<String> TODOS = Arrays.asList(
            "Pronto Socorro",
            "Enfermaria",
            "UTI Adulto",
            "UTI Neonatal",
            "Maternidade",
            "Centro Cirúrgico",
            "Farmácia",
            "Ambulatório"
    );

    private Setores() { }
}
