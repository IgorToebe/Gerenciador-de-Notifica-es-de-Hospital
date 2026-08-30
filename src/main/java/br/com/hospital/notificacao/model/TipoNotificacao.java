package br.com.hospital.notificacao.model;

import java.util.Arrays;
import java.util.List;

/**
 * Os 5 canais de notificacao do NSP HU/UEM, um por QR Code: 4 para profissionais
 * (Tecnovigilancia, Farmacovigilancia, Near Miss Materno, Problemas Assistenciais)
 * e 1 para pacientes/acompanhantes. Cada tipo carrega sua identidade visual (cor de
 * fundo propria, conforme pedido do cliente) e os campos dinamicos do passo 2 do wizard.
 *
 * Esta enum e' o espelho em Java do objeto TYPES do protótipo funcional
 * (ProtótipoFuncionalV1_LabEngSoftware.html, linhas ~510-521).
 */
public enum TipoNotificacao {

    TECNOVIGILANCIA(
            "tecno", "Notificação de Tecnovigilância", "Equipamentos e materiais médicos",
            "FOR-NSP-005-2026", "TC", "t-tecno", "caduceus", true,
            "#2f80d6", "#225fa3", "#eaf2fc", "#cfe2f6", "#bcd8f4",
            Arrays.asList(
                    CampoDinamicoDef.texto("produto", "Produto / equipamento afetado", true),
                    CampoDinamicoDef.texto("registroAnvisa", "Registro ANVISA", false),
                    CampoDinamicoDef.texto("lote", "Lote / nº de série", false)
            )),
    FARMACOVIGILANCIA(
            "farmaco", "Notificação de Farmacovigilância", "Medicamentos e vacinas",
            "FOR-NSP-002-2026", "FV", "t-farmaco", "pill", false,
            "#dd6f1e", "#b1571a", "#fbeee6", "#f6d0b6", "#f2c3a3",
            Arrays.asList(
                    CampoDinamicoDef.texto("medicamento", "Medicamento / vacina", true),
                    CampoDinamicoDef.select("via", "Via de administração", false, "Oral", "EV", "IM", "SC", "Tópica"),
                    CampoDinamicoDef.texto("lote", "Lote", false),
                    CampoDinamicoDef.texto("reacao", "Reação adversa observada", false)
            )),
    NEAR_MISS_MATERNO(
            "nearmiss", "Near Miss Materno", "Segurança materna",
            "FOR-NSP-004-2026", "NM", "t-nearmiss", "venus", false,
            "#d6336c", "#a82654", "#fce9f0", "#f6cdde", "#f1bcd2",
            Arrays.asList(
                    CampoDinamicoDef.select("tipoEvento", "Tipo de evento materno", true,
                            "Hemorragia grave", "Pré-eclâmpsia / eclâmpsia", "Sepse", "Ruptura uterina", "Outro near miss"),
                    CampoDinamicoDef.texto("idadeGestacional", "Idade gestacional (semanas)", false),
                    CampoDinamicoDef.select("momento", "Momento", false, "Pré-parto", "Parto", "Pós-parto")
            )),
    PROBLEMAS_ASSISTENCIAIS(
            "assist", "Notificação de Problemas Assistenciais", "Segurança do paciente",
            "FOR-NSP-001-2026", "PA", "t-assist", "heart", true,
            "#4e9c3e", "#3c7a30", "#eef5e3", "#d6ecbb", "#c8e3a8",
            Arrays.asList(
                    CampoDinamicoDef.select("tipoIncidente", "Tipo de incidente", true,
                            "Queda", "Lesão por pressão", "Erro de identificação", "Flebite", "Falha de comunicação", "Outro")
            )),
    NOTIFICACAO_PACIENTE(
            "paciente", "Notificação do Paciente", "Denúncia, reclamação, sugestão ou elogio",
            "Ouvidoria · Segurança do paciente", "NP", "t-paciente", "person", true,
            "#b07a1c", "#8a5f15", "#faf1dd", "#fbe0a3", "#f3d28a",
            Arrays.asList(
                    CampoDinamicoDef.select("tipoManifestacao", "Tipo de manifestação", true,
                            "Denúncia", "Reclamação", "Sugestão", "Elogio"),
                    CampoDinamicoDef.toggle("voceE", "Você é", "Paciente", "Acompanhante")
            ));

    private final String chaveUrl;
    private final String titulo;
    private final String subtitulo;
    private final String codigoFormulario;
    private final String prefixoProtocolo;
    private final String classeCss;
    private final String icone;
    private final boolean iconePreenchido;
    private final String cor;
    private final String corEscura;
    private final String corSuave;
    private final String corCabecalho;
    private final String corAnel;
    private final List<CampoDinamicoDef> camposDinamicos;

    TipoNotificacao(String chaveUrl, String titulo, String subtitulo, String codigoFormulario,
                     String prefixoProtocolo, String classeCss, String icone, boolean iconePreenchido,
                     String cor, String corEscura, String corSuave, String corCabecalho, String corAnel,
                     List<CampoDinamicoDef> camposDinamicos) {
        this.chaveUrl = chaveUrl;
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.codigoFormulario = codigoFormulario;
        this.prefixoProtocolo = prefixoProtocolo;
        this.classeCss = classeCss;
        this.icone = icone;
        this.iconePreenchido = iconePreenchido;
        this.cor = cor;
        this.corEscura = corEscura;
        this.corSuave = corSuave;
        this.corCabecalho = corCabecalho;
        this.corAnel = corAnel;
        this.camposDinamicos = camposDinamicos;
    }

    public static TipoNotificacao porChaveUrl(String chave) {
        for (TipoNotificacao t : values()) {
            if (t.chaveUrl.equals(chave)) return t;
        }
        throw new IllegalArgumentException("Tipo de notificação desconhecido: " + chave);
    }

    public static boolean existe(String chave) {
        for (TipoNotificacao t : values()) {
            if (t.chaveUrl.equals(chave)) return true;
        }
        return false;
    }

    public String getChaveUrl() { return chaveUrl; }
    public String getTitulo() { return titulo; }
    public String getSubtitulo() { return subtitulo; }
    public String getCodigoFormulario() { return codigoFormulario; }
    public String getPrefixoProtocolo() { return prefixoProtocolo; }
    public String getClasseCss() { return classeCss; }
    public String getIcone() { return icone; }
    public boolean isIconePreenchido() { return iconePreenchido; }
    public String getCor() { return cor; }
    public String getCorEscura() { return corEscura; }
    public String getCorSuave() { return corSuave; }
    public String getCorCabecalho() { return corCabecalho; }
    public String getCorAnel() { return corAnel; }
    public List<CampoDinamicoDef> getCamposDinamicos() { return camposDinamicos; }

    public boolean isAcessoProfissional() {
        return this != NOTIFICACAO_PACIENTE;
    }
}
