package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.dao.NotificacaoDAO;
import br.com.hospital.notificacao.model.AcaoPlano;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.StatusAcao;
import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.service.NotificacaoService;
import br.com.hospital.notificacao.service.PlanoAcaoService;
import br.com.hospital.notificacao.service.ValidacaoNotificacao;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

import java.util.List;

/**
 * Plano de ação 5W2H (SGQ-001): tabela editável, contadores por status e conclusão da
 * notificação quando todas as ações estiverem finalizadas.
 */
public class PlanoComposer extends AdminComposer {

    private final NotificacaoService notificacaoService = new NotificacaoService();
    private final PlanoAcaoService planoAcaoService = new PlanoAcaoService();

    private Notificacao notificacao;

    @Override
    protected String paginaAtiva() {
        return "plano";
    }

    @Override
    protected void renderConteudo(Div conteudo) {
        adicionarVoltar(conteudo, "/admin/notificacoes.zul", "← Voltar para notificações");
        notificacao = carregarNotificacao();
        if (notificacao == null) {
            Label vazio = new Label("Nenhum plano de ação gerado ainda. Conclua uma investigação para gerar.");
            vazio.setSclass("nsp-empty");
            conteudo.appendChild(vazio);
            return;
        }
        montar(conteudo);
    }

    private void montar(Div conteudo) {
        conteudoAtual = conteudo;
        conteudo.getChildren().clear();
        List<AcaoPlano> acoes = planoAcaoService.listar(notificacao.getId());

        Div nota = new Div();
        nota.setSclass("nsp-note");
        nota.appendChild(new Label("Plano gerado a partir da investigação #" + notificacao.getId() + " ("
                + notificacao.getTipo().getTitulo() + "). A causa raiz vira o \"Por quê?\" das ações. Concluir " +
                "todas as ações fecha a notificação e alimenta os indicadores."));
        conteudo.appendChild(nota);

        conteudo.appendChild(construirContadores(acoes));
        conteudo.appendChild(construirTabela(acoes));

        Div rodape = new Div();
        rodape.setStyle("display:flex;justify-content:flex-end;gap:11px;margin-top:18px;flex-wrap:wrap");

        Label addAcao = botao("+ Adicionar ação", "#fff", "#3a352f", "1px solid var(--ring)");
        addAcao.addEventListener("onClick", e -> {
            String causa = acoes.isEmpty() ? "" : acoes.get(0).getPorque();
            planoAcaoService.adicionarAcao(notificacao.getId(), causa, notificacao.getSetor());
            montar(conteudo);
        });
        rodape.appendChild(addAcao);

        Label exportar = botao("Exportar (.xlsx)", "#fff", "#3a352f", "1px solid var(--ring)");
        exportar.addEventListener("onClick", e ->
                Clients.showNotification("Exportação de indicadores fica para a V2 (ver ROADMAP-V2.md)", "info", null, "top_center", 3000));
        rodape.appendChild(exportar);

        boolean podeConcluir = planoAcaoService.podeConcluir(notificacao.getId());
        Label concluir = botao("Concluir notificação",
                podeConcluir ? "#2b2723" : "#eee", podeConcluir ? "#fff" : "#999", "none");
        concluir.addEventListener("onClick", e -> {
            if (!podeConcluir) {
                Clients.showNotification("Conclua ou cancele todas as ações para fechar a notificação", "error", null, "top_center", 3000);
                return;
            }
            planoAcaoService.concluirNotificacao(notificacao.getId(),
                    SessaoUtil.usuarioAtual() != null ? SessaoUtil.usuarioAtual().getId() : null);
            Clients.showNotification("Notificação concluída · indicadores alimentados", "info", null, "top_center", 3000);
            Executions.getCurrent().sendRedirect("/admin/plano.zul?id=" + notificacao.getId());
        });
        rodape.appendChild(concluir);

        conteudo.appendChild(rodape);
    }

