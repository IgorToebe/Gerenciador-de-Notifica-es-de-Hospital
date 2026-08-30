package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.model.TipoNotificacao;

import java.security.SecureRandom;
import java.time.Year;

/**
 * Gera o protocolo publico ("#TC-2026-004821") e a senha de acompanhamento de uma
 * notificacao, no mesmo formato do protototipo funcional (genProto/genSenha).
 */
public final class ProtocoloService {

    private static final String ALFABETO_SENHA = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#";
    private static final int TAMANHO_SENHA = 9;
    private static final SecureRandom RANDOM = new SecureRandom();

    private ProtocoloService() { }

    public static String gerarProtocolo(TipoNotificacao tipo, long sequencial) {
        int ano = Year.now().getValue();
        return "#" + tipo.getPrefixoProtocolo() + "-" + ano + "-" + String.format("%06d", sequencial);
    }

    public static String gerarSenha() {
        StringBuilder sb = new StringBuilder(TAMANHO_SENHA);
        for (int i = 0; i < TAMANHO_SENHA; i++) {
            sb.append(ALFABETO_SENHA.charAt(RANDOM.nextInt(ALFABETO_SENHA.length())));
        }
        return sb.toString();
    }
}
