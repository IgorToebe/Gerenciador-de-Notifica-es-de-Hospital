package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.dao.NotificacaoDAO;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.NivelRisco;
import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.model.Usuario;
import br.com.hospital.notificacao.service.MatrizRisco;
import br.com.hospital.notificacao.service.NotificacaoService;
import br.com.hospital.notificacao.service.TriagemService;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

import java.util.Optional;

/**
 * Triagem &amp; classificação (Item 3): pré-visualização dos detalhes da notificação antes
 * da triagem formal (ajuste pedido pelo cliente na ata de validação), matriz de risco
 * Probabilidade x Impacto e avanço para investigação.
 */
public class TriagemComposer extends AdminComposer {

    private final NotificacaoService notificacaoService = new NotificacaoService();
    private final TriagemService triagemService = new TriagemService();

    private Notificacao notificacao;
    private Integer probSelecionada;
    private Integer impSelecionado;
    private Div areaMatriz;
    private Div areaResultado;

    @Override
    protected String paginaAtiva() {
        return "triagem";
    }

    @Override
    protected void renderConteudo(Div conteudo) {
        notificacao = carregarNotificacao();
        if (notificacao == null) {
            Label vazio = new Label("Nenhuma notificação aguardando triagem no momento.");
            vazio.setSclass("nsp-empty");
            conteudo.appendChild(vazio);
            return;
        }
        probSelecionada = notificacao.getProbabilidade();
        impSelecionado = notificacao.getImpacto();

        conteudo.appendChild(construirCard());
        conteudo.appendChild(construirPainelMatriz());
    }

    private Notificacao carregarNotificacao() {
        String idParam = Executions.getCurrent().getParameter("id");
        if (idParam != null) {
            try {
                return notificacaoService.carregarCompleta(Long.parseLong(idParam));
            } catch (Exception ignored) { /* cai para a busca padrão abaixo */ }
        }
        NotificacaoDAO.Filtro f = new NotificacaoDAO.Filtro();
        f.status = StatusNotificacao.ABERTO;
        f.tamanhoPagina = 1;
        NotificacaoDAO.ResultadoPagina r = notificacaoService.listar(f);
        if (r.itens.isEmpty()) {
            f.status = StatusNotificacao.TRIAGEM;
            r = notificacaoService.listar(f);
        }
        return r.itens.isEmpty() ? null : notificacaoService.carregarCompleta(r.itens.get(0).getId());
    }

