package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.config.DataSourceFactory;
import br.com.hospital.notificacao.dao.AnexoDAO;
import br.com.hospital.notificacao.dao.CampoNotificacaoDAO;
import br.com.hospital.notificacao.dao.HistoricoDAO;
import br.com.hospital.notificacao.dao.NotificacaoDAO;
import br.com.hospital.notificacao.model.CampoDinamicoDef;
import br.com.hospital.notificacao.model.CampoNotificacao;
import br.com.hospital.notificacao.model.Notificacao;
import br.com.hospital.notificacao.model.StatusNotificacao;
import br.com.hospital.notificacao.model.TipoNotificacao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orquestra a criacao de uma notificacao (wizard de 4 passos) e as consultas usadas
 * pelo painel administrativo (lista, KPIs, acompanhamento publico).
 */
public class NotificacaoService {

    // Pre-classificacao de risco atribuida no momento da abertura, antes da triagem formal
    // (mesmo comportamento do prototipo funcional: prob=2, imp=4 -> risco Medio).
    private static final int PROB_PADRAO = 2;
    private static final int IMPACTO_PADRAO = 4;

    private final NotificacaoDAO notificacaoDAO = new NotificacaoDAO();
    private final CampoNotificacaoDAO campoDAO = new CampoNotificacaoDAO();
    private final AnexoDAO anexoDAO = new AnexoDAO();
    private final HistoricoDAO historicoDAO = new HistoricoDAO();
    private final AnexoService anexoService = new AnexoService();

    public ResultadoNotificacao criar(NovaNotificacaoRequest req) {
        if (req.getTipo() == null) {
            throw new IllegalArgumentException("Tipo de notificação é obrigatório");
        }

        long sequencial = notificacaoDAO.proximoSequencial();
        String protocolo = ProtocoloService.gerarProtocolo(req.getTipo(), sequencial);
        String senha = ProtocoloService.gerarSenha();
        String salt = HashSenha.gerarSalt();
        String senhaHash = HashSenha.gerarHash(senha, salt);

        Notificacao n = new Notificacao();
        n.setProtocolo(protocolo);
        n.setTipo(req.getTipo());
        n.setSenhaHash(senhaHash);
        n.setSenhaSalt(salt);
        n.setDataIncidente(req.getDataIncidente());
        n.setHoraIncidente(req.getHoraIncidente());
        n.setSetor(req.getSetor());
        n.setProntuario(req.getProntuario());
        n.setGravidade(req.getGravidade());
        n.setGerouDano(req.isGerouDano());
        n.setDescricao(req.getDescricao());
        boolean comContato = req.temContato();
        n.setAnonimo(!comContato);
        n.setContatoEmail(comContato ? req.getContatoEmail() : null);
        n.setContatoTelefone(comContato ? req.getContatoTelefone() : null);
        n.setStatus(StatusNotificacao.ABERTO);

        try (Connection con = DataSourceFactory.obterConexao()) {
            con.setAutoCommit(false);
            try {
                long id = notificacaoDAO.inserir(con, n);

                List<CampoNotificacao> campos = montarCamposDinamicos(req);
                if (!campos.isEmpty()) {
                    campoDAO.inserirTodos(con, id, campos);
                }

                for (AnexoUpload upload : req.getAnexos()) {
                    anexoService.salvar(con, id, upload.getNomeOriginal(), upload.getMime(), upload.getConteudo());
                }

                historicoDAO.inserir(con, id, null, StatusNotificacao.ABERTO, null,
                        "Notificação criada via QR Code (" + req.getTipo().getTitulo() + ")");

                // Pre-classificacao de risco padrao (a equipe de triagem reclassifica depois).
                notificacaoDAO.atualizarRisco(id, req.getGravidade(), PROB_PADRAO, IMPACTO_PADRAO,
                        MatrizRisco.calcularScore(PROB_PADRAO, IMPACTO_PADRAO),
                        MatrizRisco.calcularNivel(PROB_PADRAO, IMPACTO_PADRAO), StatusNotificacao.ABERTO);

                con.commit();
            } catch (RuntimeException | SQLException e) {
                con.rollback();
                throw (e instanceof RuntimeException) ? (RuntimeException) e
                        : new RuntimeException("Erro ao criar notificação", e);
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao abrir conexão para criar notificação", e);
        }

        String contatoMascarado = comContato ? mascarar(req.getContatoEmail(), req.getContatoTelefone()) : null;
        return new ResultadoNotificacao(protocolo, senha, comContato, contatoMascarado);
    }

    private List<CampoNotificacao> montarCamposDinamicos(NovaNotificacaoRequest req) {
        List<CampoNotificacao> resultado = new ArrayList<>();
        int ordem = 0;
        for (CampoDinamicoDef def : req.getTipo().getCamposDinamicos()) {
            String valor = req.getCamposDinamicos().get(def.getChave());
            if (valor != null && !valor.trim().isEmpty()) {
                resultado.add(new CampoNotificacao(def.getChave(), def.getRotulo(), valor.trim(), ordem++));
            }
        }
        return resultado;
    }

    private String mascarar(String email, String telefone) {
        if (email != null && !email.trim().isEmpty()) {
            String[] partes = email.split("@");
            String usuario = partes[0].length() > 2 ? partes[0].substring(0, 2) : partes[0];
            return usuario + "**@" + (partes.length > 1 ? partes[1] : "email.com");
        }
        if (telefone != null && telefone.length() >= 4) {
            return telefone.substring(0, 2) + "***" + telefone.substring(telefone.length() - 2);
        }
        return "";
    }

    public Optional<Notificacao> buscarParaAcompanhamento(String protocolo, String senha) {
        Optional<Notificacao> encontrada = notificacaoDAO.buscarPorProtocolo(protocolo.trim());
        if (!encontrada.isPresent()) {
            return Optional.empty();
        }
        Notificacao n = encontrada.get();
        if (senha != null && !senha.trim().isEmpty()
                && !HashSenha.conferir(senha.trim(), n.getSenhaSalt(), n.getSenhaHash())) {
            return Optional.empty();
        }
        return Optional.of(n);
    }

    public Notificacao carregarCompleta(long id) {
        Notificacao n = notificacaoDAO.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada: " + id));
        n.setCampos(campoDAO.listarPorNotificacao(id));
        n.setAnexos(anexoDAO.listarPorNotificacao(id));
        return n;
    }

    public NotificacaoDAO.ResultadoPagina listar(NotificacaoDAO.Filtro filtro) {
        return notificacaoDAO.listar(filtro);
    }

    public Map<String, Integer> contarDuplicatasPorProntuario() {
        return notificacaoDAO.contarDuplicatasPorProntuario();
    }

    public Map<StatusNotificacao, Integer> contarPorStatus() {
        return notificacaoDAO.contarPorStatus();
    }

    public Map<TipoNotificacao, Integer> contarPorTipo() {
        return notificacaoDAO.contarPorTipo();
    }

    public int contarCriadasHoje() {
        return notificacaoDAO.contarCriadasHoje();
    }

    public NotificacaoDAO getNotificacaoDAO() {
        return notificacaoDAO;
    }
}
