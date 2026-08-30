package br.com.hospital.notificacao.service;

import br.com.hospital.notificacao.model.NivelRisco;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatrizRiscoTest {

    @Test
    void scoreEhProbabilidadeVezesImpacto() {
        assertEquals(15, MatrizRisco.calcularScore(3, 5));
        assertEquals(80, MatrizRisco.calcularScore(5, 16));
    }

    @Test
    void faixasDeRiscoSeguemOPrototipoFuncional() {
        assertEquals(NivelRisco.BAIXO, NivelRisco.doScore(1));
        assertEquals(NivelRisco.BAIXO, NivelRisco.doScore(4));
        assertEquals(NivelRisco.MEDIO, NivelRisco.doScore(5));
        assertEquals(NivelRisco.MEDIO, NivelRisco.doScore(10));
        assertEquals(NivelRisco.ALTO, NivelRisco.doScore(11));
        assertEquals(NivelRisco.ALTO, NivelRisco.doScore(24));
        assertEquals(NivelRisco.EXTREMO, NivelRisco.doScore(25));
        assertEquals(NivelRisco.EXTREMO, NivelRisco.doScore(80));
    }

    @Test
    void calcularNivelCombinaScoreEFaixa() {
        assertEquals(NivelRisco.EXTREMO, MatrizRisco.calcularNivel(5, 16)); // quase certo x extremo
        assertEquals(NivelRisco.BAIXO, MatrizRisco.calcularNivel(1, 1));    // raro x muito baixo
    }
}
