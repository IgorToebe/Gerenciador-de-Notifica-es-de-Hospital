package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.model.AcaoPlano;
import br.com.hospital.notificacao.model.Anexo;
import br.com.hospital.notificacao.model.CampoNotificacao;
import br.com.hospital.notificacao.model.HistoricoStatus;
import br.com.hospital.notificacao.model.Investigacao;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.service.DetalheNotificacao;
import br.com.hospital.notificacao.service.NotificacaoService;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.A;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

import java.text.SimpleDateFormat;

/**
 * Detalhes da notificação: tudo o que foi preenchido pelo notificante e registrado pelo
 * NSP (classificação, descrição original e revisada, anexos, investigação, plano de ação e
 * histórico), com atalhos para reclassificar, exportar em PDF e ir para a etapa atual.
 * Os dados de contato do notificante nunca são exibidos — só se a notificação é anônima.
 */
public class DetalheComposer extends AdminComposer {

    private final NotificacaoService notificacaoService = new NotificacaoService();
    private final SimpleDateFormat formatoDataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Override
    protected String paginaAtiva() {
        return "lista";
    }

    @Override
    protected void renderConteudo(Div conteudo) {
        DetalheNotificacao detalhe = carregar();
        if (detalhe == null) {
            Label vazio = new Label("Notificação não encontrada.");
            vazio.setSclass("nsp-empty");
            conteudo.appendChild(vazio);
            return;
        }
        Notificacao n = detalhe.getNotificacao();
        conteudo.appendChild(construirCabecalho(n));

        Div grade = new Div();
        grade.setSclass("nsp-det-grid");

        Div classificacao = secao(grade, "Classificação");
        kv(classificacao, "Problema", n.getSubgrupo());
        kv(classificacao, "Classificação", n.getGravidade());
        Div risco = linhaKV(classificacao, "Risco");
        if (n.getNivelRisco() != null) {
            risco.appendChild(tagRisco(n.getNivelRisco()));
            Label score = new Label("  pontuação " + n.getScoreRisco() + " (P" + n.getProbabilidade() + " × I" + n.getImpacto() + ")");
            score.setStyle("font-size:12px;color:var(--muted)");
            risco.appendChild(score);
        } else {
            risco.appendChild(new Label("—"));
        }
        kv(classificacao, "Gerou dano ao paciente", n.isGerouDano() ? "Sim" : "Não");

        Div incidente = secao(grade, "Dados do incidente");
        kv(incidente, "Data / hora", ouTraco(n.getDataIncidente()) + (vazio(n.getHoraIncidente()) ? "" : " · " + n.getHoraIncidente()));
        kv(incidente, "Setor / unidade", n.getSetor());
        kv(incidente, "Prontuário", n.getProntuario());
        for (CampoNotificacao c : n.getCampos()) {
            kv(incidente, c.getRotulo(), c.getValor());
        }
        conteudo.appendChild(grade);

        Div descricao = secao(conteudo, "Descrição do problema");
        Div descricoes = new Div();
        descricoes.setStyle("display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:14px");
        descricoes.appendChild(blocoTexto("Original (notificante)", n.getDescricao()));
        if (!vazio(n.getDescricaoRevisada())) {
            descricoes.appendChild(blocoTexto("Revisada pelo NSP", n.getDescricaoRevisada()));
        }
        descricao.appendChild(descricoes);

        Div anexos = secao(conteudo, "Anexos (" + n.getAnexos().size() + ")");
        if (n.getAnexos().isEmpty()) {
            anexos.appendChild(textoMudo("Nenhum anexo."));
        }
        for (Anexo a : n.getAnexos()) {
            A link = new A(a.getNomeOriginal() + " · " + Math.max(1, a.getTamanhoBytes() / 1024) + " KB");
            link.setHref("/anexo/" + a.getId());
            link.setTarget("_blank");
            link.setStyle("display:block;font-size:13px;margin-bottom:5px");
            anexos.appendChild(link);
        }

        Investigacao inv = detalhe.getInvestigacao();
        if (inv != null) {
            Div investigacao = secao(conteudo, "Investigação (FOR-NSP-014)");
            kv(investigacao, "Parecerista", inv.getParecerista());
            kv(investigacao, "Data", inv.getDataInvestigacao());
            kv(investigacao, "Causa raiz", inv.getCausaRaiz());
            kv(investigacao, "Parecer do NSP", inv.getParecer());
            kv(investigacao, "Situação", inv.isConcluida() ? "Concluída" : "Em andamento");
        }

        if (!detalhe.getAcoes().isEmpty()) {
            Div plano = secao(conteudo, "Plano de ação 5W2H (" + detalhe.getAcoes().size() + " ações)");
            for (AcaoPlano a : detalhe.getAcoes()) {
                kv(plano, a.getCodigo() + " · " + a.getStatus().getRotulo(),
                        ouTraco(a.getoQue()) + " — " + ouTraco(a.getQuem()));
            }
        }

        Div historico = secao(conteudo, "Histórico");
        for (HistoricoStatus h : detalhe.getHistorico()) {
            String quando = h.getCriadoEm() != null ? formatoDataHora.format(h.getCriadoEm()) : "";
            String status = (h.getStatusAnterior() != null && h.getStatusAnterior() != h.getStatusNovo()
                    ? h.getStatusAnterior().getRotulo() + " → " : "") + h.getStatusNovo().getRotulo();
            kv(historico, quando, status + (vazio(h.getObservacao()) ? "" : " · " + h.getObservacao()));
        }
    }

