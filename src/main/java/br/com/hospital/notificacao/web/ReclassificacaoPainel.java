package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.model.ClassificacaoIncidente;
import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.TipoNotificacao;
import br.com.hospital.notificacao.model.Usuario;
import br.com.hospital.notificacao.service.MatrizRisco;
import br.com.hospital.notificacao.service.ReclassificacaoService;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

import java.util.List;
import java.util.Objects;

/**
 * Painel unico de reclassificacao (Triagem, Investigacao e Detalhes): tipo/canal, problema
 * (subgrupo), classificacao do incidente, risco opcional e descricao revisada — com a
 * descricao original sempre visivel e somente leitura ao lado.
 */
public class ReclassificacaoPainel extends Div {

    private final ReclassificacaoService reclassificacaoService = new ReclassificacaoService();

    private final Notificacao notificacao;
    private final Runnable aoSalvar;

    private final Combobox cboTipo = combo();
    private final Combobox cboSubgrupo = combo();
    private final Combobox cboGravidade = combo();
    private Combobox cboProbabilidade;
    private Combobox cboImpacto;
    private Label lblNivel;
    private final Textbox txtRevisada = new Textbox();

    /**
     * @param comRisco inclui probabilidade x impacto (a Triagem nao usa: ela tem a matriz)
     * @param aoSalvar executado depois de uma reclassificacao gravada (ex.: recarregar a pagina)
     */
    public ReclassificacaoPainel(Notificacao notificacao, boolean comRisco, Runnable aoSalvar) {
        this.notificacao = notificacao;
        this.aoSalvar = aoSalvar;

        Div grade = new Div();
        grade.setStyle("display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:12px;margin-bottom:12px");

        for (TipoNotificacao t : TipoNotificacao.values()) {
            Comboitem item = new Comboitem(t.getTitulo());
            item.setValue(t);
            cboTipo.appendChild(item);
            if (t == notificacao.getTipo()) cboTipo.setSelectedItem(item);
        }
        cboTipo.addEventListener("onSelect", e -> recarregarSubgrupos(cboSubgrupo.getValue()));
        grade.appendChild(campo("Tipo de notificação", cboTipo));

        recarregarSubgrupos(notificacao.getSubgrupo());
        grade.appendChild(campo("Problema (subgrupo)", cboSubgrupo));

        for (String g : ClassificacaoIncidente.TODAS) {
            Comboitem item = new Comboitem(g);
            cboGravidade.appendChild(item);
            if (g.equals(notificacao.getGravidade())) cboGravidade.setSelectedItem(item);
        }
        grade.appendChild(campo("Classificação do incidente", cboGravidade));

        if (comRisco) {
            cboProbabilidade = comboEixo(MatrizRisco.PROBABILIDADE, notificacao.getProbabilidade());
            cboImpacto = comboEixo(MatrizRisco.IMPACTO, notificacao.getImpacto());
            grade.appendChild(campo("Probabilidade", cboProbabilidade));
            grade.appendChild(campo("Impacto", cboImpacto));
            lblNivel = new Label();
            lblNivel.setSclass("nsp-tg");
            Div nivelWrap = new Div();
            nivelWrap.setStyle("padding-top:6px");
            nivelWrap.appendChild(lblNivel);
            grade.appendChild(campo("Risco resultante", nivelWrap));
            cboProbabilidade.addEventListener("onSelect", e -> atualizarNivel());
            cboImpacto.addEventListener("onSelect", e -> atualizarNivel());
            atualizarNivel();
        }
        appendChild(grade);

        Div descricoes = new Div();
        descricoes.setStyle("display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:12px");
        Label original = new Label(notificacao.getDescricao());
        original.setMultiline(true);
        original.setSclass("nsp-desc-original");
        descricoes.appendChild(campo("Descrição original (notificante · não editável)", original));

        txtRevisada.setMultiline(true);
        txtRevisada.setRows(4);
        txtRevisada.setWidth("100%");
        txtRevisada.setPlaceholder("Reescreva/reclassifique a descrição do problema. Em branco = vale a original.");
        txtRevisada.setValue(notificacao.getDescricaoRevisada());
        descricoes.appendChild(campo("Descrição revisada (NSP)", txtRevisada));
        appendChild(descricoes);

        Label salvar = new Label("Salvar reclassificação");
        salvar.setSclass("nsp-btn nsp-btn-dark");
        salvar.setStyle("display:inline-block;margin-top:12px");
        salvar.addEventListener("onClick", e -> salvar());
        appendChild(salvar);
    }

