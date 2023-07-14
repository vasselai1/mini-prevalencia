package testes;

import java.util.Date;

import org.junit.jupiter.api.Test;

import br.tec.mboi.api.MiniPrevalencia;
import br.tec.mboi.api.MiniPrevalencia.ExecucaoTransacaoException;
import br.tec.mboi.api.MiniPrevalencia.GravacaoEmDiscoException;
import br.tec.mboi.api.MiniPrevalencia.LeituraEmDiscoException;
import testes.entidades.exemplo.ExemploModelo;
import testes.entidades.exemplo.Pessoa;
import testes.exceptions.ValidacaoCampoException;
import testes.transacoes.exemplo.AdicionarPessoa;
import testes.transacoes.exemplo.PessoaVO;

class TesteTransacoesComEscravoObservando {

	private static final String DIRETORIO = System.getProperty("user.home") + "/ExemploModelo_Transacao";
	
	static {
		MiniPrevalencia.configurar(DIRETORIO, false, 60, false);
	}	
	
	@Test
	void testAddPessoa() throws GravacaoEmDiscoException, LeituraEmDiscoException, ValidacaoCampoException, ExecucaoTransacaoException {
		MiniPrevalencia<ExemploModelo> prevalencia = MiniPrevalencia.prevalecer(ExemploModelo.class);

		Long id = prevalencia.obterProximoIdSequencial(Pessoa.class);
		PessoaVO pessoaVO = new PessoaVO();
		pessoaVO.setDataNascimento(new Date());
		pessoaVO.setEmail("emai1" + id + "ricardo@teste.br");
		pessoaVO.setNome("Ricardo");
		pessoaVO.setId(id);
		
		prevalencia.executar(new AdicionarPessoa(pessoaVO));//Executar com escravo (TesteEscravo.java) observando na linha de comando...
	}

}
