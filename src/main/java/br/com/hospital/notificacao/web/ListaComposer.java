package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.dao.NotificacaoDAO;
import br.com.hospital.notificacao.model.ClassificacaoIncidente;
import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.Setores;
import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.model.TipoNotificacao;
import br.com.hospital.notificacao.service.NotificacaoService;
import br.com.hospital.notificacao.service.ValidacaoNotificacao;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Notificações — lista geral (Item 3): filtros por status, deteccao de duplicatas
 * (mesmo prontuario e mesma gravidade), periodo do incidente, filtros avancados (tipo,
 * problema, classificacao, risco, setor, prontuario do paciente, data de recebimento),
 * ordenacao, busca e paginacao resolvidas em SQL, e exportacao em PDF do resultado filtrado.
 */
public class ListaComposer extends AdminComposer {

    private static final String TODOS = "Todos";

    private final NotificacaoService notificacaoService = new NotificacaoService();
    private NotificacaoDAO.Filtro filtro = new NotificacaoDAO.Filtro();
    private Div conteudo;
    private Div areaTabela;
    private boolean filtrosAbertos = false;

    @Override
    protected String paginaAtiva() {
        return "lista";
    }

    @Override
    protected void renderConteudo(Div conteudo) {
        this.conteudo = conteudo;
        montar();
    }

    private void montar() {
        conteudo.getChildren().clear();
        conteudo.appendChild(construirBarraFiltros());
        conteudo.appendChild(construirFiltrosAvancados());
        areaTabela = new Div();
        conteudo.appendChild(areaTabela);
        atualizarTabela();
    }

