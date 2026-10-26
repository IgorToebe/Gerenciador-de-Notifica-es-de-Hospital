package br.com.hospital.notificacao.integracao;

import br.com.hospital.notificacao.config.SchemaInitializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SchemaInitializerIT extends IntegracaoBase {

    @Test
    void criaAsOitoTabelasEAsColunasDaV11() {
        for (String tabela : new String[]{"USUARIO", "NOTIFICACAO", "NOTIFICACAO_CAMPO", "NOTIFICACAO_ANEXO",
                "NOTIFICACAO_HISTORICO", "INVESTIGACAO", "INVESTIGACAO_MARCO", "PLANO_ACAO"}) {
            assertEquals(0, contar("SELECT COUNT(*) FROM " + tabela), tabela);
        }
        assertEquals(0, contar("SELECT COUNT(*) FROM NOTIFICACAO WHERE SUBGRUPO IS NOT NULL OR DESCRICAO_REVISADA IS NOT NULL"));
    }

    @Test
    void migraOSubgrupoDasNotificacoesDaV1() {
        executar("INSERT INTO NOTIFICACAO (PROTOCOLO, TIPO, SENHA_HASH, SENHA_SALT, STATUS) "
                + "VALUES ('#PA-2026-000001', 'PROBLEMAS_ASSISTENCIAIS', 'h', 's', 'ABERTO')");
        executar("INSERT INTO NOTIFICACAO (PROTOCOLO, TIPO, SENHA_HASH, SENHA_SALT, STATUS) "
                + "VALUES ('#TC-2026-000002', 'TECNOVIGILANCIA', 'h', 's', 'ABERTO')");
        long assist = idPorProtocolo("#PA-2026-000001");
        long tecno = idPorProtocolo("#TC-2026-000002");
        executar("INSERT INTO NOTIFICACAO_CAMPO (ID_NOTIFICACAO, CHAVE, ROTULO, VALOR, ORDEM) "
                + "VALUES (?, 'tipoIncidente', 'Tipo de incidente', 'Queda', 0)", assist);
        executar("INSERT INTO NOTIFICACAO_CAMPO (ID_NOTIFICACAO, CHAVE, ROTULO, VALOR, ORDEM) "
                + "VALUES (?, 'produto', 'Produto', 'Bomba', 0)", tecno);

        SchemaInitializer.migrarDados();
        SchemaInitializer.migrarDados(); // idempotente

        assertEquals("Queda", valor("SELECT SUBGRUPO FROM NOTIFICACAO WHERE ID = ?", assist));
        assertEquals(null, valor("SELECT SUBGRUPO FROM NOTIFICACAO WHERE ID = ?", tecno),
                "tipos sem campo legado continuam sem subgrupo (a triagem pede para definir)");
    }

    @Test
    void migracaoNaoSobrescreveSubgrupoJaDefinido() {
        executar("INSERT INTO NOTIFICACAO (PROTOCOLO, TIPO, SENHA_HASH, SENHA_SALT, STATUS, SUBGRUPO) "
                + "VALUES ('#PA-2026-000003', 'PROBLEMAS_ASSISTENCIAIS', 'h', 's', 'ABERTO', 'Flebite')");
        long id = idPorProtocolo("#PA-2026-000003");
        executar("INSERT INTO NOTIFICACAO_CAMPO (ID_NOTIFICACAO, CHAVE, ROTULO, VALOR, ORDEM) "
                + "VALUES (?, 'tipoIncidente', 'Tipo de incidente', 'Queda', 0)", id);

        SchemaInitializer.migrarDados();

        assertEquals("Flebite", valor("SELECT SUBGRUPO FROM NOTIFICACAO WHERE ID = ?", id));
    }

    /** O DDL roda em todo startup do container; so' o DB2 real devolve os SQLSTATEs tratados. */
    @Test
    void executarDuasVezesNoDb2NaoFalha() {
        assumeTrue(usandoDb2Real(), "só faz sentido contra o DB2 real (-Dnsp.teste.jdbcUrl)");
        assertDoesNotThrow(SchemaInitializer::executar);
    }
}
