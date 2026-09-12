package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.model.CampoDinamicoDef;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

/** Valida os dados da notificacao independentemente da camada ZK. */
public final class ValidacaoNotificacao {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern HORA = Pattern.compile("(?:[01]\\d|2[0-3]):[0-5]\\d");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private ValidacaoNotificacao() { }

    public static void validar(NovaNotificacaoRequest req) {
        if (req == null || req.getTipo() == null) exigir("Tipo de notificação é obrigatório");
        exigirTexto(req.getDataIncidente(), "Data do incidente é obrigatória");
        validarData(req.getDataIncidente(), "Data do incidente");
        validarHora(req.getHoraIncidente());
        exigirTexto(req.getSetor(), "Setor / unidade é obrigatório");
        exigirTexto(req.getGravidade(), "Pré-classificação da gravidade é obrigatória");
        exigirTexto(req.getDescricao(), "Descrição do incidente é obrigatória");
        validarTamanho(req.getDescricao(), 4000, "Descrição do incidente");
        validarTamanho(req.getProntuario(), 40, "Prontuário");

        if (naoVazio(req.getContatoEmail())) {
            validarTamanho(req.getContatoEmail(), 160, "E-mail");
            if (!EMAIL.matcher(req.getContatoEmail().trim()).matches()) exigir("Informe um e-mail válido");
        }
        if (naoVazio(req.getContatoTelefone())) {
            String somenteDigitos = req.getContatoTelefone().replaceAll("\\D", "");
            if (somenteDigitos.length() < 10 || somenteDigitos.length() > 11) {
                exigir("Informe um telefone válido com DDD");
            }
        }

        for (CampoDinamicoDef campo : req.getTipo().getCamposDinamicos()) {
            String valor = req.getCamposDinamicos().get(campo.getChave());
            if (campo.isObrigatorio() && !naoVazio(valor)) exigir("Preencha o campo obrigatório: " + campo.getRotulo());
            validarTamanho(valor, 500, campo.getRotulo());
        }
    }

    public static void validarData(String valor, String campo) {
        if (!naoVazio(valor)) return;
        try {
            LocalDate data = LocalDate.parse(valor.trim(), DATA);
            if (data.isAfter(LocalDate.now())) exigir(campo + " não pode estar no futuro");
        } catch (DateTimeParseException e) {
            exigir(campo + " deve usar o formato dd/MM/aaaa");
        }
    }

    public static void validarHora(String valor) {
        if (naoVazio(valor) && !HORA.matcher(valor.trim()).matches()) exigir("Hora deve usar o formato HH:mm");
    }

    public static void validarIntervaloDatas(String inicio, String fim) {
        if (!naoVazio(inicio) || !naoVazio(fim) || "—".equals(inicio.trim()) || "—".equals(fim.trim())) return;
        try {
            LocalDate dataInicio = LocalDate.parse(inicio.trim(), DATA);
            LocalDate dataFim = LocalDate.parse(fim.trim(), DATA);
            if (dataFim.isBefore(dataInicio)) exigir("Data final não pode ser anterior à data inicial");
        } catch (DateTimeParseException e) {
            exigir("As datas do plano devem usar o formato dd/MM/aaaa");
        }
    }

    private static void exigirTexto(String valor, String mensagem) {
        if (!naoVazio(valor)) exigir(mensagem);
    }

    private static void validarTamanho(String valor, int maximo, String campo) {
        if (valor != null && valor.trim().length() > maximo) exigir(campo + " excede o limite de " + maximo + " caracteres");
    }

    private static boolean naoVazio(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    private static void exigir(String mensagem) {
        throw new IllegalArgumentException(mensagem);
    }
}
