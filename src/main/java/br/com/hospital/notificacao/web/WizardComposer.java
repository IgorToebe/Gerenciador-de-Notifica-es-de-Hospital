package br.com.hospital.notificacao.web;

import br.com.hospital.notificacao.model.CampoDinamicoDef;
import br.com.hospital.notificacao.model.TipoNotificacao;
import br.com.hospital.notificacao.service.AnexoService;
import br.com.hospital.notificacao.service.AnexoUpload;
import br.com.hospital.notificacao.service.NotificacaoService;
import br.com.hospital.notificacao.service.NovaNotificacaoRequest;
import br.com.hospital.notificacao.service.ResultadoNotificacao;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wizard de 4 passos para criacao de uma notificacao (nucleo obrigatorio da V1):
 * sigilo/LGPD -> dados do incidente (com campos dinamicos por tipo) -> descricao/anexos
 * -> contato opcional. Porta o fluxo finish()/applyType() do prototipo funcional para
 * um Composer ZK com estado no servidor (sem sessao, tudo vive no ciclo de vida da pagina).
 */
public class WizardComposer extends SelectorComposer<Component> {

    @Wire private Div shell;
    @Wire private Vlayout passo1;
    @Wire private Vlayout passo2;
    @Wire private Vlayout passo3;
    @Wire private Vlayout passo4;
    @Wire private Vlayout passo5;
    @Wire private Label lblSubPasso2;
    @Wire private Textbox txtData;
    @Wire private Textbox txtHora;
    @Wire private Combobox cboSetor;
    @Wire private Div camposDinamicos;
    @Wire private Combobox cboGravidade;
    @Wire private Button btnDanoSim;
    @Wire private Button btnDanoNao;
    @Wire private Textbox txtProntuario;
    @Wire private Textbox txtDescricao;
    @Wire private Button btnAnexar;
    @Wire private Label lblAnexos;
    @Wire private Textbox txtEmail;
    @Wire private Textbox txtTelefone;
    @Wire private Component linhaContato;
    @Wire private Label lblProtocolo;
    @Wire private Label lblSenha;
    @Wire private Label lblContato;
    @Wire private Label lblDica;

    private final NotificacaoService notificacaoService = new NotificacaoService();
    private final AnexoService anexoService = new AnexoService();

    private TipoNotificacao tipo;
    private boolean gerouDano = false;
    private final Map<String, Component> componentesDinamicos = new LinkedHashMap<>();
    private final Map<String, String> valoresToggleDinamico = new LinkedHashMap<>();
    private final List<AnexoUpload> anexosPendentes = new ArrayList<>();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        String chaveTipo = Executions.getCurrent().getParameter("tipo");
        if (chaveTipo == null || !TipoNotificacao.existe(chaveTipo)) {
            Executions.getCurrent().sendRedirect("/index.zul");
            return;
        }
        tipo = TipoNotificacao.porChaveUrl(chaveTipo);

        shell.setSclass("nsp-phone " + tipo.getClasseCss());
        lblSubPasso2.setValue(tipo.getCodigoFormulario() + " · " + tipo.getTitulo());
        btnDanoNao.setSclass("sel-on-toggle");

