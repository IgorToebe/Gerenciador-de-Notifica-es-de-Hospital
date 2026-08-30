package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.dao.NotificacaoDAO;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.service.NotificacaoService;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Notificações — lista geral (Item 3): filtros por status, deteccao de duplicatas por
 * prontuario, busca e paginacao resolvidas em SQL.
 */
public class ListaComposer extends AdminComposer {

    private final NotificacaoService notificacaoService = new NotificacaoService();
    private final NotificacaoDAO.Filtro filtro = new NotificacaoDAO.Filtro();
    private Div areaTabela;

    @Override
    protected String paginaAtiva() {
        return "lista";
    }

    @Override
    protected void renderConteudo(Div conteudo) {
        conteudo.appendChild(construirBarraFiltros());
        areaTabela = new Div();
        conteudo.appendChild(areaTabela);
        atualizarTabela();
    }

    private Div construirBarraFiltros() {
        Div barra = new Div();
        barra.setStyle("display:flex;justify-content:space-between;align-items:center;gap:14px;margin-bottom:14px;flex-wrap:wrap");

        Div chips = new Div();
        chips.setStyle("display:flex;gap:8px;flex-wrap:wrap");
        Map<String, String> opcoes = new LinkedHashMap<>();
        opcoes.put("todas", "Todas");
        opcoes.put("ABERTO", "Aberto");
        opcoes.put("TRIAGEM", "Em triagem");
        opcoes.put("INVESTIGACAO", "Investigação");
        opcoes.put("PLANO", "Plano");
        opcoes.put("CONCLUIDO", "Concluído");
        opcoes.put("dup", "⚠ Duplicatas");
        for (Map.Entry<String, String> entry : opcoes.entrySet()) {
            Label chip = new Label(entry.getValue());
            chip.setSclass("nsp-chip" + (chipAtivo(entry.getKey()) ? " on" : ""));
            chip.setStyle("cursor:pointer");
            chip.addEventListener("onClick", event -> {
                filtro.apenasDuplicatas = "dup".equals(entry.getKey());
                filtro.status = ("todas".equals(entry.getKey()) || "dup".equals(entry.getKey()))
                        ? null : StatusNotificacao.valueOf(entry.getKey());
                filtro.pagina = 1;
                atualizarTabela();
            });
            chips.appendChild(chip);
        }
        barra.appendChild(chips);

        Textbox busca = new Textbox();
        busca.setPlaceholder("Buscar por prontuário, gravidade, texto, protocolo…");
        busca.setWidth("300px");
        busca.setInstant(true);
        busca.addEventListener("onChanging", event -> {
            filtro.texto = ((org.zkoss.zk.ui.event.InputEvent) event).getValue();
            filtro.pagina = 1;
            atualizarTabela();
        });
        barra.appendChild(busca);
        return barra;
    }

    private boolean chipAtivo(String chave) {
        if ("dup".equals(chave)) return filtro.apenasDuplicatas;
        if ("todas".equals(chave)) return !filtro.apenasDuplicatas && filtro.status == null;
        return !filtro.apenasDuplicatas && filtro.status != null && filtro.status.name().equals(chave);
    }

    private void atualizarTabela() {
        areaTabela.getChildren().clear();
        NotificacaoDAO.ResultadoPagina resultado = notificacaoService.listar(filtro);
        Map<String, Integer> duplicatas = notificacaoService.contarDuplicatasPorProntuario();

        Div tabela = new Div();
        tabela.setSclass("nsp-table");

        Div head = new Div();
        head.setSclass("nsp-thead");
        head.appendChild(celula("#", "50px", true));
        head.appendChild(celula("Tipo", "160px", true));
        head.appendChild(celula("Data", "70px", true));
        head.appendChild(celula("Setor", "130px", true));
        head.appendChild(celula("Prontuário", "160px", true));
        head.appendChild(celula("Risco", "90px", true));
        head.appendChild(celula("Status", "130px", true));
        head.appendChild(celula("", "90px", true));
        tabela.appendChild(head);

        if (resultado.itens.isEmpty()) {
            Label vazio = new Label("Nenhuma notificação para este filtro.");
            vazio.setSclass("nsp-empty");
            tabela.appendChild(vazio);
        }

        for (Notificacao n : resultado.itens) {
            boolean dup = n.getProntuario() != null && duplicatas.containsKey(n.getProntuario());
            Div linha = new Div();
            linha.setSclass("nsp-row" + (dup ? " dup" : ""));

            linha.appendChild(celula(String.valueOf(n.getId()), "50px", false));
            linha.appendChild(badgeCelula(n.getTipo().getTitulo(), n.getTipo().getCorSuave(), n.getTipo().getCor(), "160px"));
            linha.appendChild(celula(n.getDataIncidente(), "70px", false));
            linha.appendChild(celula(n.getSetor(), "130px", false));

            Div prontCelula = new Div();
            prontCelula.setSclass("nsp-cell");
            prontCelula.setStyle("flex:0 0 160px");
            Label pront = new Label(n.getProntuario() != null && !n.getProntuario().isEmpty() ? n.getProntuario() : "—");
            prontCelula.appendChild(pront);
            if (dup) {
                Label flag = new Label(duplicatas.get(n.getProntuario()) + " c/ mesmo prontuário");
                flag.setSclass("nsp-dupflag");
                prontCelula.appendChild(flag);
            }
            linha.appendChild(prontCelula);

            if (n.getNivelRisco() != null) {
                linha.appendChild(badgeRisco(n.getNivelRisco(), "90px"));
            } else {
                linha.appendChild(celula("—", "90px", false));
            }
            linha.appendChild(badgeStatus(n.getStatus(), "130px"));

            Div acao = new Div();
            acao.setSclass("nsp-cell");
            acao.setStyle("flex:0 0 90px");
            Label link = new Label(rotuloAcao(n.getStatus()));
            link.setSclass("nsp-linkbtn");
            final long id = n.getId();
            link.addEventListener("onClick", event ->
                    Executions.getCurrent().sendRedirect(destinoAcao(n.getStatus()) + "?id=" + id));
            acao.appendChild(link);
            linha.appendChild(acao);

            tabela.appendChild(linha);
        }
        areaTabela.appendChild(tabela);
        areaTabela.appendChild(construirPaginacao(resultado.total));
    }