    private void recarregarSubgrupos(String manterSelecionado) {
        cboSubgrupo.getItems().clear();
        cboSubgrupo.setValue(null);
        TipoNotificacao tipo = tipoSelecionado();
        if (tipo == null) return;
        for (String s : tipo.getSubgrupos()) {
            Comboitem item = new Comboitem(s);
            cboSubgrupo.appendChild(item);
            if (s.equals(manterSelecionado)) cboSubgrupo.setSelectedItem(item);
        }
    }

    private void atualizarNivel() {
        Integer p = valorEixo(cboProbabilidade);
        Integer i = valorEixo(cboImpacto);
        if (p == null || i == null) {
            lblNivel.setValue("—");
            lblNivel.setStyle("");
            return;
        }
        int score = MatrizRisco.calcularScore(p, i);
        NivelRisco nivel = NivelRisco.doScore(score);
        lblNivel.setValue(nivel.getRotulo() + " · " + score);
        lblNivel.setStyle("background:var(--r-" + nivel.getChave() + "-bg);color:var(--r-" + nivel.getChave() + "-fg)");
    }

    private void salvar() {
        ReclassificacaoService.Reclassificacao r = new ReclassificacaoService.Reclassificacao();
        r.tipo = tipoSelecionado();
        r.subgrupo = cboSubgrupo.getSelectedItem() != null ? cboSubgrupo.getSelectedItem().getLabel() : null;
        r.gravidade = cboGravidade.getSelectedItem() != null ? cboGravidade.getSelectedItem().getLabel() : null;
        r.descricaoRevisada = txtRevisada.getValue();
        if (cboProbabilidade != null) {
            r.probabilidade = valorEixo(cboProbabilidade);
            r.impacto = valorEixo(cboImpacto);
        }
        Usuario usuario = SessaoUtil.usuarioAtual();
        boolean alterou;
        try {
            alterou = reclassificacaoService.reclassificar(notificacao.getId(), r, usuario != null ? usuario.getId() : null);
        } catch (IllegalArgumentException ex) {
            Clients.showNotification(ex.getMessage(), "error", null, "top_center", 3500);
            return;
        }
        if (!alterou) {
            Clients.showNotification("Nenhuma alteração para salvar", "info", null, "top_center", 2500);
            return;
        }
        Clients.showNotification("Reclassificação registrada no histórico", "info", null, "top_center", 2500);
        aoSalvar.run();
    }

    private TipoNotificacao tipoSelecionado() {
        return cboTipo.getSelectedItem() != null ? (TipoNotificacao) cboTipo.getSelectedItem().getValue() : null;
    }

    private static Integer valorEixo(Combobox cb) {
        return cb.getSelectedItem() != null ? (Integer) cb.getSelectedItem().getValue() : null;
    }

    private static Combobox comboEixo(List<MatrizRisco.Eixo> eixos, Integer atual) {
        Combobox cb = combo();
        for (MatrizRisco.Eixo eixo : eixos) {
            Comboitem item = new Comboitem(eixo.getRotulo() + " (" + eixo.getValor() + ")");
            item.setValue(eixo.getValor());
            cb.appendChild(item);
            if (Objects.equals(atual, eixo.getValor())) cb.setSelectedItem(item);
        }
        return cb;
    }

    private static Combobox combo() {
        Combobox cb = new Combobox();
        cb.setReadonly(true);
        cb.setWidth("100%");
        return cb;
    }

    private static Div campo(String rotulo, org.zkoss.zk.ui.Component componente) {
        Div wrapper = new Div();
        Label lbl = new Label(rotulo);
        lbl.setSclass("nsp-field-label");
        wrapper.appendChild(lbl);
        wrapper.appendChild(componente);
        return wrapper;
    }
}
