package br.com.hospital.notificacao.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashSenhaTest {

    @Test
    void confereSenhaCorretaComOMesmoSalt() {
        String salt = HashSenha.gerarSalt();
        String hash = HashSenha.gerarHash("Minha$enha123", salt);
        assertTrue(HashSenha.conferir("Minha$enha123", salt, hash));
    }

    @Test
    void rejeitaSenhaIncorreta() {
        String salt = HashSenha.gerarSalt();
        String hash = HashSenha.gerarHash("Minha$enha123", salt);
        assertFalse(HashSenha.conferir("senhaErrada", salt, hash));
    }

    @Test
    void saltsDiferentesGeramHashesDiferentesParaAMesmaSenha() {
        String salt1 = HashSenha.gerarSalt();
        String salt2 = HashSenha.gerarSalt();
        assertNotEquals(salt1, salt2);
        assertNotEquals(HashSenha.gerarHash("mesma-senha", salt1), HashSenha.gerarHash("mesma-senha", salt2));
    }

    @Test
    void nuncaGuardaASenhaEmTextoPuroNoHash() {
        String salt = HashSenha.gerarSalt();
        String senha = "SenhaVisivel!";
        String hash = HashSenha.gerarHash(senha, salt);
        assertNotEquals(senha, hash);
        assertFalse(hash.contains(senha));
    }
}
