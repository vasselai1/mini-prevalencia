package testes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import br.tec.mboi.api.MiniPrevalencia;
import br.tec.mboi.api.MiniPrevalencia.Configurador;
import br.tec.mboi.api.MiniPrevalencia.ExecucaoTransacaoException;
import testes.consultas.exemplo.ObterPessoaPorId;
import testes.entidades.exemplo.ExemploModelo;
import testes.entidades.exemplo.Pessoa;
import testes.exceptions.ValidacaoCampoException;
import testes.transacoes.exemplo.AdicionarPessoa;
import testes.transacoes.exemplo.AlterarNomePessoaAdicionandoApelidoErro;
import testes.transacoes.exemplo.PessoaVO;

class TesteAtomicidade {

	private static final String DIRETORIO = System.getProperty("user.home") + "/ExemploModelo_Transacao";
	
	@BeforeAll	
	static void inicializarDiretorioEconfigurador() throws IOException {
		if (new File(DIRETORIO + "/" + ExemploModelo.class.getCanonicalName()).exists()) {
			Files.walk(Paths.get(DIRETORIO + "/" + ExemploModelo.class.getCanonicalName())).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
		Configurador conf = new Configurador() {
			public boolean isPrimario() {return true;}
			public String getDiretorioGravacao() {return DIRETORIO;}
			public boolean isRetornoProjegidoPorCopia() {return true;}
			public Integer getSegundosInatividadeParaIniciarGravacaoAcelerador() {return null;}
			public boolean isApagarTransacoesInternalizadasPeloAcelerador() {return false;}
		};
		MiniPrevalencia.setConfigurador(conf);		
	}	
	
	@Test()	
	void testAtomicidadePessoa() throws ValidacaoCampoException, ExecucaoTransacaoException {
		MiniPrevalencia<ExemploModelo> prevalencia = MiniPrevalencia.prevalecer(ExemploModelo.class);
		
		Long id = prevalencia.obterProximoIdSequencial(Pessoa.class);
		String nomeInicial = "Ricardo";
		String nomeAtualizacao = "Vasselai";
		
		PessoaVO pessoaVO = new PessoaVO();
		pessoaVO.setDataNascimento(new Date());
		pessoaVO.setEmail("emai1ricardo@teste.br");
		pessoaVO.setNome(nomeInicial);
		pessoaVO.setId(id);
		AdicionarPessoa transacaoAddPessoa = new AdicionarPessoa(pessoaVO);
		prevalencia.executar(transacaoAddPessoa);
				
		AlterarNomePessoaAdicionandoApelidoErro transacaoAlterarNome = new AlterarNomePessoaAdicionandoApelidoErro(id, nomeAtualizacao, "Mestre");		
		assertThrows(ExecucaoTransacaoException.class,() -> {
			prevalencia.executar(transacaoAlterarNome);//Nullpointer recebido dentro de ExecucaoTransacaoException  
		});
		
		Pessoa pessoa = prevalencia.executar(new ObterPessoaPorId(id));//Pessoa no estado anterior a executação da transação com erro...		
		assertTrue(pessoa.getNome().equals(nomeInicial));
		assertFalse(pessoa.getNome().equals(nomeAtualizacao));
		assertNull(pessoa.getApelidos());
	}	
	
}
