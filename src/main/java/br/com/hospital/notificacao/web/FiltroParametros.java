package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.dao.NotificacaoDAO;
import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.model.TipoNotificacao;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Function;

/**
 * Converte o filtro da lista de notificacoes em query string e vice-versa, para que a
 * exportacao em PDF (ExportacaoServlet) gere exatamente o que esta' filtrado na tela.
 * Valores invalidos na URL sao ignorados (o filtro correspondente fica vazio).
 */
public final class FiltroParametros {

    private FiltroParametros() { }

    public static String paraQueryString(NotificacaoDAO.Filtro f) {
        StringBuilder sb = new StringBuilder();
        adicionar(sb, "status", f.status != null ? f.status.name() : null);
        adicionar(sb, "dup", f.apenasDuplicatas ? "1" : null);
        adicionar(sb, "q", f.texto);
        adicionar(sb, "tipo", f.tipo != null ? f.tipo.getChaveUrl() : null);
        adicionar(sb, "subgrupo", f.subgrupo);
        adicionar(sb, "gravidade", f.gravidade);
        adicionar(sb, "risco", f.nivelRisco != null ? f.nivelRisco.name() : null);
        adicionar(sb, "prontuario", f.prontuario);
        adicionar(sb, "setor", f.setor);
        adicionar(sb, "de", f.dataDe != null ? f.dataDe.toString() : null);
        adicionar(sb, "ate", f.dataAte != null ? f.dataAte.toString() : null);
        adicionar(sb, "ordem", f.ordenacao != null ? f.ordenacao.name() : null);
        return sb.toString();
    }

    /** @param parametro normalmente {@code request::getParameter} */
    public static NotificacaoDAO.Filtro deParametros(Function<String, String> parametro) {
        NotificacaoDAO.Filtro f = new NotificacaoDAO.Filtro();
        f.status = enumOuNulo(StatusNotificacao.class, parametro.apply("status"));
        f.apenasDuplicatas = "1".equals(parametro.apply("dup"));
        f.texto = vazioParaNulo(parametro.apply("q"));
        String tipo = parametro.apply("tipo");
        f.tipo = tipo != null && TipoNotificacao.existe(tipo) ? TipoNotificacao.porChaveUrl(tipo) : null;
        f.subgrupo = vazioParaNulo(parametro.apply("subgrupo"));
        f.gravidade = vazioParaNulo(parametro.apply("gravidade"));
        f.nivelRisco = enumOuNulo(NivelRisco.class, parametro.apply("risco"));
        f.prontuario = vazioParaNulo(parametro.apply("prontuario"));
        f.setor = vazioParaNulo(parametro.apply("setor"));
        f.dataDe = dataOuNula(parametro.apply("de"));
        f.dataAte = dataOuNula(parametro.apply("ate"));
        NotificacaoDAO.Ordenacao ordem = enumOuNulo(NotificacaoDAO.Ordenacao.class, parametro.apply("ordem"));
        f.ordenacao = ordem != null ? ordem : NotificacaoDAO.Ordenacao.RECENTES;
        return f;
    }

    private static void adicionar(StringBuilder sb, String chave, String valor) {
        if (valor == null || valor.trim().isEmpty()) return;
        if (sb.length() > 0) sb.append('&');
        try {
            sb.append(chave).append('=').append(URLEncoder.encode(valor.trim(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 indisponível", e);
        }
    }

    private static <E extends Enum<E>> E enumOuNulo(Class<E> tipo, String valor) {
        if (valor == null) return null;
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LocalDate dataOuNula(String valor) {
        if (valor == null || valor.isEmpty()) return null;
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : valor.trim();
    }
}
