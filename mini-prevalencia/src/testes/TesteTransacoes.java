package testes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import br.tec.mboi.api.MiniPrevalencia;
import br.tec.mboi.api.MiniPrevalencia.Configurador;
import br.tec.mboi.api.MiniPrevalencia.ExecucaoTransacaoException;
import br.tec.mboi.api.MiniPrevalencia.GravacaoEmDiscoException;
import br.tec.mboi.api.MiniPrevalencia.Transacao;
import br.tec.mboi.api.MiniPrevalencia.TransacaoSemRetorno;
import testes.consultas.exemplo.ListarNomesPessoas;
import testes.consultas.exemplo.ObterNomeExemploModelo;
import testes.consultas.exemplo.ObterPessoaPorId;
import testes.entidades.exemplo.ExemploModelo;
import testes.entidades.exemplo.Pessoa;
import testes.exceptions.ValidacaoCampoException;
import testes.transacoes.exemplo.AdicionarPessoa;
import testes.transacoes.exemplo.AlterarNomePessoaAdicionandoApelidoErro;
import testes.transacoes.exemplo.NomearModeloExemplo;
import testes.transacoes.exemplo.PessoaVO;

@TestMethodOrder(OrderAnnotation.class)
class TesteTransacoes {
	
	public static class ObservadorTeste implements br.tec.mboi.api.MiniPrevalencia.Observador<ExemploModelo> {
		private int contador = 0;
		@Override
		public synchronized void receberAvisoExecucao(Transacao<ExemploModelo, ? extends Throwable> transacao) {
			//System.out.println(transacao);
			contador++;
		}
		public int getContador() {
			return contador;
		}
	}
	
	private static final String DIRETORIO = System.getProperty("user.home") + "/ExemploModelo_Transacao";
	private static final int QTD_ADD_PESSOAS = 10;
	private static ObservadorTeste observadorTeste = new ObservadorTeste();
	
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
	
	@AfterAll
	static void finalizarPrevalencia() {
		MiniPrevalencia.prevalecer(ExemploModelo.class).finalizarPrevalencia();
	}
	
	@Test()
	@Order(1)
	void testAtualizarNomeModelo() throws ValidacaoCampoException, ExecucaoTransacaoException {
		MiniPrevalencia<ExemploModelo> prevalencia = MiniPrevalencia.prevalecer(ExemploModelo.class);
		prevalencia.registrarObservador(observadorTeste);
		assertEquals(0L, prevalencia.getIdentificadorUltimaTransacaoExecutada(), "A base não está zerada");
		
		NomearModeloExemplo transacaoNomearModelo = new NomearModeloExemplo("Primeiro Teste");		
		prevalencia.executar(transacaoNomearModelo);
		assertEquals(1L, prevalencia.getIdentificadorUltimaTransacaoExecutada(), "A base não está contando corretamente as transações executadas");		
		
		ObterNomeExemploModelo consultaNomeModelo = new ObterNomeExemploModelo();
		String nome = prevalencia.executar(consultaNomeModelo);
		assertEquals("Primeiro Teste", nome);
		assertEquals(1L, prevalencia.getIdentificadorUltimaTransacaoExecutada(), "A base não está contando corretamente as transações executadas");
	}

	@Test()
	@Order(2)
	void testValidarNomeModelo() {		
		MiniPrevalencia<ExemploModelo> prevalencia = MiniPrevalencia.prevalecer(ExemploModelo.class);
		prevalencia.registrarObservador(observadorTeste);
		NomearModeloExemplo transacaoNomearModelo = new NomearModeloExemplo("Teste Validação");
		assertThrows(ValidacaoCampoException.class,() -> {
			prevalencia.executar(transacaoNomearModelo);
		});
		assertEquals(1L, prevalencia.getIdentificadorUltimaTransacaoExecutada(), "A base não está contando corretamente as transações executadas");
	}
	