    private Notificacao carregarNotificacao() {
        String idParam = Executions.getCurrent().getParameter("id");
        if (idParam != null) {
            try {
                return notificacaoService.carregarCompleta(Long.parseLong(idParam));
            } catch (Exception ignored) { }
        }
        NotificacaoDAO.Filtro f = new NotificacaoDAO.Filtro();
        f.status = StatusNotificacao.PLANO;
        f.tamanhoPagina = 1;
        NotificacaoDAO.ResultadoPagina r = notificacaoService.listar(f);
        return r.itens.isEmpty() ? null : notificacaoService.carregarCompleta(r.itens.get(0).getId());
    }

    private Div construirContadores(List<AcaoPlano> acoes) {
        Div barra = new Div();
        barra.setStyle("display:flex;gap:9px;margin-bottom:18px;flex-wrap:wrap");
        for (StatusAcao s : StatusAcao.values()) {
            long qtd = acoes.stream().filter(a -> a.getStatus() == s).count();
            barra.appendChild(stbox(String.valueOf(qtd), s.getRotulo()));
        }
        barra.appendChild(stbox(String.valueOf(acoes.size()), "Total"));
        return barra;
    }

    private Div stbox(String numero, String rotulo) {
        Div box = new Div();
        box.setSclass("nsp-stbox");
        Label n = new Label(numero);
        n.setSclass("n");
        Label l = new Label(rotulo);
        l.setSclass("l");
        box.appendChild(n);
        box.appendChild(l);
        return box;
    }

    private Div construirTabela(List<AcaoPlano> acoes) {
        Div scroll = new Div();
        scroll.setStyle("overflow-x:auto");
        Div tabela = new Div();
        tabela.setSclass("nsp-table");
        tabela.setStyle("min-width:980px");

        Div head = new Div();
        head.setSclass("nsp-thead");
        String[][] colunas = {{"Cód", "60"}, {"O quê?", "170"}, {"Quem?", "120"}, {"Onde?", "110"},
                {"Por quê? (causa raiz)", "170"}, {"Como?", "150"}, {"Quando", "120"}, {"Status", "150"}, {"Evidência", "120"}};
        for (String[] col : colunas) {
            Div c = new Div();
            c.setSclass("nsp-cell");
            c.setStyle("flex:0 0 " + col[1] + "px");
            c.appendChild(new Label(col[0]));
            head.appendChild(c);
        }
        tabela.appendChild(head);

        for (AcaoPlano a : acoes) {
            Div linha = new Div();
            linha.setSclass("nsp-row");
            linha.appendChild(textoCelula(a.getCodigo(), "60"));
            linha.appendChild(campoAcao(a, "oque", a.getoQue(), "170"));
            linha.appendChild(campoAcao(a, "quem", a.getQuem(), "120"));
            linha.appendChild(campoAcao(a, "onde", a.getOnde(), "110"));
            linha.appendChild(campoAcao(a, "porque", a.getPorque(), "170"));
            linha.appendChild(campoAcao(a, "como", a.getComo(), "150"));

            Div quando = new Div();
            quando.setSclass("nsp-cell");
            quando.setStyle("flex:0 0 120px;display:flex;gap:4px");
            Textbox inicio = textbox(a.getDataInicio(), "dd/mm/aaaa");
            Textbox fim = textbox(a.getDataFim(), "dd/mm/aaaa");
            inicio.addEventListener("onChange", e -> atualizarDatas(a, inicio, fim));
            fim.addEventListener("onChange", e -> atualizarDatas(a, inicio, fim));
            quando.appendChild(inicio);
            quando.appendChild(fim);
            linha.appendChild(quando);

            Div statusCel = new Div();
            statusCel.setSclass("nsp-cell");
            statusCel.setStyle("flex:0 0 150px");
            Combobox cbo = new Combobox();
            cbo.setReadonly(true);
            cbo.setWidth("100%");
            for (StatusAcao s : StatusAcao.values()) {
                Comboitem item = new Comboitem(s.getRotulo());
                item.setValue(s);
                cbo.appendChild(item);
                if (s == a.getStatus()) cbo.setSelectedItem(item);
            }
            final long idAcao = a.getId();
            cbo.addEventListener("onSelect", e -> {
                StatusAcao novo = (StatusAcao) cbo.getSelectedItem().getValue();
                planoAcaoService.atualizarStatus(idAcao, novo);
                montar(conteudoAtual);
            });
            statusCel.appendChild(cbo);
            linha.appendChild(statusCel);

            Div evCel = new Div();
            evCel.setSclass("nsp-cell");
            evCel.setStyle("flex:0 0 120px");
            Textbox ev = new Textbox();
            ev.setValue(a.getEvidencia());
            ev.setWidth("100%");
            ev.addEventListener("onChange", e -> planoAcaoService.atualizarEvidencia(idAcao, ev.getValue()));
            evCel.appendChild(ev);
            linha.appendChild(evCel);

            tabela.appendChild(linha);
        }
        scroll.appendChild(tabela);
        return scroll;
    }

