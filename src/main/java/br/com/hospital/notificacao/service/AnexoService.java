package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.dao.AnexoDAO;
import br.com.hospital.notificacao.model.Anexo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia de anexos (fotos/documentos) da notificacao: arquivo fisico gravado num
 * volume Docker (ANEXOS_DIR), metadados no DB2 (NOTIFICACAO_ANEXO). O nome fisico e' um
 * UUID para nao expor o nome original do arquivo no sistema de arquivos.
 */
public class AnexoService {

    public static final long TAMANHO_MAXIMO_BYTES = 10L * 1024 * 1024; // 10 MB, conforme criterios de aceitacao
    private static final List<String> TIPOS_PERMITIDOS = Arrays.asList("image/jpeg", "image/png", "application/pdf");

    private final AnexoDAO anexoDAO = new AnexoDAO();

    private Path diretorioAnexos() {
        String dir = System.getenv("ANEXOS_DIR");
        if (dir == null || dir.isEmpty()) {
            dir = System.getProperty("java.io.tmpdir") + "/nsp-anexos";
        }
        Path path = Paths.get(dir);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível preparar o diretório de anexos: " + dir, e);
        }
        return path;
    }

    public boolean tipoPermitido(String mime) {
        return mime != null && TIPOS_PERMITIDOS.contains(mime.toLowerCase());
    }

    /** Grava o arquivo fisico e o metadado, dentro da mesma transacao da notificacao. */
    public void salvar(Connection con, long idNotificacao, String nomeOriginal, String mime, byte[] conteudo)
            throws SQLException {
        if (conteudo.length > TAMANHO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("Arquivo excede o limite de 10 MB");
        }
        if (!tipoPermitido(mime)) {
            throw new IllegalArgumentException("Tipo de arquivo não permitido: " + mime);
        }
        String extensao = extensaoDe(nomeOriginal);
        String nomeFisico = UUID.randomUUID() + (extensao.isEmpty() ? "" : "." + extensao);
        Path destino = diretorioAnexos().resolve(nomeFisico);
        try {
            Files.write(destino, conteudo);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao gravar arquivo de anexo em " + destino, e);
        }

        Anexo a = new Anexo();
        a.setIdNotificacao(idNotificacao);
        a.setNomeOriginal(nomeOriginal);
        a.setNomeFisico(nomeFisico);
        a.setTipoArquivo(mime);
        a.setTamanhoBytes(conteudo.length);
        anexoDAO.inserir(con, a);
    }

    public Optional<byte[]> lerConteudo(String nomeFisico) {
        Path arquivo = diretorioAnexos().resolve(nomeFisico);
        if (!Files.exists(arquivo)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(arquivo));
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler arquivo de anexo " + arquivo, e);
        }
    }

    public AnexoDAO getAnexoDAO() {
        return anexoDAO;
    }

    private String extensaoDe(String nomeOriginal) {
        if (nomeOriginal == null) return "";
        int idx = nomeOriginal.lastIndexOf('.');
        if (idx < 0 || idx == nomeOriginal.length() - 1) return "";
        return nomeOriginal.substring(idx + 1).replaceAll("[^a-zA-Z0-9]", "");
    }

    public static byte[] lerStream(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int lidos;
        while ((lidos = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, lidos);
        }
        return buffer.toByteArray();
    }
}