    private Div construirPaginacao(int total) {
        Div pag = new Div();
        pag.setStyle("display:flex;justify-content:space-between;align-items:center;margin-top:12px;color:var(--muted);font-size:12.5px");
        int totalPaginas = Math.max(1, (int) Math.ceil(total / (double) filtro.tamanhoPagina));
        Label info = new Label("Página " + filtro.pagina + " de " + totalPaginas + " · " + total + " notificações");
        pag.appendChild(info);

        Div botoes = new Div();
        botoes.setStyle("display:flex;gap:7px");
        if (filtro.pagina > 1) {
            Label anterior = new Label("‹ Anterior");
            anterior.setSclass("nsp-linkbtn");
            anterior.addEventListener("onClick", e -> { filtro.pagina--; atualizarTabela(); });
            botoes.appendChild(anterior);
        }
        if (filtro.pagina < totalPaginas) {
            Label proxima = new Label("Próxima ›");
            proxima.setSclass("nsp-linkbtn");
            proxima.addEventListener("onClick", e -> { filtro.pagina++; atualizarTabela(); });
            botoes.appendChild(proxima);
        }
        pag.appendChild(botoes);
        return pag;
    }

    private String rotuloAcao(StatusNotificacao status) {
        switch (status) {
            case ABERTO: return "Triar →";
            case TRIAGEM: return "Classificar →";
            case INVESTIGACAO: return "Investigar →";
            case PLANO: return "Plano →";
            default: return "Ver →";
        }
    }

    private String destinoAcao(StatusNotificacao status) {
        switch (status) {
            case ABERTO:
            case TRIAGEM:
                return "/admin/triagem.zul";
            case INVESTIGACAO:
                return "/admin/investigacao.zul";
            case PLANO:
                return "/admin/plano.zul";
            default:
                return "/admin/investigacao.zul";
        }
    }

    private Div celula(String texto, String largura, boolean cabecalho) {
        Div div = new Div();
        div.setSclass("nsp-cell");
        div.setStyle("flex:0 0 " + largura + (cabecalho ? ";" : ";"));
        Label lbl = new Label(texto);
        div.appendChild(lbl);
        return div;
    }

    private Div badgeCelula(String texto, String bg, String fg, String largura) {
        Div div = new Div();
        div.setSclass("nsp-cell");
        div.setStyle("flex:0 0 " + largura);
        Label lbl = new Label(texto);
        lbl.setSclass("nsp-tg");
        lbl.setStyle("background:" + bg + ";color:" + fg);
        div.appendChild(lbl);
        return div;
    }

    private Div badgeRisco(NivelRisco nivel, String largura) {
        Div div = new Div();
        div.setSclass("nsp-cell");
        div.setStyle("flex:0 0 " + largura);
        Label lbl = new Label(nivel.getRotulo());
        lbl.setSclass("nsp-tg");
        lbl.setStyle("background:var(--r-" + nivel.getChave() + "-bg);color:var(--r-" + nivel.getChave() + "-fg)");
        div.appendChild(lbl);
        return div;
    }

    private Div badgeStatus(StatusNotificacao status, String largura) {
        Div div = new Div();
        div.setSclass("nsp-cell");
        div.setStyle("flex:0 0 " + largura);
        Label lbl = new Label(status.getRotulo());
        lbl.setSclass("nsp-tg");
        lbl.setStyle("background:var(--s-" + status.getClasseCss() + "-bg);color:var(--s-" + status.getClasseCss() + "-fg)");
        div.appendChild(lbl);
        return div;
    }
}