    private Div construirCard() {
        Div card = new Div();
        card.setSclass("nsp-tcard");
        card.setStyle("border-left-color:" + notificacao.getTipo().getCor());

        Label titulo = new Label("Notif. #" + notificacao.getId() + " — " + notificacao.getTipo().getTitulo());
        titulo.setStyle("display:block;font-size:15px;font-weight:800;color:#23201c;margin-bottom:4px");
        card.appendChild(titulo);

        String anonTexto = notificacao.isAnonimo() ? "Anônimo" : "Contato vinculado";
        Label meta = new Label(notificacao.getDataIncidente() + " · " + anonTexto + " · " + notificacao.getSetor()
                + " · pré-classificada como “" + notificacao.getGravidade() + "”");
        meta.setStyle("display:block;font-size:12px;color:var(--muted);margin-bottom:6px");
        card.appendChild(meta);

        Label desc = new Label(notificacao.getDescricao());
        desc.setStyle("display:block;font-size:13px;color:#4a443c;margin-bottom:10px");
        card.appendChild(desc);

        // Pré-visualização dos detalhes completos antes de iniciar a triagem formal.
        Div detalhes = new Div();
        detalhes.setStyle("display:none;background:#faf8f4;border-radius:10px;padding:10px 12px;margin-bottom:10px");
        if (!notificacao.getCampos().isEmpty()) {
            notificacao.getCampos().forEach(c -> {
                Label linha = new Label(c.getRotulo() + ": " + c.getValor());
                linha.setStyle("display:block;font-size:12px;color:#3a352f;margin-bottom:3px");
                detalhes.appendChild(linha);
            });
        }
        Label anexosInfo = new Label(notificacao.getAnexos().size() + " anexo(s)");
        anexosInfo.setStyle("display:block;font-size:11.5px;color:var(--muted);margin-top:4px");
        detalhes.appendChild(anexosInfo);

        Label expandir = new Label("Ver detalhes completos ▾");
        expandir.setSclass("nsp-linkbtn");
        expandir.addEventListener("onClick", e -> {
            boolean visivel = detalhes.getStyle() != null && detalhes.getStyle().contains("display:block");
            detalhes.setStyle(visivel
                    ? "display:none;background:#faf8f4;border-radius:10px;padding:10px 12px;margin-bottom:10px"
                    : "display:block;background:#faf8f4;border-radius:10px;padding:10px 12px;margin-bottom:10px");
            expandir.setValue(visivel ? "Ver detalhes completos ▾" : "Ocultar detalhes ▴");
        });
        card.appendChild(expandir);
        card.appendChild(detalhes);

        Div acoes = new Div();
        acoes.setStyle("display:flex;gap:9px;align-items:center;margin-top:12px;flex-wrap:wrap");

        Combobox reclassificar = new Combobox();
        reclassificar.setReadonly(true);
        reclassificar.setWidth("220px");
        String[] gravidades = {"Near miss (quase erro)", "Incidente sem dano", "Dano leve", "Dano moderado",
                "Dano grave", "Óbito"};
        for (String g : gravidades) {
            Comboitem item = new Comboitem(g);
            reclassificar.appendChild(item);
            if (g.equals(notificacao.getGravidade())) reclassificar.setSelectedItem(item);
        }
        acoes.appendChild(reclassificar);

        Label btnIniciarInvestigacao = new Label("Iniciar investigação →");
        btnIniciarInvestigacao.setSclass("nsp-linkbtn");
        btnIniciarInvestigacao.setStyle("background:#dd6f1e;color:#fff;padding:9px 15px;border-radius:9px;font-weight:600;cursor:pointer");
        final long idNotificacao = notificacao.getId();
        btnIniciarInvestigacao.addEventListener("onClick", e -> {
            Usuario usuario = SessaoUtil.usuarioAtual();
            triagemService.iniciarInvestigacao(idNotificacao, usuario != null ? usuario.getId() : null);
            Executions.getCurrent().sendRedirect("/admin/investigacao.zul?id=" + idNotificacao);
        });
        acoes.appendChild(btnIniciarInvestigacao);
        card.appendChild(acoes);

        this.comboReclassificar = reclassificar;
        return card;
    }

    private Combobox comboReclassificar;

    private Div construirPainelMatriz() {
        Div painel = new Div();
        painel.setSclass("nsp-panel");
        Label titulo = new Label("Classificação de risco — Matriz Probabilidade × Impacto");
        titulo.setStyle("display:block;font-size:16px;font-weight:800;color:#23201c");
        Label meta = new Label("Base_Classificação (planilha SGQ) · ANVISA. Clique numa célula para reclassificar.");
        meta.setStyle("display:block;font-size:12.5px;color:var(--muted);margin-bottom:14px");
        painel.appendChild(titulo);
        painel.appendChild(meta);

        Div wrap = new Div();
        wrap.setStyle("display:flex;gap:26px;flex-wrap:wrap;align-items:flex-start");

        areaMatriz = new Div();
        montarMatriz();
        wrap.appendChild(areaMatriz);

        areaResultado = new Div();
        wrap.appendChild(areaResultado);
        atualizarResultado();

        painel.appendChild(wrap);
        return painel;
    }

