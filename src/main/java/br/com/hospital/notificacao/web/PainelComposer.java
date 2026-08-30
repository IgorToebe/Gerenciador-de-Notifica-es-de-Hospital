package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.model.TipoNotificacao;
import br.com.hospital.notificacao.service.NotificacaoService;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

import java.util.Map;

/** Painel / visão geral: KPIs, etapas do fluxo e distribuição por tipo (porte de renderPainel()/barChart()). */
public class PainelComposer extends AdminComposer {

    private final NotificacaoService notificacaoService = new NotificacaoService();

    @Override
    protected String paginaAtiva() {
        return "painel";
    }

    @Override
    protected void renderConteudo(Div conteudo) {
        Map<StatusNotificacao, Integer> porStatus = notificacaoService.contarPorStatus();
        int backlog = porStatus.get(StatusNotificacao.ABERTO) + porStatus.get(StatusNotificacao.TRIAGEM)
                + porStatus.get(StatusNotificacao.INVESTIGACAO) + porStatus.get(StatusNotificacao.PLANO);
        int novas = notificacaoService.contarCriadasHoje();

        Div kpis = new Div();
        kpis.setSclass("nsp-kpis");
        kpis.appendChild(kpi("Backlog (em aberto)", backlog, "#c0392b", "inclui pendências acumuladas"));
        kpis.appendChild(kpi("Novas (hoje)", novas, "#2f80d6", "via QR Code"));
        kpis.appendChild(kpi("Em investigação", porStatus.get(StatusNotificacao.INVESTIGACAO), "#6b4fc0", "FOR-NSP-014"));
        kpis.appendChild(kpi("Plano em execução", porStatus.get(StatusNotificacao.PLANO), "#dd6f1e", "5W2H · SGQ-001"));
        kpis.appendChild(kpi("Concluídas", porStatus.get(StatusNotificacao.CONCLUIDO), "#4e9c3e", "taxa de resolução"));
        conteudo.appendChild(kpis);

        Div nota = new Div();
        nota.setSclass("nsp-note");
        nota.appendChild(new Label("Antes: o revisor orquestrava 6 planilhas separadas manualmente. Aqui a " +
                "notificação entra pelo QR e percorre um fluxo único — triagem → investigação → plano de ação → " +
                "indicadores — sem reentrada de dados."));
        conteudo.appendChild(nota);

        Div painelEtapas = new Div();
        painelEtapas.setSclass("nsp-panel");
        Label tituloEtapas = new Label("Notificações por etapa do fluxo");
        tituloEtapas.setStyle("display:block;font-size:16px;font-weight:800;margin-bottom:14px;color:#23201c");
        painelEtapas.appendChild(tituloEtapas);
        painelEtapas.appendChild(construirStepperContagens(porStatus));
        conteudo.appendChild(painelEtapas);

        Div painelTipos = new Div();
        painelTipos.setSclass("nsp-panel");
        Label tituloTipos = new Label("Distribuição por tipo");
        tituloTipos.setStyle("display:block;font-size:16px;font-weight:800;margin-bottom:14px;color:#23201c");
        painelTipos.appendChild(tituloTipos);
        painelTipos.appendChild(construirBarChart(notificacaoService.contarPorTipo()));
        conteudo.appendChild(painelTipos);
    }

    private Div kpi(String rotulo, int valor, String corBorda, String legenda) {
        Div div = new Div();
        div.setSclass("nsp-kpi");
        div.setStyle("border-left-color:" + corBorda);
        Label lab = new Label(rotulo);
        lab.setSclass("lab");
        lab.setStyle("display:block");
        Label val = new Label(String.valueOf(valor));
        val.setSclass("val");
        val.setStyle("display:block");
        Label delta = new Label(legenda);
        delta.setSclass("delta");
        delta.setStyle("display:block");
        div.appendChild(lab);
        div.appendChild(val);
        div.appendChild(delta);
        return div;
    }

    private Div construirStepperContagens(Map<StatusNotificacao, Integer> porStatus) {
        Div stepper = new Div();
        stepper.setSclass("nsp-stepper");
        StatusNotificacao[] ordem = StatusNotificacao.values();
        for (int i = 0; i < ordem.length; i++) {
            StatusNotificacao s = ordem[i];
            Div st = new Div();
            String classe = s == StatusNotificacao.CONCLUIDO ? "done" : (porStatus.get(s) > 0 ? "act" : "");
            st.setSclass("st " + classe);
            Label numero = new Label(String.valueOf(i + 1));
            Label rotulo = new Label(s.getRotulo());
            rotulo.setStyle("display:block");
            Label contagem = new Label(String.valueOf(porStatus.get(s)));
            contagem.setStyle("display:block;color:var(--muted)");
            st.appendChild(numero);
            st.appendChild(rotulo);
            st.appendChild(contagem);
            stepper.appendChild(st);
            if (i < ordem.length - 1) {
                Div bar = new Div();
                bar.setSclass("bar");
                stepper.appendChild(bar);
            }
        }
        return stepper;
    }

    private Div construirBarChart(Map<TipoNotificacao, Integer> porTipo) {
        int max = Math.max(1, porTipo.values().stream().mapToInt(Integer::intValue).max().orElse(1));
        Div wrapper = new Div();
        wrapper.setStyle("display:flex;flex-direction:column;gap:10px");
        for (TipoNotificacao tipo : TipoNotificacao.values()) {
            int valor = porTipo.getOrDefault(tipo, 0);
            int largura = (int) Math.round(valor * 100.0 / max);

            Div linha = new Div();
            linha.setStyle("display:flex;align-items:center;gap:12px");

            Label nome = new Label(tipo.getTitulo());
            nome.setStyle("flex:0 0 190px;font-size:12.5px;color:#3a352f");

            Div trilha = new Div();
            trilha.setStyle("flex:1;background:#f0ece4;border-radius:7px;height:22px;overflow:hidden");
            Div barra = new Div();
            barra.setStyle("width:" + largura + "%;height:100%;background:" + tipo.getCor() + ";border-radius:7px");
            trilha.appendChild(barra);

            Label valorLbl = new Label(String.valueOf(valor));
            valorLbl.setStyle("flex:0 0 28px;text-align:right;font-weight:700;font-size:13px;color:#23201c");

            linha.appendChild(nome);
            linha.appendChild(trilha);
            linha.appendChild(valorLbl);
            wrapper.appendChild(linha);
        }
        return wrapper;
    }
}