    private DetalheNotificacao carregar() {
        String idParam = Executions.getCurrent().getParameter("id");
        if (idParam == null) return null;
        try {
            return notificacaoService.carregarDetalhe(Long.parseLong(idParam));
        } catch (IllegalArgumentException e) { // id invalido ou inexistente
            return null;
        }
    }

    private Div construirCabecalho(Notificacao n) {
        Div painel = new Div();
        painel.setSclass("nsp-panel");
        painel.setStyle("border-left:5px solid " + n.getTipo().getCor());

        Div topo = new Div();
        topo.setStyle("display:flex;justify-content:space-between;align-items:flex-start;gap:14px;flex-wrap:wrap");
        Div info = new Div();
        Label titulo = new Label(n.getProtocolo() + " · Notif. #" + n.getId());
        titulo.setStyle("display:block;font-size:18px;font-weight:800;color:#23201c;margin-bottom:6px");
        info.appendChild(titulo);
        Div tags = new Div();
        tags.setStyle("display:flex;gap:7px;flex-wrap:wrap;align-items:center");
        tags.appendChild(tagTipo(n.getTipo()));
        tags.appendChild(tagStatus(n.getStatus()));
        Label meta = new Label("Recebida em " + (n.getCriadoEm() != null ? formatoDataHora.format(n.getCriadoEm()) : "—")
                + " · " + n.getTipo().getCodigoFormulario()
                + " · " + (n.isAnonimo() ? "Anônima" : "Contato vinculado (sigiloso)"));
        meta.setStyle("font-size:12px;color:var(--muted)");
        tags.appendChild(meta);
        info.appendChild(tags);
        topo.appendChild(info);

        Div botoes = new Div();
        botoes.setStyle("display:flex;gap:9px;flex-wrap:wrap");
        Label reclassificar = new Label("Reclassificar");
        reclassificar.setSclass("nsp-btn");
        botoes.appendChild(reclassificar);
        Label exportar = new Label("Exportar PDF");
        exportar.setSclass("nsp-btn");
        exportar.addEventListener("onClick", e ->
                Executions.getCurrent().sendRedirect("/export/notificacao/" + n.getId() + ".pdf", "_blank"));
        botoes.appendChild(exportar);
        Label etapa = new Label("Ir para etapa atual: " + rotuloEtapa(n.getStatus()));
        etapa.setSclass("nsp-btn nsp-btn-dark");
        etapa.addEventListener("onClick", e ->
                Executions.getCurrent().sendRedirect(destinoEtapa(n.getStatus()) + "?id=" + n.getId()));
        botoes.appendChild(etapa);
        topo.appendChild(botoes);
        painel.appendChild(topo);

        Div areaReclass = new Div();
        areaReclass.setVisible(false);
        areaReclass.setStyle("margin-top:6px");
        Div sec = new Div();
        sec.setSclass("nsp-sec-title");
        sec.appendChild(new Label("Reclassificação"));
        areaReclass.appendChild(sec);
        areaReclass.appendChild(new ReclassificacaoPainel(n, true,
                () -> Executions.getCurrent().sendRedirect("/admin/detalhe.zul?id=" + n.getId())));
        painel.appendChild(areaReclass);
        reclassificar.addEventListener("onClick", e -> areaReclass.setVisible(!areaReclass.isVisible()));
        return painel;
    }

    private Div secao(Div pai, String titulo) {
        Div painel = new Div();
        painel.setSclass("nsp-panel");
        Label lbl = new Label(titulo);
        lbl.setStyle("display:block;font-size:15px;font-weight:800;color:#23201c;margin-bottom:12px");
        painel.appendChild(lbl);
        pai.appendChild(painel);
        return painel;
    }

    private void kv(Div pai, String chave, String valor) {
        Label v = new Label(ouTraco(valor));
        v.setMultiline(true);
        linhaKV(pai, chave).appendChild(v);
    }

    /** Linha "chave: valor"; devolve o container do valor para quem precisa por um componente nele. */
    private Div linhaKV(Div pai, String chave) {
        Div linha = new Div();
        linha.setSclass("nsp-kv");
        Label k = new Label(chave);
        k.setSclass("k");
        Div v = new Div();
        v.setSclass("v");
        linha.appendChild(k);
        linha.appendChild(v);
        pai.appendChild(linha);
        return v;
    }

    private Div blocoTexto(String titulo, String texto) {
        Div bloco = new Div();
        Label lbl = new Label(titulo);
        lbl.setSclass("nsp-field-label");
        Label corpo = new Label(ouTraco(texto));
        corpo.setMultiline(true);
        corpo.setSclass("nsp-desc-original");
        bloco.appendChild(lbl);
        bloco.appendChild(corpo);
        return bloco;
    }

    private Label textoMudo(String texto) {
        Label l = new Label(texto);
        l.setStyle("font-size:12.5px;color:var(--muted)");
        return l;
    }

    private boolean vazio(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String ouTraco(String s) {
        return vazio(s) ? "—" : s;
    }
}