	@Test()
	@Order(3)
	void testAtomicidadePessoa() throws ValidacaoCampoException, ExecucaoTransacaoException {
		MiniPrevalencia<ExemploModelo> prevalencia = MiniPrevalencia.prevalecer(ExemploModelo.class);
		prevalencia.registrarObservador(observadorTeste);
		Long id = prevalencia.obterProximoIdSequencial(Pessoa.class);
		
		PessoaVO pessoaVO = new PessoaVO();
		pessoaVO.setDataNascimento(new Date());
		pessoaVO.setEmail("emai1ricardo@teste.br");
		pessoaVO.setNome("Ricardo");
		pessoaVO.setId(id);
		AdicionarPessoa transacaoAddPessoa = new AdicionarPessoa(pessoaVO);
		prevalencia.executar(transacaoAddPessoa);
		assertEquals(2L, prevalencia.getIdentificadorUltimaTransacaoExecutada(), "A base não está contando corretamente as transações executadas");
				
		AlterarNomePessoaAdicionandoApelidoErro transacaoAlterarNome = new AlterarNomePessoaAdicionandoApelidoErro(id, "Vasselai", "Mestre");		
		ExecucaoTransacaoException ex = assertThrows(ExecucaoTransacaoException.class,() -> {
			prevalencia.executar(transacaoAlterarNome);
		});
		assertTrue(ex.getMessage().contains("Mestre"));
		assertEquals(2L, prevalencia.getIdentificadorUltimaTransacaoExecutada(), "A base não está contando corretamente as transações executadas");		
		
		Pessoa pessoa = prevalencia.executar(new ObterPessoaPorId(id));
		
		assertEquals(2L, prevalencia.getIdentificadorUltimaTransacaoExecutada(), "A base não está contando corretamente as transações executadas");
		assertEquals(pessoaVO.getNome(), pessoa.getNome());
		assertFalse(pessoa.getNome().equals("Vasselai"));
		assertNull(pessoa.getApelidos());
	}	
	
	
	@Test()
	@Order(4)
	void testAdicionarPessoas() throws ValidacaoCampoException, ExecucaoTransacaoException {
		MiniPrevalencia<ExemploModelo> prevalencia = MiniPrevalencia.prevalecer(ExemploModelo.class);
		prevalencia.registrarObservador(observadorTeste);
		
		List<Long> ids = new ArrayList<Long>();
		for (int id = 0; id < QTD_ADD_PESSOAS; id++) {
			ids.add(prevalencia.obterProximoIdSequencial(Pessoa.class));
		}
		
		long tempoInicial = System.currentTimeMillis();		
		for (int indice = 0; indice < QTD_ADD_PESSOAS; indice++) {
			Long id = ids.get(indice);
			PessoaVO pessoaVO = new PessoaVO();
			pessoaVO.setDataNascimento(new Date());
			pessoaVO.setEmail("emai1" + id + "@teste.br");
			pessoaVO.setNome("Nome teste " + id);
			pessoaVO.setId(id);
			AdicionarPessoa addPessoa = new AdicionarPessoa(pessoaVO);
			prevalencia.executar(addPessoa);
		}
		long tempoDecorrido = System.currentTimeMillis() - tempoInicial;
		if (tempoDecorrido < 1000) {
			tempoDecorrido = 1000;
		}
		
		float transacoesPorSegundo = QTD_ADD_PESSOAS / (tempoDecorrido / 1000);
		System.out.println(transacoesPorSegundo);
		
		assertEquals(2L + QTD_ADD_PESSOAS, prevalencia.getIdentificadorUltimaTransacaoExecutada(), "A base não está contando corretamente as transações executadas");
		assertTrue("Menos de 2000 transacoes por segundo (" + transacoesPorSegundo + ")", transacoesPorSegundo  >= 2000);//Computador ruim se falhou
	}	

