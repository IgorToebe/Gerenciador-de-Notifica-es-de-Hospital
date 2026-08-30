package br.com.hospital.notificacao.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hash de senhas (login administrativo e senha de acompanhamento da notificacao) via
 * SHA-256 com salt aleatorio por registro. Sem biblioteca externa de hashing para manter
 * a stack minima; suficiente para o escopo da V1 (nao ha reuso de senha entre sistemas).
 */
public final class HashSenha {

    private static final SecureRandom RANDOM = new SecureRandom();

    private HashSenha() { }

    public static String gerarSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String gerarHash(String senhaEmTexto, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Base64.getDecoder().decode(salt));
            byte[] hash = digest.digest(senhaEmTexto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponivel na JVM", e);
        }
    }

    public static boolean conferir(String senhaEmTexto, String salt, String hashEsperado) {
        if (senhaEmTexto == null || salt == null || hashEsperado == null) {
            return false;
        }
        String hashCalculado = gerarHash(senhaEmTexto, salt);
        return MessageDigest.isEqual(
                hashCalculado.getBytes(StandardCharsets.UTF_8),
                hashEsperado.getBytes(StandardCharsets.UTF_8));
    }
}
