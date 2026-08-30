package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.model.TipoNotificacao;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;

/** Cartazes com os 5 QR Codes (um por canal), prontos para impressão. */
public class QrCodesComposer extends AdminComposer {

    @Override
    protected String paginaAtiva() {
        return "qrcodes";
    }

    @Override
    protected void renderConteudo(Div conteudo) {
        Div grid = new Div();
        grid.setStyle("display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:18px");
        for (TipoNotificacao tipo : TipoNotificacao.values()) {
            Div card = new Div();
            card.setStyle("background:#fff;border:1px solid var(--line);border-radius:14px;padding:18px;text-align:center;border-top:5px solid " + tipo.getCor());

            Label titulo = new Label(tipo.getTitulo());
            titulo.setStyle("display:block;font-weight:800;font-size:14px;margin-bottom:4px;color:#23201c");
            Label codigo = new Label(tipo.getCodigoFormulario());
            codigo.setStyle("display:block;font-size:11px;color:var(--muted);margin-bottom:12px");

            Html img = new Html("<img src=\"/qr?tipo=" + tipo.getChaveUrl() + "&size=220\" width=\"180\" height=\"180\" " +
                    "style=\"border-radius:8px;border:1px solid " + tipo.getCorAnel() + "\"/>");

            Label url = new Label("/n/" + tipo.getChaveUrl());
            url.setStyle("display:block;margin-top:10px;font-size:11px;color:" + tipo.getCorEscura() + ";font-family:monospace");

            card.appendChild(titulo);
            card.appendChild(codigo);
            card.appendChild(img);
            card.appendChild(url);
            grid.appendChild(card);
        }
        conteudo.appendChild(grid);
    }
}
