package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.dao.NotificacaoDAO;
import br.com.hospital.notificacao.model.Investigacao;
import br.com.hospital.notificacao.model.MarcoInvestigacao;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.model.Usuario;
import br.com.hospital.notificacao.service.InvestigacaoService;
import br.com.hospital.notificacao.service.NotificacaoService;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

/**
 * Investigação e Análise de Incidente (FOR-NSP-014): timeline, fatores contribuintes
 * (Protocolo de Londres) e 5 Porquês. A causa raiz (5º porquê) é obrigatória para gerar
 * o plano de ação (Item 4 dos critérios de aceitação).
 */
public class InvestigacaoComposer extends AdminComposer {

    private final NotificacaoService notificacaoService = new NotificacaoService();
    private final InvestigacaoService investigacaoService = new InvestigacaoService();

    private Notificacao notificacao;
    private Investigacao investigacao;

    private Textbox txtParecerista, txtData, txtFontes;
    private Textbox txtProf, txtCom, txtPac, txtAmb, txtOrg, txtExt;
    private Textbox txtP1, txtP2, txtP3, txtP4, txtCausaRaiz;
    private Textbox txtDeteccao, txtAtenuantes, txtParecer;

    @Override
    protected String paginaAtiva() {
        return "invest";
    }

    @Override
    protected void renderConteudo(Div conteudo) {
        notificacao = carregarNotificacao();
        if (notificacao == null) {
            Label vazio = new Label("Nenhuma notificação em investigação no momento.");
            vazio.setSclass("nsp-empty");
            conteudo.appendChild(vazio);
            return;
        }
        investigacao = investigacaoService.carregarOuCriar(notificacao.getId());

        Div painel = new Div();
        painel.setSclass("nsp-panel");

        Label titulo = new Label("Investigação e Análise de Incidente");
        titulo.setStyle("display:block;font-size:17px;font-weight:800;color:#23201c");
        Label meta = new Label("FOR-NSP-014-2026 · Notif. #" + notificacao.getId() + " — " + notificacao.getTipo().getTitulo()
                + " · Prontuário " + (isEmpty(notificacao.getProntuario()) ? "—" : notificacao.getProntuario())
                + " · Risco " + (notificacao.getNivelRisco() != null ? notificacao.getNivelRisco().getRotulo() : "—"));
        meta.setStyle("display:block;font-size:12.5px;color:var(--muted);margin-bottom:14px");
        painel.appendChild(titulo);
        painel.appendChild(meta);

        Div cabecalho = new Div();
        cabecalho.setStyle("display:grid;grid-template-columns:1fr 1fr 1fr;gap:14px;margin-bottom:8px");
        txtParecerista = campoGrid(cabecalho, "NEA / parecerista", investigacao.getParecerista());
        txtData = campoGrid(cabecalho, "Data da investigação", investigacao.getDataInvestigacao());
        txtFontes = campoGrid(cabecalho, "Fontes de informação", investigacao.getFontes());
        painel.appendChild(cabecalho);

        painel.appendChild(secTitulo("A", "Timeline do incidente"));
        painel.appendChild(construirTimeline());

        painel.appendChild(secTitulo("B", "Fatores contribuintes · Protocolo de Londres"));
        Div cfgrid = new Div();
        cfgrid.setStyle("display:grid;grid-template-columns:1fr 1fr;gap:13px;margin-bottom:8px");
        txtProf = campoFator(cfgrid, "nsp-cf-prof", "Profissional — cognitivos / desempenho / comportamento", investigacao.getFatorProfissional());
        txtCom = campoFator(cfgrid, "nsp-cf-com", "Comunicação", investigacao.getFatorComunicacao());
        txtPac = campoFator(cfgrid, "nsp-cf-pac", "Dirigidos ao paciente / acompanhante", investigacao.getFatorPaciente());
        txtAmb = campoFator(cfgrid, "nsp-cf-amb", "Trabalho / ambiente", investigacao.getFatorAmbiente());
        txtOrg = campoFator(cfgrid, "nsp-cf-org", "Organizacionais", investigacao.getFatorOrganizacional());
        txtExt = campoFator(cfgrid, "nsp-cf-ext", "Externos", investigacao.getFatorExterno());
        painel.appendChild(cfgrid);

        painel.appendChild(secTitulo("C", "Análise de causa raiz — 5 Porquês"));
        Div whys = new Div();
        whys.setSclass("nsp-whys");
        whys.setStyle("display:flex;flex-direction:column;gap:9px;margin-bottom:8px");
        txtP1 = campoWhy(whys, "1º Por quê?", "Sintoma", investigacao.getPorque1());
        txtP2 = campoWhy(whys, "2º Por quê?", "", investigacao.getPorque2());
        txtP3 = campoWhy(whys, "3º Por quê?", "", investigacao.getPorque3());
        txtP4 = campoWhy(whys, "4º Por quê?", "Causa", investigacao.getPorque4());
        txtCausaRaiz = campoWhy(whys, "5º Por quê?", "Causa raiz *", investigacao.getCausaRaiz());
        painel.appendChild(whys);

        painel.appendChild(secTitulo("D", "Detecção e fatores atenuantes"));
        Div row2 = new Div();
        row2.setStyle("display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-bottom:8px");
        txtDeteccao = campoGridArea(row2, "Como foi identificado", investigacao.getDeteccao());
        txtAtenuantes = campoGridArea(row2, "Fatores atenuantes aplicados", investigacao.getAtenuantes());
        painel.appendChild(row2);

        Label parecerLbl = new Label("Parecer e conclusão do NSP");
        parecerLbl.setSclass("nsp-field-label");
        parecerLbl.setStyle("margin-top:14px;display:block");
        painel.appendChild(parecerLbl);
        txtParecer = new Textbox();
        txtParecer.setMultiline(true);
        txtParecer.setRows(3);
        txtParecer.setWidth("100%");
        txtParecer.setValue(investigacao.getParecer());
        painel.appendChild(txtParecer);

        Div rodape = new Div();
        rodape.setStyle("display:flex;justify-content:flex-end;gap:11px;margin-top:22px;flex-wrap:wrap");
        Label salvar = botao("Salvar rascunho", "#fff", "#3a352f", "1px solid var(--ring)");
        salvar.addEventListener("onClick", e -> salvarRascunho());
        rodape.appendChild(salvar);

        if (notificacao.getStatus() == StatusNotificacao.INVESTIGACAO) {
            Label gerarPlano = botao("Gerar plano de ação a partir da causa raiz →", "#2f80d6", "#fff", "none");
            gerarPlano.addEventListener("onClick", e -> gerarPlano());
            rodape.appendChild(gerarPlano);
        } else {
            Label verPlano = botao("Ver plano de ação →", "#2b2723", "#fff", "none");
            verPlano.addEventListener("onClick", e -> Executions.getCurrent().sendRedirect("/admin/plano.zul?id=" + notificacao.getId()));
            rodape.appendChild(verPlano);
        }
        painel.appendChild(rodape);

        conteudo.appendChild(painel);
    }

