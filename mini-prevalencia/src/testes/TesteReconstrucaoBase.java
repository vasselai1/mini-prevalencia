package testes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import br.tec.mboi.api.MiniPrevalencia;
import br.tec.mboi.api.MiniPrevalencia.ExecucaoTransacaoException;
import testes.consultas.exemplo.ListarPalavrasTextoFragmentado;
import testes.entidades.exemplo.ExemploModelo;
import testes.exceptions.ValidacaoCampoException;
import testes.transacoes.exemplo.AdicionarTextoFragmentado;

class TesteReconstrucaoBase {

	private static final String DIRETORIO = System.getProperty("user.home") + "/ExemploModelo_Transacao";
	
	private static final String[] PALAVRAS_TESTE = {"Ricardo", " Vasselai", " Paulino", " testando", " ordem" , " de" , " reconstrução"};
	
	static {
		MiniPrevalencia.configurar(DIRETORIO, false, 60, false);
	}	
	
	@BeforeAll	
	static void limparDiretorioDeGravacao() throws IOException {
		if (new File(DIRETORIO + "/" + ExemploModelo.class.getCanonicalName()).exists()) {
			Files.walk(Paths.get(DIRETORIO + "/" + ExemploModelo.class.getCanonicalName())).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}				
	}
	
	@Test
	void test() throws ValidacaoCampoException, ExecucaoTransacaoException {
		MiniPrevalencia<ExemploModelo> prevalencia1 = MiniPrevalencia.prevalecer(ExemploModelo.class);		
		prevalencia1.executar(new AdicionarTextoFragmentado(PALAVRAS_TESTE[0]));
		prevalencia1.executar(new AdicionarTextoFragmentado(PALAVRAS_TESTE[1]));
		prevalencia1.executar(new AdicionarTextoFragmentado(PALAVRAS_TESTE[2]));
		prevalencia1.finalizarPrevalencia();
		
		MiniPrevalencia<ExemploModelo> prevalencia2 = MiniPrevalencia.prevalecer(ExemploModelo.class);
		List<String> palavrasBase1 = prevalencia2.executar(new ListarPalavrasTextoFragmentado());		
		for (int indicePalavra = 0; indicePalavra < 3; indicePalavra++) {
			assertEquals(PALAVRAS_TESTE[indicePalavra], palavrasBase1.get(indicePalavra));//Testa construção somente com execução de transações
		}
		prevalencia2.executar(new AdicionarTextoFragmentado(PALAVRAS_TESTE[3]));
		prevalencia2.executar(new AdicionarTextoFragmentado(PALAVRAS_TESTE[4]));
		prevalencia2.atualizarArquivoAceleradorInicializacao();//Forçando gravação antes dos 60 segundos configurado
		prevalencia2.finalizarPrevalencia();
		
		MiniPrevalencia<ExemploModelo> prevalencia3 = MiniPrevalencia.prevalecer(ExemploModelo.class);
		prevalencia3.executar(new AdicionarTextoFragmentado(PALAVRAS_TESTE[5]));
		prevalencia3.executar(new AdicionarTextoFragmentado(PALAVRAS_TESTE[6]));
		prevalencia3.finalizarPrevalencia();
		
		MiniPrevalencia<ExemploModelo> prevalencia4 = MiniPrevalencia.prevalecer(ExemploModelo.class);
		List<String> palavrasBase2 = prevalencia4.executar(new ListarPalavrasTextoFragmentado());		
		for (int indicePalavra = 0; indicePalavra < PALAVRAS_TESTE.length; indicePalavra++) {
			assertEquals(PALAVRAS_TESTE[indicePalavra], palavrasBase2.get(indicePalavra));//Testa a união do arquivo acelerador seguido das transações restantes
		}
		
		prevalencia4.finalizarPrevalencia();
				
		MiniPrevalencia<ExemploModelo> prevalencia5 = MiniPrevalencia.prevalecer(ExemploModelo.class);
		List<String> palavrasBase3 = prevalencia5.executar(new ListarPalavrasTextoFragmentado());		
		for (int indicePalavra = 0; indicePalavra < PALAVRAS_TESTE.length; indicePalavra++) {
			assertEquals(PALAVRAS_TESTE[indicePalavra], palavrasBase3.get(indicePalavra));//Testa a união do arquivo acelerador seguido das transações restantes depois de apaguar transacões aceleradas			
		}
		
		prevalencia5.atualizarArquivoAceleradorInicializacao();		
		prevalencia5.finalizarPrevalencia();
		
		MiniPrevalencia<ExemploModelo> prevalencia6 = MiniPrevalencia.prevalecer(ExemploModelo.class);
		List<String> palavrasBase4 = prevalencia6.executar(new ListarPalavrasTextoFragmentado());		
		for (int indicePalavra = 0; indicePalavra < PALAVRAS_TESTE.length; indicePalavra++) {
			assertEquals(PALAVRAS_TESTE[indicePalavra], palavrasBase4.get(indicePalavra));//Testa o carregamento usando somente o arquivo acelerador			
		}
	}

}