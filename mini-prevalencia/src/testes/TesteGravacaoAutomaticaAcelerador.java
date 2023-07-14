package testes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import br.tec.mboi.api.MiniPrevalencia;
import br.tec.mboi.api.MiniPrevalencia.ExecucaoTransacaoException;
import testes.entidades.exemplo.ExemploModelo;
import testes.entidades.exemplo.Pessoa;
import testes.exceptions.ValidacaoCampoException;
import testes.transacoes.exemplo.AdicionarPessoa;
import testes.transacoes.exemplo.AtribuirEstoque;
import testes.transacoes.exemplo.PessoaVO;

@TestMethodOrder(OrderAnnotation.class)
class TesteGravacaoAutomaticaAcelerador {
	
	private static final String DIRETORIO = System.getProperty("user.home") + "/ExemploModelo_Transacao";
	
	static {
		MiniPrevalencia.configurar(DIRETORIO);
	} 	
	
	@BeforeAll	
	static void limparDiretorio() throws IOException {
		if (new File(DIRETORIO + "/" + ExemploModelo.class.getCanonicalName()).exists()) {
			Files.walk(Paths.get(DIRETORIO + "/" + ExemploModelo.class.getCanonicalName())).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}	
	}
	
	@Test
	@Order(1)
	void testInicial() throws ValidacaoCampoException, ExecucaoTransacaoException, InterruptedException {
		MiniPrevalencia<ExemploModelo> prevalencia = MiniPrevalencia.prevalecer(ExemploModelo.class);
		
		PessoaVO pessoaVO = new PessoaVO();
		pessoaVO.setDataNascimento(new Date());
		pessoaVO.setEmail("emai1ricardo@teste.br");
		pessoaVO.setNome("Ricardo");
		pessoaVO.setId(prevalencia.obterProximoIdSequencial(Pessoa.class));		
		
		prevalencia.executar(new AdicionarPessoa(pessoaVO));		
		prevalencia.executar(new AtribuirEstoque(100));
		assertEquals(0, prevalencia.getIdentificadorUltimaTransacaoAcelerada());
		assertEquals(2, prevalencia.getIdentificadorUltimaTransacaoExecutada());
		
		Thread.sleep(12000);
		assertEquals(2, prevalencia.getIdentificadorUltimaTransacaoAcelerada());
		assertEquals(2, prevalencia.getIdentificadorUltimaTransacaoExecutada());
	}

	@Test
	@Order(2)
	void testCarregamentoAcelerado() throws ValidacaoCampoException, ExecucaoTransacaoException, InterruptedException {
		MiniPrevalencia<ExemploModelo> prevalencia = MiniPrevalencia.prevalecer(ExemploModelo.class);
		assertEquals(2, prevalencia.getIdentificadorUltimaTransacaoAcelerada());
		
		PessoaVO pessoaVO = new PessoaVO();
		pessoaVO.setDataNascimento(new Date());
		pessoaVO.setEmail("emai1joao@teste.br");
		pessoaVO.setNome("João");
		pessoaVO.setId(prevalencia.obterProximoIdSequencial(Pessoa.class));		
		
		prevalencia.executar(new AdicionarPessoa(pessoaVO));		
		prevalencia.executar(new AtribuirEstoque(200));
		assertEquals(4, prevalencia.getIdentificadorUltimaTransacaoExecutada());
		assertEquals(2, prevalencia.getIdentificadorUltimaTransacaoAcelerada());
				
		Thread.sleep(12000);
		assertEquals(4, prevalencia.getIdentificadorUltimaTransacaoAcelerada());
		assertEquals(4, prevalencia.getIdentificadorUltimaTransacaoExecutada());
	}
	
}