    private Notificacao carregarNotificacao() {
        String idParam = Executions.getCurrent().getParameter("id");
        if (idParam != null) {
            try {
                return notificacaoService.carregarCompleta(Long.parseLong(idParam));
            } catch (Exception ignored) { }
        }
        NotificacaoDAO.Filtro f = new NotificacaoDAO.Filtro();
        f.status = StatusNotificacao.INVESTIGACAO;
        f.tamanhoPagina = 1;
        NotificacaoDAO.ResultadoPagina r = notificacaoService.listar(f);
        return r.itens.isEmpty() ? null : notificacaoService.carregarCompleta(r.itens.get(0).getId());
    }

    private Div construirTimeline() {
        Div tl = new Div();
        tl.setStyle("display:flex;gap:0;overflow-x:auto;padding:6px 2px 12px;margin-bottom:8px");
        for (MarcoInvestigacao m : investigacao.getTimeline()) {
            Div ev = new Div();
            ev.setStyle("flex:0 0 200px;background:#faf8f4;border:1px solid var(--line);border-radius:12px;padding:12px 13px;margin-right:14px");
            Label quando = new Label(m.getQuando());
            quando.setStyle("display:block;font-size:11px;color:#2f80d6;font-weight:700;margin-bottom:5px");
            Label desc = new Label(m.getDescricao());
            desc.setStyle("display:block;font-size:12.5px;color:#3a352f;line-height:1.45");
            ev.appendChild(quando);
            ev.appendChild(desc);
            tl.appendChild(ev);
        }
        Textbox novoMarco = new Textbox();
        novoMarco.setPlaceholder("Descrição do novo marco (Enter para adicionar)");
        novoMarco.setWidth("220px");
        novoMarco.addEventListener("onOK", e -> {
            if (novoMarco.getValue() != null && !novoMarco.getValue().trim().isEmpty()) {
                investigacaoService.adicionarMarco(investigacao.getId(),
                        java.time.LocalDateTime.now().toString().substring(0, 16).replace("T", " · "),
                        novoMarco.getValue().trim());
                Executions.getCurrent().sendRedirect("/admin/investigacao.zul?id=" + notificacao.getId());
            }
        });
        tl.appendChild(novoMarco);
        return tl;
    }

