package testes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import br.tec.mboi.api.MiniPrevalencia;
import br.tec.mboi.api.MiniPrevalencia.Configurador;
import br.tec.mboi.api.MiniPrevalencia.ExecucaoTransacaoException;
import testes.consultas.exemplo.ObterEstoque;
import testes.entidades.exemplo.ExemploModelo;
import testes.exceptions.ValidacaoCampoException;
import testes.transacoes.exemplo.AdicionarEstoque;
import testes.transacoes.exemplo.AtribuirEstoque;
import testes.transacoes.exemplo.BaixarEstoque;

@TestMethodOrder(OrderAnnotation.class)
class TesteIsolamento {

	private static final String DIRETORIO = System.getProperty("user.home") + "/ExemploModelo_Transacao";
	
	private int QTD_THREADS = 15000;
	
	private static class ThreadAddEstoque extends Thread {
		public void run() {
			try {
				MiniPrevalencia.prevalecer(ExemploModelo.class).executar(new AdicionarEstoque(5));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}	

	private static class ThreadRmvEstoque extends Thread {
		public void run() {
			try {
				MiniPrevalencia.prevalecer(ExemploModelo.class).executar(new BaixarEstoque(10));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}	
	
	@BeforeAll	
	static void inicializarDiretorioEconfigurador() throws IOException {
		if (new File(DIRETORIO + "/" + ExemploModelo.class.getCanonicalName()).exists()) {
			Files.walk(Paths.get(DIRETORIO + "/" + ExemploModelo.class.getCanonicalName())).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
		Configurador conf = new Configurador() {
			public boolean isPrimario() {return true;}
			public String getDiretorioGravacao() {return DIRETORIO;}
			public boolean isRetornoProjegidoPorCopia() {return false;}
			public Integer getSegundosInatividadeParaIniciarGravacaoAcelerador() {return 10;}
			public boolean isApagarTransacoesInternalizadasPeloAcelerador() {return false;}
		};		
		MiniPrevalencia.setConfigurador(conf);		
	}
	
	@Test
	@Order(1)
	void testeIniciarEstoque() throws ValidacaoCampoException, ExecucaoTransacaoException {
		Integer quantidade = 0;
		MiniPrevalencia<ExemploModelo> prevalencia = MiniPrevalencia.prevalecer(ExemploModelo.class);
		prevalencia.executar(new AtribuirEstoque(quantidade));
		Integer resultado = prevalencia.executar(new ObterEstoque());
		assertEquals(quantidade, resultado);
	}

	@Test
	@Order(1)
	void testeAdicionarConcorrente() throws ValidacaoCampoException, ExecucaoTransacaoException, InterruptedException {
		Thread[] threadsAdd = new Thread[QTD_THREADS];
		for (int i = 0; i < QTD_THREADS; i++) {
			threadsAdd[i] = new ThreadAddEstoque();
		}
		for (int i = 0; i < QTD_THREADS; i++) {
			threadsAdd[i].start();
		}
		for (int i = 0; i < QTD_THREADS; i++) {
			threadsAdd[i].join();
		}
		MiniPrevalencia<ExemploModelo> prevalencia = MiniPrevalencia.prevalecer(ExemploModelo.class);
		Integer resultado = prevalencia.executar(new ObterEstoque());
		assertEquals(AdicionarEstoque.QTD_MAXIMA, resultado);
	}

	@Test
	@Order(2)
	void testeRemoverConcorrente() throws ValidacaoCampoException, ExecucaoTransacaoException, InterruptedException {
		Thread[] threadsRmv = new Thread[QTD_THREADS];
		for (int i = 0; i < QTD_THREADS; i++) {
			threadsRmv[i] = new ThreadRmvEstoque();
		}
		for (int i = 0; i < QTD_THREADS; i++) {
			threadsRmv[i].start();
		}
		for (int i = 0; i < QTD_THREADS; i++) {
			threadsRmv[i].join();
		}
		MiniPrevalencia<ExemploModelo> prevalencia = MiniPrevalencia.prevalecer(ExemploModelo.class);
		Integer resultado = prevalencia.executar(new ObterEstoque());
		assertEquals(0, resultado);
	}	
	
}
