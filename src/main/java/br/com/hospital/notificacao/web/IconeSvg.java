package br.com.hospital.notificacao.web;

import org.zkoss.zul.Html;

import java.util.HashMap;
import java.util.Map;

/**
 * Renderiza os icones SVG dos 5 tipos de notificacao (mesmos paths do objeto ICONS do
 * prototipo funcional). O zhtml do ZK nao tem elemento &lt;svg&gt; nativo, entao o markup
 * e' injetado via {@link Html}, que aceita HTML bruto dentro do componente.
 */
public final class IconeSvg {

    private static final Map<String, String> PATHS = new HashMap<>();
    static {
        PATHS.put("caduceus", "<path d=\"M12 2a2 2 0 00-2 2c0 .7.4 1.3 1 1.7V7H8v2h3v2H9v2h2v6h2v-6h2v-2h-2V9h3V7h-3V5.7c.6-.4 1-1 1-1.7a2 2 0 00-2-2z\"/>");
        PATHS.put("pill", "<path d=\"M10.5 3.5a4 4 0 015.7 5.7l-7 7a4 4 0 01-5.7-5.7l7-7zM9 9l4 4\"/>");
        PATHS.put("person", "<path d=\"M12 12a4 4 0 100-8 4 4 0 000 8zm-7 8a7 7 0 0114 0H5z\"/>");
        PATHS.put("venus", "<path d=\"M12 3a5 5 0 100 10 5 5 0 000-10zm0 10v5m-3-3h6\"/>");
        PATHS.put("heart", "<path d=\"M12 21s-7-4.5-9.5-9C1 9 2.5 5 6 5c2 0 3.2 1.2 4 2.4C10.8 6.2 12 5 14 5c3.5 0 5 4 3.5 7-2.5 4.5-9.5 9-9.5 9z\"/>");
    }

    private IconeSvg() { }

    public static Html criar(String icone, boolean preenchido, String corHexTraco, int tamanho) {
        String path = PATHS.getOrDefault(icone, PATHS.get("heart"));
        String atributos = preenchido
                ? "fill=\"" + corHexTraco + "\""
                : "fill=\"none\" stroke=\"" + corHexTraco + "\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\"";
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + tamanho + "\" height=\"" + tamanho +
                "\" viewBox=\"0 0 24 24\" " + atributos + ">" + path + "</svg>";
        return new Html(svg);
    }
}
