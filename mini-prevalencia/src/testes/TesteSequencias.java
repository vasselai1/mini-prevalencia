package testes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import br.tec.mboi.api.MiniPrevalencia;
import testes.entidades.exemplo.ExemploModelo;
import testes.entidades.exemplo.Pessoa;

class TesteSequencias {

	private static final String DIRETORIO = System.getProperty("user.home") + "/ExemploModelo_Transacao";
	
	static {
		MiniPrevalencia.configurar(DIRETORIO);
	} 	

	@BeforeAll	
	static void inicializarDiretorioEconfigurador() throws IOException {
		if (new File(DIRETORIO + "/" + ExemploModelo.class.getCanonicalName()).exists()) {
			Files.walk(Paths.get(DIRETORIO + "/" + ExemploModelo.class.getCanonicalName())).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
	}
	
	@Test
	void test() {
		MiniPrevalencia<ExemploModelo> prevalencia = MiniPrevalencia.prevalecer(ExemploModelo.class);
		
		prevalencia.obterProximoIdSequencial(ExemploModelo.class);
		prevalencia.obterProximoIdSequencial(ExemploModelo.class);
		prevalencia.obterProximoIdSequencial(ExemploModelo.class);		
		assertEquals(4, prevalencia.obterProximoIdSequencial(ExemploModelo.class));
		
		prevalencia.obterProximoIdSequencial(Pessoa.class);
		prevalencia.obterProximoIdSequencial(Pessoa.class);
		assertEquals(3, prevalencia.obterProximoIdSequencial(Pessoa.class));
	}

}
