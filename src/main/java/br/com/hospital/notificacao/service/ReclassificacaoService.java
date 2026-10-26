package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.config.DataSourceFactory;
import br.com.hospital.notificacao.dao.HistoricoDAO;
import br.com.hospital.notificacao.dao.NotificacaoDAO;
import br.com.hospital.notificacao.model.ClassificacaoIncidente;
import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.model.TipoNotificacao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reclassificacao unificada de uma notificacao pelo NSP (usada na Triagem, na Investigacao
 * e na tela de Detalhes): tipo/canal, subgrupo (problema), classificacao do incidente,
 * risco e descricao revisada. A descricao original do notificante e o protocolo nunca
 * mudam; toda alteracao fica registrada no historico da notificacao.
 */
public class ReclassificacaoService {

    /** Tamanho da coluna NOTIFICACAO_HISTORICO.OBSERVACAO. */
    static final int LIMITE_OBSERVACAO = 500;

    private final NotificacaoDAO notificacaoDAO = new NotificacaoDAO();
    private final HistoricoDAO historicoDAO = new HistoricoDAO();

    /** Valores desejados apos a reclassificacao. Probabilidade/impacto nulos = risco inalterado. */
    public static class Reclassificacao {
        public TipoNotificacao tipo;
        public String subgrupo;
        public String gravidade;
        public String descricaoRevisada;
        public Integer probabilidade;
        public Integer impacto;
    }

    /**
     * Aplica a reclassificacao numa transacao propria.
     *
     * @return false quando nada mudou (nada e' gravado)
     */
    public boolean reclassificar(long idNotificacao, Reclassificacao r, Long idUsuario) {
        validar(r);
        Notificacao atual = notificacaoDAO.buscarPorId(idNotificacao)
                .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada: " + idNotificacao));
        r.descricaoRevisada = normalizar(r.descricaoRevisada);
        String mudancas = descreverMudancas(atual, r);
        if (mudancas.isEmpty()) {
            return false;
        }

        // Reclassificar uma notificacao recem-aberta equivale a iniciar a triagem dela.
        StatusNotificacao statusAnterior = atual.getStatus();
        StatusNotificacao novoStatus = statusAnterior == StatusNotificacao.ABERTO
                ? StatusNotificacao.TRIAGEM : statusAnterior;

        try (Connection con = DataSourceFactory.obterConexao()) {
            con.setAutoCommit(false);
            try {
                notificacaoDAO.atualizarClassificacao(con, idNotificacao, r.tipo, r.subgrupo, r.gravidade,
                        r.descricaoRevisada, r.probabilidade, r.impacto);
                if (novoStatus != statusAnterior) {
                    notificacaoDAO.atualizarStatus(con, idNotificacao, novoStatus);
                }
                historicoDAO.inserir(con, idNotificacao, statusAnterior, novoStatus, idUsuario,
                        truncar("Reclassificada: " + mudancas));
                con.commit();
            } catch (RuntimeException | SQLException e) {
                con.rollback();
                throw (e instanceof RuntimeException) ? (RuntimeException) e
                        : new RuntimeException("Erro ao reclassificar notificação", e);
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao abrir conexão para reclassificar", e);
        }
        return true;
    }

    static void validar(Reclassificacao r) {
        if (r.tipo == null) {
            throw new IllegalArgumentException("Selecione o tipo de notificação");
        }
        if (!r.tipo.aceitaSubgrupo(r.subgrupo)) {
            throw new IllegalArgumentException("Selecione o problema (" + r.tipo.getRotuloSubgrupo().toLowerCase()
                    + ") correspondente ao tipo " + r.tipo.getTitulo());
        }
        if (!ClassificacaoIncidente.valida(r.gravidade)) {
            throw new IllegalArgumentException("Selecione a classificação do incidente");
        }
        if ((r.probabilidade == null) != (r.impacto == null)) {
            throw new IllegalArgumentException("Informe probabilidade e impacto para reclassificar o risco");
        }
    }

    /** Texto legivel do que mudou ("Tipo A → B; Problema X → Y; ..."), vazio se nada mudou. */
    static String descreverMudancas(Notificacao antes, Reclassificacao depois) {
        List<String> partes = new ArrayList<>();
        if (antes.getTipo() != depois.tipo) {
            partes.add("Tipo " + rotuloTipo(antes.getTipo()) + " → " + rotuloTipo(depois.tipo));
        }
        if (!Objects.equals(antes.getSubgrupo(), depois.subgrupo)) {
            partes.add("Problema " + ouTraco(antes.getSubgrupo()) + " → " + ouTraco(depois.subgrupo));
        }
        if (!Objects.equals(antes.getGravidade(), depois.gravidade)) {
            partes.add("Classificação " + ouTraco(antes.getGravidade()) + " → " + ouTraco(depois.gravidade));
        }
        if (depois.probabilidade != null && depois.impacto != null
                && (!Objects.equals(antes.getProbabilidade(), depois.probabilidade)
                    || !Objects.equals(antes.getImpacto(), depois.impacto))) {
            int score = MatrizRisco.calcularScore(depois.probabilidade, depois.impacto);
            partes.add("Risco " + rotuloRisco(antes.getNivelRisco(), antes.getScoreRisco())
                    + " → " + rotuloRisco(NivelRisco.doScore(score), score));
        }
        String revisadaAntes = normalizar(antes.getDescricaoRevisada());
        String revisadaDepois = normalizar(depois.descricaoRevisada);
        if (!Objects.equals(revisadaAntes, revisadaDepois)) {
            partes.add(revisadaDepois == null ? "Descrição revisada removida (vale a original)"
                    : "Descrição revisada pelo NSP (original mantida)");
        }
        return String.join("; ", partes);
    }

    static String truncar(String texto) {
        return texto.length() <= LIMITE_OBSERVACAO ? texto : texto.substring(0, LIMITE_OBSERVACAO - 1) + "…";
    }

    private static String normalizar(String texto) {
        return texto == null || texto.trim().isEmpty() ? null : texto.trim();
    }

    private static String rotuloTipo(TipoNotificacao tipo) {
        return tipo == null ? "—" : tipo.getTitulo();
    }

    private static String rotuloRisco(NivelRisco nivel, Integer score) {
        if (nivel == null) return "—";
        return nivel.getRotulo() + (score != null ? " (" + score + ")" : "");
    }

    private static String ouTraco(String valor) {
        return valor == null || valor.trim().isEmpty() ? "—" : valor;
    }
}