    private Div conteudoAtual;

    private Div textoCelula(String texto, String larguraPx) {
        Div c = new Div();
        c.setSclass("nsp-cell");
        c.setStyle("flex:0 0 " + larguraPx + "px");
        c.appendChild(new Label(texto == null ? "" : texto));
        return c;
    }

    private Div campoAcao(AcaoPlano acao, String campo, String valor, String larguraPx) {
        Div celula = new Div();
        celula.setSclass("nsp-cell");
        celula.setStyle("flex:0 0 " + larguraPx + "px");
        Textbox input = textbox(valor, null);
        input.addEventListener("onChange", e -> {
            String novo = input.getValue() == null ? "" : input.getValue().trim();
            if ("oque".equals(campo)) acao.setoQue(novo);
            if ("quem".equals(campo)) acao.setQuem(novo);
            if ("onde".equals(campo)) acao.setOnde(novo);
            if ("porque".equals(campo)) acao.setPorque(novo);
            if ("como".equals(campo)) acao.setComo(novo);
            planoAcaoService.atualizarCampos(acao);
        });
        celula.appendChild(input);
        return celula;
    }

    private Textbox textbox(String valor, String placeholder) {
        Textbox input = new Textbox();
        input.setValue(valor == null ? "" : valor);
        input.setWidth("100%");
        if (placeholder != null) input.setPlaceholder(placeholder);
        return input;
    }

    private void atualizarDatas(AcaoPlano acao, Textbox inicio, Textbox fim) {
        String dataInicio = inicio.getValue() == null ? "" : inicio.getValue().trim();
        String dataFim = fim.getValue() == null ? "" : fim.getValue().trim();
        if ("—".equals(dataInicio)) dataInicio = "";
        if ("—".equals(dataFim)) dataFim = "";
        try {
            if (!dataInicio.isEmpty()) ValidacaoNotificacao.validarData(dataInicio, "Data inicial");
            if (!dataFim.isEmpty()) ValidacaoNotificacao.validarData(dataFim, "Data final");
            ValidacaoNotificacao.validarIntervaloDatas(dataInicio, dataFim);
        } catch (IllegalArgumentException e) {
            Clients.showNotification(e.getMessage(), "error", null, "top_center", 3000);
            return;
        }
        acao.setDataInicio(dataInicio);
        acao.setDataFim(dataFim);
        planoAcaoService.atualizarCampos(acao);
    }

    private Label botao(String texto, String bg, String fg, String borda) {
        Label b = new Label(texto);
        b.setStyle("background:" + bg + ";color:" + fg + ";border:" + borda + ";border-radius:10px;padding:11px 18px;" +
                "font-size:13px;font-weight:600;cursor:pointer");
        return b;
    }
}
