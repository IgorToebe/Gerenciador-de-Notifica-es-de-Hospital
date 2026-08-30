package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.dao.UsuarioDAO;
import br.com.hospital.notificacao.model.Usuario;

import java.util.Optional;

/** Autenticacao do painel administrativo (login institucional + senha com hash). */
public class AutenticacaoService {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public Optional<Usuario> autenticar(String login, String senha) {
        if (login == null || senha == null) {
            return Optional.empty();
        }
        Optional<Usuario> usuario = usuarioDAO.buscarPorLogin(login.trim());
        if (!usuario.isPresent() || !usuario.get().isAtivo()) {
            return Optional.empty();
        }
        Usuario u = usuario.get();
        if (!HashSenha.conferir(senha, u.getSalt(), u.getSenhaHash())) {
            return Optional.empty();
        }
        return Optional.of(u);
    }
}