    private void montarMatriz() {
        areaMatriz.getChildren().clear();
        for (MatrizRisco.Eixo p : MatrizRisco.PROBABILIDADE) {
            Div linha = new Div();
            linha.setStyle("display:flex;align-items:center;gap:6px;margin-bottom:6px");
            Label rotulo = new Label(p.getRotulo());
            rotulo.setStyle("flex:0 0 96px;font-size:10px;color:var(--muted);font-weight:700;text-align:right;padding-right:4px");
            linha.appendChild(rotulo);
            for (MatrizRisco.Eixo i : MatrizRisco.IMPACTO) {
                int score = MatrizRisco.calcularScore(p.getValor(), i.getValor());
                NivelRisco nivel = NivelRisco.doScore(score);
                boolean selecionada = p.getValor() == intOu(probSelecionada, -1) && i.getValor() == intOu(impSelecionado, -1);
                Label cell = new Label(String.valueOf(score));
                cell.setSclass("nsp-matrix-cell" + (selecionada ? " sel" : ""));
                cell.setStyle("background:var(--r-" + nivel.getChave() + ");cursor:pointer;display:flex;align-items:center;justify-content:center;margin-right:4px");
                final int pv = p.getValor();
                final int iv = i.getValor();
                cell.addEventListener("onClick", e -> {
                    probSelecionada = pv;
                    impSelecionado = iv;
                    montarMatriz();
                    atualizarResultado();
                });
                linha.appendChild(cell);
            }
            areaMatriz.appendChild(linha);
        }
    }

    private void atualizarResultado() {
        areaResultado.getChildren().clear();
        areaResultado.setStyle("min-width:230px");
        Div card = new Div();
        card.setSclass("nsp-card-soft");
        if (probSelecionada == null || impSelecionado == null) {
            card.appendChild(new Label("Selecione uma célula da matriz."));
        } else {
            int score = MatrizRisco.calcularScore(probSelecionada, impSelecionado);
            NivelRisco nivel = NivelRisco.doScore(score);
            Label pontuacao = new Label("Pontuação: " + score);
            pontuacao.setStyle("display:block;font-size:13px;margin-bottom:6px");
            Label nivelLbl = new Label(nivel.getRotulo());
            nivelLbl.setSclass("nsp-tg");
            nivelLbl.setStyle("background:var(--r-" + nivel.getChave() + "-bg);color:var(--r-" + nivel.getChave() + "-fg);font-size:14px");
            card.appendChild(pontuacao);
            card.appendChild(nivelLbl);
            if (nivel == NivelRisco.ALTO || nivel == NivelRisco.EXTREMO) {
                Label alerta = new Label("⚠ Risco Alto/Extremo dispara alerta ao parecerista.");
                alerta.setStyle("display:block;margin-top:8px;font-size:11px;color:#b1571a");
                card.appendChild(alerta);
            }
        }
        areaResultado.appendChild(card);

        Label aplicar = new Label("Aplicar reclassificação");
        aplicar.setStyle("display:block;text-align:center;width:100%;margin-top:12px;background:#2b2723;color:#fff;" +
                "padding:11px;border-radius:10px;cursor:pointer;font-weight:600;font-size:13px");
        aplicar.addEventListener("onClick", e -> aplicarReclassificacao());
        areaResultado.appendChild(aplicar);
    }

    private void aplicarReclassificacao() {
        if (probSelecionada == null || impSelecionado == null) {
            Clients.showNotification("Selecione uma célula da matriz antes de aplicar.", "error", null, "top_center", 3000);
            return;
        }
        String gravidade = comboReclassificar.getSelectedItem() != null
                ? comboReclassificar.getSelectedItem().getLabel() : notificacao.getGravidade();
        Usuario usuario = SessaoUtil.usuarioAtual();
        NivelRisco nivel = triagemService.aplicarClassificacao(notificacao.getId(), gravidade, probSelecionada,
                impSelecionado, usuario != null ? usuario.getId() : null);
        String extra = (nivel == NivelRisco.ALTO || nivel == NivelRisco.EXTREMO) ? " · alerta enviado ao parecerista" : "";
        Clients.showNotification("Risco reclassificado: " + nivel.getRotulo() + extra, "info", null, "top_center", 3000);
        Executions.getCurrent().sendRedirect("/admin/triagem.zul?id=" + notificacao.getId());
    }

    private int intOu(Integer valor, int padrao) {
        return valor != null ? valor : padrao;
    }
}