        for (CampoDinamicoDef def : tipo.getCamposDinamicos()) {
            camposDinamicos.appendChild(construirRotulo(def));
            camposDinamicos.appendChild(construirCampo(def));
        }
    }

    private Label construirRotulo(CampoDinamicoDef def) {
        Label rotulo = new Label(def.getRotulo() + (def.isObrigatorio() ? " *" : ""));
        rotulo.setSclass("nsp-field-label");
        rotulo.setStyle("margin-top:10px;display:block");
        return rotulo;
    }

    private Component construirCampo(CampoDinamicoDef def) {
        switch (def.getTipo()) {
            case SELECT: {
                Combobox cb = new Combobox();
                cb.setReadonly(true);
                cb.setWidth("100%");
                for (String opcao : def.getOpcoes()) {
                    Comboitem item = new Comboitem(opcao);
                    cb.appendChild(item);
                }
                componentesDinamicos.put(def.getChave(), cb);
                return cb;
            }
            case TOGGLE: {
                Div wrapper = new Div();
                wrapper.setSclass("nsp-btnrow");
                List<String> opcoes = def.getOpcoes();
                valoresToggleDinamico.put(def.getChave(), opcoes.get(0));
                for (String opcao : opcoes) {
                    Button b = new Button(opcao);
                    if (opcao.equals(opcoes.get(0))) {
                        b.setSclass("sel-on-toggle");
                    }
                    b.addEventListener("onClick", event -> {
                        for (Component irmao : wrapper.getChildren()) {
                            ((Button) irmao).setSclass("");
                        }
                        b.setSclass("sel-on-toggle");
                        valoresToggleDinamico.put(def.getChave(), opcao);
                    });
                    wrapper.appendChild(b);
                }
                componentesDinamicos.put(def.getChave(), wrapper);
                return wrapper;
            }
            case TEXTO:
            default: {
                Textbox tb = new Textbox();
                tb.setWidth("100%");
                componentesDinamicos.put(def.getChave(), tb);
                return tb;
            }
        }
    }

    private String valorDoCampoDinamico(CampoDinamicoDef def) {
        Component c = componentesDinamicos.get(def.getChave());
        if (c instanceof Textbox) {
            return ((Textbox) c).getValue();
        }
        if (c instanceof Combobox) {
            Comboitem sel = ((Combobox) c).getSelectedItem();
            return sel != null ? sel.getLabel() : "";
        }
        return valoresToggleDinamico.getOrDefault(def.getChave(), "");
    }

    // ---- Passo 1 ----
    @Listen("onClick = #btnVoltar1")
    public void voltar1() {
        Executions.getCurrent().sendRedirect("/index.zul");
    }

    @Listen("onClick = #btnProsseguir1")
    public void prosseguir1() {
        trocarPasso(passo1, passo2);
    }

    // ---- Passo 2 ----
    @Listen("onClick = #btnVoltar2")
    public void voltar2() {
        trocarPasso(passo2, passo1);
    }

    @Listen("onClick = #btnDanoSim")
    public void danoSim() {
        gerouDano = true;
        btnDanoSim.setSclass("sel-on-toggle");
        btnDanoNao.setSclass("");
    }

    @Listen("onClick = #btnDanoNao")
    public void danoNao() {
        gerouDano = false;
        btnDanoNao.setSclass("sel-on-toggle");
        btnDanoSim.setSclass("");
    }

    @Listen("onClick = #btnProsseguir2")
    public void prosseguir2() {
        if (!validarObrigatorio(txtData, "Informe a data do incidente")) return;
        if (cboSetor.getSelectedItem() == null) {
            Clients.wrongValue(cboSetor, "Selecione o setor / unidade");
            return;
        }
        if (cboGravidade.getSelectedItem() == null) {
            Clients.wrongValue(cboGravidade, "Selecione a pré-classificação da gravidade");
            return;
        }
        for (CampoDinamicoDef def : tipo.getCamposDinamicos()) {
            if (def.isObrigatorio() && (valorDoCampoDinamico(def) == null || valorDoCampoDinamico(def).trim().isEmpty())) {
                Clients.showNotification("Preencha o campo obrigatório: " + def.getRotulo(), "error",
                        componentesDinamicos.get(def.getChave()), "before_center", 3000);
                return;
            }
        }
        trocarPasso(passo2, passo3);
    }

    // ---- Passo 3 ----
    @Listen("onClick = #btnVoltar3")
    public void voltar3() {
        trocarPasso(passo3, passo2);
    }

    @Listen("onUpload = #btnAnexar")
    public void anexar(UploadEvent event) throws Exception {
        Media[] medias = event.getMedias();
        if (medias == null) {
            Media m = event.getMedia();
            medias = m != null ? new Media[]{m} : new Media[0];
        }
        int aceitos = 0;
        for (Media media : medias) {
            if (media == null) continue;
            if (!anexoService.tipoPermitido(media.getContentType())) {
                Clients.showNotification("Tipo de arquivo não permitido: " + media.getName(), "error", btnAnexar,
                        "before_center", 3000);
                continue;
            }
            byte[] conteudo = media.isBinary() ? media.getByteData()
                    : AnexoService.lerStream(media.getStreamData());
            if (conteudo.length > AnexoService.TAMANHO_MAXIMO_BYTES) {
                Clients.showNotification("Arquivo maior que 10 MB: " + media.getName(), "error", btnAnexar,
                        "before_center", 3000);
                continue;
            }
            anexosPendentes.add(new AnexoUpload(media.getName(), media.getContentType(), conteudo));
            aceitos++;
        }
        if (aceitos > 0) {
            lblAnexos.setValue("✓ " + anexosPendentes.size() + " arquivo(s) anexado(s)");
        }
    }

    @Listen("onClick = #btnProsseguir3")
    public void prosseguir3() {
        if (!validarObrigatorio(txtDescricao, "Descreva o que aconteceu")) return;
        trocarPasso(passo3, passo4);
    }

    // ---- Passo 4 / conclusao ----
    @Listen("onClick = #btnVoltar4")
    public void voltar4() {
        trocarPasso(passo4, passo3);
    }

    @Listen("onClick = #btnConcluir")
    public void concluir() {
        NovaNotificacaoRequest req = new NovaNotificacaoRequest();
        req.setTipo(tipo);
        req.setDataIncidente(txtData.getValue());
        req.setHoraIncidente(txtHora.getValue());
        req.setSetor(cboSetor.getSelectedItem().getLabel());
        req.setProntuario(txtProntuario.getValue());
        req.setGravidade(cboGravidade.getSelectedItem().getLabel());
        req.setGerouDano(gerouDano);
        req.setDescricao(txtDescricao.getValue());
        req.setContatoEmail(txtEmail.getValue());
        req.setContatoTelefone(txtTelefone.getValue());
        for (CampoDinamicoDef def : tipo.getCamposDinamicos()) {
            req.getCamposDinamicos().put(def.getChave(), valorDoCampoDinamico(def));
        }
        req.getAnexos().addAll(anexosPendentes);

        ResultadoNotificacao resultado;
        try {
            resultado = notificacaoService.criar(req);
        } catch (RuntimeException e) {
            Clients.showNotification("Não foi possível registrar a notificação: " + e.getMessage(), "error",
                    null, "top_center", 4000);
            return;
        }

        lblProtocolo.setValue(resultado.getProtocolo());
        lblSenha.setValue(resultado.getSenha());
        if (resultado.isComContato()) {
            linhaContato.setVisible(true);
            lblContato.setValue(resultado.getContatoMascarado());
            lblDica.setValue("Contato salvo. A equipe poderá te procurar se necessário.");
        } else {
            linhaContato.setVisible(false);
            lblDica.setValue("Notificação anônima — guarde em local seguro.");
        }
        trocarPasso(passo4, passo5);
    }

    @Listen("onClick = #btnNovaNotificacao")
    public void novaNotificacao() {
        Executions.getCurrent().sendRedirect("/index.zul");
    }

    private void trocarPasso(Vlayout atual, Vlayout proximo) {
        atual.setVisible(false);
        proximo.setVisible(true);
    }

    private boolean validarObrigatorio(Textbox campo, String mensagem) {
        if (campo.getValue() == null || campo.getValue().trim().isEmpty()) {
            Clients.wrongValue(campo, mensagem);
            return false;
        }
        return true;
    }

}
