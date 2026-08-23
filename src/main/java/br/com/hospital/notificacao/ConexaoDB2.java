package br.com.hospital.notificacao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexaoDB2 {

    static {
        try {
            Class.forName("com.ibm.db2.jcc.DB2Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static String env(String nome, String padrao) {
        String valor = System.getenv(nome);
        return valor != null ? valor : padrao;
    }

    public static Connection obterConexao() throws SQLException {
        String host = env("DB2_HOST", "localhost");
        String porta = env("DB2_PORT", "50000");
        String database = env("DB2_DATABASE", "HOSPITAL");
        String usuario = env("DB2_USER", "db2inst1");
        String senha = System.getenv("DB2_PASSWORD");
        if (senha == null) {
            throw new IllegalStateException("Variável de ambiente DB2_PASSWORD não definida");
        }

        String url = "jdbc:db2://" + host + ":" + porta + "/" + database;
        return DriverManager.getConnection(url, usuario, senha);
    }
}