    private Div construirBarraFiltros() {
        Div barra = new Div();
        barra.setStyle("display:flex;align-items:center;gap:8px;margin-bottom:14px;flex-wrap:wrap");

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
                montar();
            });
            chips.appendChild(chip);
        }
        barra.appendChild(chips);

        Textbox busca = new Textbox();
        busca.setPlaceholder("Buscar por prontuário, problema, texto, protocolo…");
        busca.setWidth("300px");
        busca.setInstant(true);
        busca.setValue(filtro.texto);
        busca.addEventListener("onChanging", event -> {
            filtro.texto = ((org.zkoss.zk.ui.event.InputEvent) event).getValue();
            filtro.pagina = 1;
            atualizarTabela();
        });
        barra.appendChild(busca);

        // Periodo do incidente (data informada pelo notificante).
        Textbox dataInicial = new Textbox();
        dataInicial.setPlaceholder("De dd/mm/aaaa");
        dataInicial.setMaxlength(10);
        dataInicial.setWidth("120px");
        dataInicial.setValue(filtro.dataInicial);
        dataInicial.addEventListener("onChange", event -> aplicarData(dataInicial, true));
        barra.appendChild(dataInicial);
        instalarMascaraData(dataInicial);

        Textbox dataFinal = new Textbox();
        dataFinal.setPlaceholder("Até dd/mm/aaaa");
        dataFinal.setMaxlength(10);
        dataFinal.setWidth("120px");
        dataFinal.setValue(filtro.dataFinal);
        dataFinal.addEventListener("onChange", event -> aplicarData(dataFinal, false));
        barra.appendChild(dataFinal);
        instalarMascaraData(dataFinal);

        Label limpar = new Label("Limpar filtros");
        limpar.setSclass("nsp-linkbtn");
        limpar.addEventListener("onClick", event -> {
            filtro = new NotificacaoDAO.Filtro();
            montar();
        });
        barra.appendChild(limpar);

        Label avancados = new Label("Filtros avançados " + (filtrosAbertos ? "▴" : "▾"));
        avancados.setSclass("nsp-btn");
        avancados.addEventListener("onClick", e -> {
            filtrosAbertos = !filtrosAbertos;
            montar();
        });
        barra.appendChild(avancados);

        Label exportar = new Label("Exportar PDF");
        exportar.setSclass("nsp-btn nsp-btn-dark");
        exportar.addEventListener("onClick", e -> {
            String qs = FiltroParametros.paraQueryString(filtro);
            Executions.getCurrent().sendRedirect("/export/notificacoes.pdf" + (qs.isEmpty() ? "" : "?" + qs), "_blank");
        });
        barra.appendChild(exportar);
        return barra;
    }

    private void instalarMascaraData(Textbox campo) {
        String script = "zk.afterMount(function(){var w=zk.Widget.$('" + campo.getUuid() + "');"
            + "var input=w&&w.getInputNode?w.getInputNode():null;if(input)jq(input).on('input',function(){"
            + "var v=this.value.replace(/\\D/g,'').slice(0,8);this.value=v.length>2?v.slice(0,2)+'/'+"
            + "(v.length>4?v.slice(2,4)+'/'+v.slice(4):v.slice(2)):v;});});";
        Clients.evalJavaScript(script);
    }

    private void aplicarData(Textbox campo, boolean inicial) {
        String valor = campo.getValue() == null ? "" : campo.getValue().trim();
        try {
            if (!valor.isEmpty()) ValidacaoNotificacao.validarData(valor, inicial ? "Data inicial" : "Data final");
        } catch (IllegalArgumentException e) {
            Clients.wrongValue(campo, e.getMessage());
            return;
        }
        if (inicial) filtro.dataInicial = valor.isEmpty() ? null : valor;
        else filtro.dataFinal = valor.isEmpty() ? null : valor;
        filtro.pagina = 1;
        atualizarTabela();
    }

    private Div construirFiltrosAvancados() {
        Div painel = new Div();
        painel.setSclass("nsp-filtros");
        painel.setVisible(filtrosAbertos);

        Combobox cboSubgrupo = new Combobox();
        Combobox cboTipo = comboFiltro(painel, "Tipo", TODOS, valor -> {
            filtro.tipo = valor == null ? null : tipoPorTitulo(valor);
            filtro.subgrupo = null;
            filtro.pagina = 1;
            montar();
        });
        for (TipoNotificacao t : TipoNotificacao.values()) {
            adicionarItem(cboTipo, t.getTitulo(), filtro.tipo == t);
        }

        configurarCombo(cboSubgrupo, painel, "Problema (subgrupo)", TODOS, valor -> {
            filtro.subgrupo = valor;
            filtro.pagina = 1;
            atualizarTabela();
        });
        for (String s : subgruposDisponiveis()) {
            adicionarItem(cboSubgrupo, s, s.equals(filtro.subgrupo));
        }

        Combobox cboGravidade = comboFiltro(painel, "Classificação", "Todas", valor -> {
            filtro.gravidade = valor;
            filtro.pagina = 1;
            atualizarTabela();
        });
        for (String g : ClassificacaoIncidente.TODAS) {
            adicionarItem(cboGravidade, g, g.equals(filtro.gravidade));
        }

        Combobox cboRisco = comboFiltro(painel, "Risco", TODOS, valor -> {
            filtro.nivelRisco = null;
            for (NivelRisco n : NivelRisco.values()) {
                if (n.getRotulo().equals(valor)) filtro.nivelRisco = n;
            }
            filtro.pagina = 1;
            atualizarTabela();
        });
        for (NivelRisco n : NivelRisco.values()) {
            adicionarItem(cboRisco, n.getRotulo(), filtro.nivelRisco == n);
        }

        Combobox cboSetor = comboFiltro(painel, "Setor", TODOS, valor -> {
            filtro.setor = valor;
            filtro.pagina = 1;
            atualizarTabela();
        });
        for (String s : Setores.TODOS) {
            adicionarItem(cboSetor, s, s.equals(filtro.setor));
        }

        Textbox txtProntuario = new Textbox();
        txtProntuario.setPlaceholder("Nº do prontuário");
        txtProntuario.setWidth("100%");
        txtProntuario.setInstant(true);
        txtProntuario.setValue(filtro.prontuario);
        txtProntuario.addEventListener("onChanging", event -> {
            filtro.prontuario = ((org.zkoss.zk.ui.event.InputEvent) event).getValue();
            filtro.pagina = 1;
            atualizarTabela();
        });
        painel.appendChild(campo("Paciente (prontuário)", txtProntuario));

        Datebox de = datebox(filtro.dataDe);
        de.addEventListener("onChange", e -> {
            filtro.dataDe = de.getValueInLocalDate();
            filtro.pagina = 1;
            atualizarTabela();
        });
        painel.appendChild(campo("Recebida de", de));
        Datebox ate = datebox(filtro.dataAte);
        ate.addEventListener("onChange", e -> {
            filtro.dataAte = ate.getValueInLocalDate();
            filtro.pagina = 1;
            atualizarTabela();
        });
        painel.appendChild(campo("Recebida até", ate));

        Combobox cboOrdem = new Combobox();
        configurarCombo(cboOrdem, painel, "Ordenar por", null, valor -> {
            for (NotificacaoDAO.Ordenacao o : NotificacaoDAO.Ordenacao.values()) {
                if (o.getRotulo().equals(valor)) filtro.ordenacao = o;
            }
            filtro.pagina = 1;
            atualizarTabela();
        });
        for (NotificacaoDAO.Ordenacao o : NotificacaoDAO.Ordenacao.values()) {
            adicionarItem(cboOrdem, o.getRotulo(), filtro.ordenacao == o);
        }
        return painel;
    }

    /** Subgrupos do tipo filtrado ou, sem tipo, a uniao (sem repetir) de todos os tipos. */
    private Set<String> subgruposDisponiveis() {
        Set<String> resultado = new LinkedHashSet<>();
        for (TipoNotificacao t : TipoNotificacao.values()) {
            if (filtro.tipo == null || filtro.tipo == t) resultado.addAll(t.getSubgrupos());
        }
        return resultado;
    }

    private TipoNotificacao tipoPorTitulo(String titulo) {
        for (TipoNotificacao t : TipoNotificacao.values()) {
            if (t.getTitulo().equals(titulo)) return t;
        }
        return null;
    }

    /**
     * Combobox de filtro com uma primeira opcao "sem filtro" (rotuloTodos). O callback
     * recebe o rotulo escolhido, ou null quando a opcao "sem filtro" e' selecionada.
     */
    private Combobox comboFiltro(Div pai, String rotulo, String rotuloTodos, Consumer<String> aoSelecionar) {
        Combobox cb = new Combobox();
        configurarCombo(cb, pai, rotulo, rotuloTodos, aoSelecionar);
        return cb;
    }

    private void configurarCombo(Combobox cb, Div pai, String rotulo, String rotuloTodos, Consumer<String> aoSelecionar) {
        cb.setReadonly(true);
        cb.setWidth("100%");
        if (rotuloTodos != null) {
            Comboitem todos = new Comboitem(rotuloTodos);
            cb.appendChild(todos);
            cb.setSelectedItem(todos);
        }
        cb.addEventListener("onSelect", e -> {
            Comboitem sel = cb.getSelectedItem();
            boolean semFiltro = sel == null || (rotuloTodos != null && sel.getIndex() == 0);
            aoSelecionar.accept(semFiltro ? null : sel.getLabel());
        });
        pai.appendChild(campo(rotulo, cb));
    }

    private void adicionarItem(Combobox cb, String rotulo, boolean selecionado) {
        Comboitem item = new Comboitem(rotulo);
        cb.appendChild(item);
        if (selecionado) cb.setSelectedItem(item);
    }

    private Datebox datebox(java.time.LocalDate valor) {
        Datebox db = new Datebox();
        db.setFormat("dd/MM/yyyy");
        db.setWidth("100%");
        db.setValueInLocalDate(valor);
        return db;
    }

    private Div campo(String rotulo, Component componente) {
        Div wrapper = new Div();
        Label lbl = new Label(rotulo);
        lbl.setSclass("nsp-field-label");
        wrapper.appendChild(lbl);
        wrapper.appendChild(componente);
        return wrapper;
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
        SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy");

        Div scroll = new Div();
        scroll.setStyle("overflow-x:auto");
        Div tabela = new Div();
        tabela.setSclass("nsp-table");
        tabela.setStyle("min-width:1060px");

        Div head = new Div();
        head.setSclass("nsp-thead");
        head.appendChild(celula("# / Incidente", "90px"));
        head.appendChild(celula("Tipo", "150px"));
        head.appendChild(celula("Problema", "160px"));
        head.appendChild(celula("Classificação", "150px"));
        head.appendChild(celula("Risco", "90px"));
        head.appendChild(celula("Prontuário", "160px"));
        head.appendChild(celula("Status", "130px"));
        head.appendChild(celula("", "150px"));
        tabela.appendChild(head);

        if (resultado.itens.isEmpty()) {
            Label vazio = new Label("Nenhuma notificação para este filtro.");
            vazio.setSclass("nsp-empty");
            tabela.appendChild(vazio);
        }

        for (Notificacao n : resultado.itens) {
            String chaveDup = n.getProntuario() == null ? null
                    : NotificacaoDAO.chaveDuplicidade(n.getProntuario(), n.getGravidade());
            boolean dup = chaveDup != null && duplicatas.containsKey(chaveDup);
            Div linha = new Div();
            linha.setSclass("nsp-row" + (dup ? " dup" : ""));

            Div idCelula = celula("#" + n.getId(), "90px");
            Label datas = new Label(ouTraco(n.getDataIncidente()));
            datas.setStyle("display:block;font-size:10.5px;color:var(--muted)");
            if (n.getCriadoEm() != null) {
                datas.setTooltiptext("Recebida em " + formatoData.format(n.getCriadoEm()));
            }
            idCelula.appendChild(datas);
            linha.appendChild(idCelula);

            linha.appendChild(envolver(tagTipo(n.getTipo()), "150px"));
            linha.appendChild(celula(ouTraco(n.getSubgrupo()), "160px"));
            linha.appendChild(celula(ouTraco(n.getGravidade()), "150px"));
            linha.appendChild(n.getNivelRisco() != null ? envolver(tagRisco(n.getNivelRisco()), "90px") : celula("—", "90px"));

            Div prontCelula = celula(ouTraco(n.getProntuario()), "160px");
            if (dup) {
                Label flag = new Label(duplicatas.get(chaveDup) + " c/ prontuário e gravidade");
                flag.setSclass("nsp-dupflag");
                prontCelula.appendChild(flag);
            }
            linha.appendChild(prontCelula);
            linha.appendChild(envolver(tagStatus(n.getStatus()), "130px"));

            Div acao = new Div();
            acao.setSclass("nsp-cell");
            acao.setStyle("flex:0 0 150px;display:flex;gap:12px");
            final long id = n.getId();
            Label detalhes = new Label("Detalhes");
            detalhes.setSclass("nsp-linkbtn");
            detalhes.addEventListener("onClick", event ->
                    Executions.getCurrent().sendRedirect("/admin/detalhe.zul?id=" + id));
            acao.appendChild(detalhes);
            Label etapa = new Label(rotuloEtapa(n.getStatus()));
            etapa.setSclass("nsp-linkbtn");
            etapa.addEventListener("onClick", event ->
                    Executions.getCurrent().sendRedirect(destinoEtapa(n.getStatus()) + "?id=" + id));
            acao.appendChild(etapa);
            linha.appendChild(acao);

            tabela.appendChild(linha);
        }
        scroll.appendChild(tabela);
        areaTabela.appendChild(scroll);
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

    private Div celula(String texto, String largura) {
        Div div = new Div();
        div.setSclass("nsp-cell");
        div.setStyle("flex:0 0 " + largura);
        Label lbl = new Label(texto);
        lbl.setTooltiptext(texto);
        div.appendChild(lbl);
        return div;
    }

    private Div envolver(Label tag, String largura) {
        Div div = new Div();
        div.setSclass("nsp-cell");
        div.setStyle("flex:0 0 " + largura);
        div.appendChild(tag);
        return div;
    }

    private String ouTraco(String s) {
        return s == null || s.trim().isEmpty() ? "—" : s;
    }
}