    private Textbox campoGrid(Div pai, String rotulo, String valor) {
        Div wrapper = new Div();
        Label lbl = new Label(rotulo);
        lbl.setStyle("display:block;font-size:13px;font-weight:700;color:#3a352f;margin-bottom:7px");
        Textbox tb = new Textbox();
        tb.setValue(valor);
        tb.setWidth("100%");
        wrapper.appendChild(lbl);
        wrapper.appendChild(tb);
        pai.appendChild(wrapper);
        return tb;
    }

    private Textbox campoGridArea(Div pai, String rotulo, String valor) {
        Div wrapper = new Div();
        Label lbl = new Label(rotulo);
        lbl.setStyle("display:block;font-size:13px;font-weight:700;color:#3a352f;margin-bottom:7px");
        Textbox tb = new Textbox();
        tb.setMultiline(true);
        tb.setRows(3);
        tb.setValue(valor);
        tb.setWidth("100%");
        wrapper.appendChild(lbl);
        wrapper.appendChild(tb);
        pai.appendChild(wrapper);
        return tb;
    }

    private Textbox campoFator(Div pai, String sclasseCor, String titulo, String valor) {
        Div cf = new Div();
        cf.setSclass("nsp-cf " + sclasseCor);
        Div h = new Div();
        h.setSclass("h");
        h.appendChild(new Label(titulo));
        cf.appendChild(h);
        Textbox tb = new Textbox();
        tb.setMultiline(true);
        tb.setRows(3);
        tb.setValue(valor);
        tb.setWidth("100%");
        tb.setStyle("border:none");
        cf.appendChild(tb);
        pai.appendChild(cf);
        return tb;
    }

    private Textbox campoWhy(Div pai, String rotulo, String subRotulo, String valor) {
        Div linha = new Div();
        linha.setStyle("display:flex;align-items:center;gap:11px");
        Label lbl = new Label(rotulo + (subRotulo.isEmpty() ? "" : " · " + subRotulo));
        lbl.setSclass("lbl");
        Textbox tb = new Textbox();
        tb.setValue(valor);
        tb.setWidth("100%");
        linha.appendChild(lbl);
        linha.appendChild(tb);
        pai.appendChild(linha);
        return tb;
    }

    private Div secTitulo(String numero, String texto) {
        Div div = new Div();
        div.setSclass("nsp-sec-title");
        div.appendChild(new Label(numero + " · " + texto));
        return div;
    }

    private Label botao(String texto, String bg, String fg, String borda) {
        Label b = new Label(texto);
        b.setStyle("background:" + bg + ";color:" + fg + ";border:" + borda + ";border-radius:10px;padding:11px 18px;" +
                "font-size:13px;font-weight:600;cursor:pointer");
        return b;
    }

    private void preencherInvestigacaoComFormulario() {
        investigacao.setParecerista(txtParecerista.getValue());
        investigacao.setDataInvestigacao(txtData.getValue());
        investigacao.setFontes(txtFontes.getValue());
        investigacao.setFatorProfissional(txtProf.getValue());
        investigacao.setFatorComunicacao(txtCom.getValue());
        investigacao.setFatorPaciente(txtPac.getValue());
        investigacao.setFatorAmbiente(txtAmb.getValue());
        investigacao.setFatorOrganizacional(txtOrg.getValue());
        investigacao.setFatorExterno(txtExt.getValue());
        investigacao.setPorque1(txtP1.getValue());
        investigacao.setPorque2(txtP2.getValue());
        investigacao.setPorque3(txtP3.getValue());
        investigacao.setPorque4(txtP4.getValue());
        investigacao.setCausaRaiz(txtCausaRaiz.getValue());
        investigacao.setDeteccao(txtDeteccao.getValue());
        investigacao.setAtenuantes(txtAtenuantes.getValue());
        investigacao.setParecer(txtParecer.getValue());
    }

    private void salvarRascunho() {
        preencherInvestigacaoComFormulario();
        investigacaoService.salvarRascunho(investigacao);
        Clients.showNotification("Rascunho salvo", "info", null, "top_center", 2000);
    }

    private void gerarPlano() {
        preencherInvestigacaoComFormulario();
        try {
            investigacaoService.gerarPlanoDeAcao(investigacao, notificacao.getSetor(),
                    SessaoUtil.usuarioAtual() != null ? SessaoUtil.usuarioAtual().getId() : null);
        } catch (IllegalStateException e) {
            Clients.showNotification(e.getMessage(), "error", null, "top_center", 3500);
            return;
        }
        Executions.getCurrent().sendRedirect("/admin/plano.zul?id=" + notificacao.getId());
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
