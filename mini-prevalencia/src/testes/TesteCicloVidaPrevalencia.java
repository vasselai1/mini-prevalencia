package testes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.tec.mboi.api.MiniPrevalencia;
import testes.consultas.exemplo.ObterNomeExemploModelo;
import testes.entidades.exemplo.ExemploModelo;

class TesteCicloVidaPrevalencia {

	private static final String DIRETORIO = System.getProperty("user.home") + "/ExemploModelo_Transacao";
	
	static {
		MiniPrevalencia.configurar(DIRETORIO, false, null, false);
	} 
	
	@Test
	void testeMesmasInstancias() {
		MiniPrevalencia<ExemploModelo> prevalencia1 = MiniPrevalencia.prevalecer(ExemploModelo.class);
		MiniPrevalencia<ExemploModelo> prevalencia2 = MiniPrevalencia.prevalecer(ExemploModelo.class);
		assertEquals(prevalencia1, prevalencia2);
	}

	@Test
	void testeInstanciasDiferentes() {
		MiniPrevalencia<ExemploModelo> prevalencia1 = MiniPrevalencia.prevalecer(ExemploModelo.class);
		prevalencia1.finalizarPrevalencia();
		MiniPrevalencia<ExemploModelo> prevalencia2 = MiniPrevalencia.prevalecer(ExemploModelo.class);
		assertNotEquals(prevalencia1, prevalencia2);
	}	
	
	@Test
	void testeInoperanteAposFinalizarInstancia() {
		MiniPrevalencia<ExemploModelo> prevalencia = MiniPrevalencia.prevalecer(ExemploModelo.class);
		prevalencia.finalizarPrevalencia();
		assertThrows(IllegalStateException.class,() -> {
			prevalencia.executar(new ObterNomeExemploModelo());
		});
	}	
	
}