	@Test()
	@Order(5)
	void testTempoInicializacao() throws GravacaoEmDiscoException {
		MiniPrevalencia<ExemploModelo> prevalencia0 = MiniPrevalencia.prevalecer(ExemploModelo.class);
		prevalencia0.finalizarPrevalencia();
		
		long tempoInicialSemAcelerador = System.currentTimeMillis();
		MiniPrevalencia<ExemploModelo> prevalencia1 = MiniPrevalencia.prevalecer(ExemploModelo.class);
		long tempoDecorridoSemAcelerador = System.currentTimeMillis() - tempoInicialSemAcelerador;
		
		assertEquals(0L, prevalencia1.getIdentificadorUltimaTransacaoAcelerada(), "Existe um arquivo acelerador");
		prevalencia1.atualizarArquivoAceleradorInicializacao();
		assertEquals(2L + QTD_ADD_PESSOAS, prevalencia1.getIdentificadorUltimaTransacaoAcelerada(), "Arquivo acelerador desatualizado");
		prevalencia1.finalizarPrevalencia();
		
		long tempoInicialComAcelerador = System.currentTimeMillis();
		MiniPrevalencia<ExemploModelo> prevalencia2 = MiniPrevalencia.prevalecer(ExemploModelo.class);
		long tempoDecorridoComAcelerador = System.currentTimeMillis() - tempoInicialComAcelerador;
		assertEquals(2L + QTD_ADD_PESSOAS, prevalencia2.getIdentificadorUltimaTransacaoAcelerada(), "Arquivo acelerador desatualizado");
		prevalencia2.finalizarPrevalencia();
		
		assertTrue("O acelerador não ajudou, sem: " + tempoDecorridoSemAcelerador + "ms com: " + tempoDecorridoComAcelerador + "ms", tempoDecorridoComAcelerador < tempoInicialSemAcelerador);
	}
	
	@Test()
	@Order(6)	
	void testListarNomesPessoas() throws ValidacaoCampoException {
		MiniPrevalencia<ExemploModelo> prevalencia0 = MiniPrevalencia.prevalecer(ExemploModelo.class);
		prevalencia0.registrarObservador(observadorTeste);
		prevalencia0.finalizarPrevalencia();
		assertThrows(IllegalStateException.class,() -> {
			prevalencia0.executar(new ListarNomesPessoas());
		});
		
		MiniPrevalencia<ExemploModelo> prevalencia = MiniPrevalencia.prevalecer(ExemploModelo.class);
		prevalencia.registrarObservador(observadorTeste);
		List<String> nomesPessoas = prevalencia.executar(new ListarNomesPessoas());
		assertTrue(nomesPessoas.size() > QTD_ADD_PESSOAS);
		assertEquals(2L + QTD_ADD_PESSOAS, prevalencia.getIdentificadorUltimaTransacaoExecutada(), "A base não está contando corretamente as transações executadas");
		assertEquals(2L + QTD_ADD_PESSOAS, prevalencia.getIdentificadorUltimaTransacaoAcelerada(), "Arquivo acelerador desatualizado");
		prevalencia.finalizarPrevalencia();
	}	
	
	@Test()
	@Order(7)	
	void testTransacaoAnonimaPessoas() throws Exception {
		MiniPrevalencia<ExemploModelo> prevalencia = MiniPrevalencia.prevalecer(ExemploModelo.class);
		prevalencia.registrarObservador(observadorTeste);
		
		//não da para serializar...
		TransacaoSemRetorno<ExemploModelo, Exception> trs = new TransacaoSemRetorno<ExemploModelo, Exception>() {
			private static final long serialVersionUID = 1L;
			private String nome = null;
			public TransacaoSemRetorno<ExemploModelo, Exception> setNome(String nome) {
				this.nome = nome;
				return this;
			}
			@Override
			public void validar(ExemploModelo pojoUnico) throws Exception {
				// TODO Auto-generated method stub
				
			}				
			@Override
			public void executar(ExemploModelo pojoUnico) {
				pojoUnico.setNome(nome);
				
			}
		}.setNome("teste");
		
		assertThrows(GravacaoEmDiscoException.class,() -> {
			prevalencia.executar(trs);
		});
		assertEquals(2L + QTD_ADD_PESSOAS, prevalencia.getIdentificadorUltimaTransacaoExecutada(), "A base não está contando corretamente as transações executadas");
		
		String nome = prevalencia.executar(new ObterNomeExemploModelo());
		assertFalse("teste".equals(nome));
	}	
	
	@Test()
	@Order(8)	
	void testContarObservacoes() throws Exception {
		Thread.sleep(1000);
		assertEquals(2L + QTD_ADD_PESSOAS, observadorTeste.getContador(), "A contagem de observações está errada");
	}
}