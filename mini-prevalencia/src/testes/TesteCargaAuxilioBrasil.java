package testes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.tec.mboi.api.MiniPrevalencia;
import br.tec.mboi.api.MiniPrevalencia.ExecucaoTransacaoException;
import testes.consultas.auxbrasil.ConsultarBeneficiadosPorNomeFonetico;
import testes.consultas.auxbrasil.ContarBeneficiados;
import testes.consultas.auxbrasil.ListarBeneficiosPorCidade;
import testes.consultas.auxbrasil.SomarTotal;
import testes.consultas.auxbrasil.TotalizarValoresBeneficiosPorCidade;
import testes.entidades.auxbrasil.AuxilioBrasil;
import testes.entidades.auxbrasil.Beneficio;
import testes.entidades.auxbrasil.Cidade;
import testes.exceptions.JaCadastradoException;
import testes.transacoes.auxbrasil.AdicionarBeneficiosEmLote;
import testes.transacoes.auxbrasil.AdicionarCidadeSeNaoExistir;
import testes.transacoes.auxbrasil.AdicionarUfSeNaoExistir;
import testes.transacoes.auxbrasil.BeneficioVO;
import testes.util.MetaphonePtBrFrouxo;

/**
 * Teste carregando base com mais de 21 milhões de registros.
 * Baixar o arquivo 202301_AuxilioBrasil.csv em https://portaldatransparencia.gov.br/download-de-dados/auxilio-brasil
 * @author vasselai1
 */
class TesteCargaAuxilioBrasil {

	private static DecimalFormat DF = new DecimalFormat("#,##0.00");
	private static final String DIRETORIO = System.getProperty("user.home") + "/AUX_BRASIL_TRANSACAO";//Criar este diretório
	private static final String CSV_DIR = DIRETORIO + "/CSV";//copiar o arquivo 202301_AuxilioBrasil.csv neste diretório 	
	private static final Map<String , String> CACHE_CODIGOS = new HashMap<String, String>();
	
	static {
		MiniPrevalencia.configurar(DIRETORIO, false, null, true);//Neste caso é melhor controlar o momento de gravação do arquivo acelerador.
	}	
	
	/**
	 * Primeira execução faz a importação dos dados para bascodigosFoneticosBuscae de prevalencia, segunda execução faz teste de pesquisa.
	 * java -Xms4000m -Xmx6000m -jar importacao.jar 
	 * OBS: gerar um jar deste projeto ou ter memória suficiente pra rodar dentro de uma ide 
	 */
	public static void main(String[] args) throws Exception {
		long tempoInicialCarregamento = System.currentTimeMillis();		
		MiniPrevalencia<AuxilioBrasil> prevalencia = MiniPrevalencia.prevalecer(AuxilioBrasil.class); 
		int quantidadeBeneficios = prevalencia.executar(new ContarBeneficiados());
		System.out.println("Carregamento base em : " + ((System.currentTimeMillis() - tempoInicialCarregamento) / 1000) + " segundos > " + quantidadeBeneficios + " beneficios");
		if (quantidadeBeneficios < 21626680) {
			carregarCSV();
		} else {
			pesquisarNomeSobrenome();
			ListarBeneficiosParanavaiPR();
			totalizarPorCidade();
			totalizarBeneficios();
		}
	}
	
	/**
	 * Em um notebook i5 mobile de primeira geração com ssd...
	 * Carregamento base em : 42 segundos = 21626680 benefícios + cidades + estados
	 * Pesquisa dos códigos fonéticos contém "Cicero + Paulino". Resultados: 318 benefícios em 538 milissegundos
	 */
	public static void pesquisarNomeSobrenome() {
		System.out.println("\n");
		System.out.println("Inciando pesquisa nome e sobrenome fonéticos...");
		MiniPrevalencia<AuxilioBrasil> prevalencia = MiniPrevalencia.prevalecer(AuxilioBrasil.class);//Arquivo acelerador de 1.4 GB, 21626680 objetos de benefícios + cidades + estados
		
		long tempoInicialPesquisa = System.currentTimeMillis();
		List<Beneficio> beneficios = prevalencia.executar(new ConsultarBeneficiadosPorNomeFonetico(obterCodigosFoneticos("Cicero Paulino").split(" ")));
		System.out.println("Pesquisa dos códigos fonéticos contém Cicero + Paulino, resultados: " + beneficios.size() + " em " + (System.currentTimeMillis() - tempoInicialPesquisa) + " ms");
		
		System.out.println("\n\n Resultados:");
		for (Beneficio beneficioLoop : beneficios) {
			System.out.println(beneficioLoop.getNomeBeneficiado() + " : " + beneficioLoop.getValor());
		}
		System.out.println("Fim pesquisa nome e sobrenome fonéticos!");
		System.out.println("\n");
	}
	
	public static void ListarBeneficiosParanavaiPR() {
		System.out.println("\n");
		System.out.println("Inciando Listagem de beneficios de Paranavaí-PR...");
		MiniPrevalencia<AuxilioBrasil> prevalencia = MiniPrevalencia.prevalecer(AuxilioBrasil.class);
		
		long tempoInicialPesquisa = System.currentTimeMillis();
		List<Beneficio> beneficios = prevalencia.executar(new ListarBeneficiosPorCidade("PARANAVAI", "PR"));
		System.out.println("Listagem de resultados: " + beneficios.size() + " em " + (System.currentTimeMillis() - tempoInicialPesquisa) + " ms");
		
		System.out.println("\n\n Resultados:");
		for (Beneficio beneficioLoop : beneficios) {
			System.out.println(beneficioLoop.getNomeBeneficiado() + "; " + DF.format(beneficioLoop.getValor()));
		}
		System.out.println("Fim Listagem!");
		System.out.println("\n");
	}	
	
	public static void totalizarPorCidade() {
		System.out.println("\n");
		System.out.println("Inciando totalização por cidade...");
		MiniPrevalencia<AuxilioBrasil> prevalencia = MiniPrevalencia.prevalecer(AuxilioBrasil.class);
		
		long tempoInicialPesquisa = System.currentTimeMillis();
		Map<Cidade, Double> mapaTotalizacao = prevalencia.executar(new TotalizarValoresBeneficiosPorCidade());
		System.out.println("Totalização de " + mapaTotalizacao.size() + " cidades em " + (System.currentTimeMillis() - tempoInicialPesquisa) + " ms");
		
		System.out.println("\n\n Resultados:");
		for (Cidade chave : mapaTotalizacao.keySet()) {
			System.out.println(chave.getNome() + "-" + chave.getEstado().getSigla() + " : " + DF.format(mapaTotalizacao.get(chave)));
		}
		System.out.println("Fim totalização estados...");
		System.out.println("\n");
	}
	
	/**
	 * 660ms
	 */
	public static void totalizarBeneficios() {
		System.out.println("\n");
		System.out.println("Inciando Totalização...");
		MiniPrevalencia<AuxilioBrasil> prevalencia = MiniPrevalencia.prevalecer(AuxilioBrasil.class);
		
		long tempoInicialPesquisa = System.currentTimeMillis();
		Double total = prevalencia.executar(new SomarTotal());
		System.out.println("Resultados: em " + (System.currentTimeMillis() - tempoInicialPesquisa) + " ms");
		
		System.out.println("\n\n Resultado:" + DF.format(total));
		
		System.out.println("Fim totalização");
		System.out.println("\n");
	}	
	
	public static void carregarCSV() throws Exception {
		System.out.println("Inciando leitura do arquivo CSV...");		
		MiniPrevalencia<AuxilioBrasil> prevalencia = MiniPrevalencia.prevalecer(AuxilioBrasil.class);		
		
		List<BeneficioVO> bufferBeneficios = new ArrayList<BeneficioVO>();
		File arquivoDados = new File(new File(CSV_DIR), "202301_AuxilioBrasil.csv");//Arquivo de 2.4 GB e 21626680 linhas
		BufferedReader leitor = new BufferedReader(new FileReader(arquivoDados), 8192000);
		String linha = null;
		
		long tempoInicialGravacao = System.currentTimeMillis();
		int contador = 0;
		long tempoParcialInicial = System.currentTimeMillis();
		while ((linha = leitor.readLine()) != null) {
			if (linha.contains("CPF FAVORECIDO")) {
				continue;
			}
			contador++;
			
			BeneficioVO beneficioVO = converterLinha(linha);
			try {
				prevalencia.executar(new AdicionarUfSeNaoExistir(beneficioVO.getSiglaUf() ));
			} catch (JaCadastradoException e) {
				// Se ja for cadastrado ignorar...
			}
			try {
				prevalencia.executar(new AdicionarCidadeSeNaoExistir(beneficioVO.getSiglaUf(), beneficioVO.getNomeCidade()));
			} catch (JaCadastradoException e) {
				// Se ja for cadastrado ignorar...
			}
			
			bufferBeneficios.add(beneficioVO);
				
			if ((contador % 1000000 == 0)) {
				prevalencia.executar(new AdicionarBeneficiosEmLote(bufferBeneficios));//Cadastro em lote de 43.000 objetos de benefício por segundo				
				bufferBeneficios.clear();				
				System.out.println(contador + " : " + (System.currentTimeMillis() - tempoParcialInicial) + " ms");
				tempoParcialInicial = System.currentTimeMillis();
			}
			
			if ((contador % 5000000 == 0)) {
				long tempoInicialAcelerador = System.currentTimeMillis();
				System.out.println("Iniciando gravação arquivo acelerador...");
				prevalencia.atualizarArquivoAceleradorInicializacao();
				System.out.println("Fim gravação acelerador : " + (System.currentTimeMillis() - tempoInicialAcelerador) + " ms");
			}
		}
		
		if (!bufferBeneficios.isEmpty()) {
			prevalencia.executar(new AdicionarBeneficiosEmLote(bufferBeneficios));
			prevalencia.atualizarArquivoAceleradorInicializacao();
		}
		System.out.println("Tempo Gravacao de " + contador + " registros : " + ((System.currentTimeMillis() - tempoInicialGravacao) / 1000) + " segundos");
		
		bufferBeneficios.clear();
		leitor.close();
				
		long tempoInicialGravacaoAcelerador = System.currentTimeMillis();
		prevalencia.atualizarArquivoAceleradorInicializacao();			
		System.out.println("Tempo Gravação acelerador : " + ((System.currentTimeMillis() - tempoInicialGravacaoAcelerador) / 1000) + " segundos");
		
		System.out.println("Fim leitura arquivo CSV, total: " + contador + "!");
	}

	private static BeneficioVO converterLinha(String linha) throws ExecucaoTransacaoException, Exception {
		linha = linha.replace("\"", "");
		String[] partesLinha = linha.split(";");
		String anoMesCompStr = partesLinha[0];
		String anoCompStr = anoMesCompStr.substring(0, 4);
		String mesCompStr = anoMesCompStr.substring(4, 6);
		String siglaEstadoStr = partesLinha[2];
		String nomeCidadeStr = partesLinha[4];
		String nomeBeneficiado = partesLinha[7];
		String valorStr = partesLinha[8];		
		
		Short anoCompetencia = Short.parseShort(anoCompStr);
		Short mesCompetencia = Short.parseShort(mesCompStr);
		Float valor = Float.parseFloat(valorStr.replace(",", "."));		
		String codigoFoneticoNome = obterCodigosFoneticos(nomeBeneficiado);
		String chaveCidade = AdicionarCidadeSeNaoExistir.padronizarChaveCidade(nomeCidadeStr, siglaEstadoStr);
		
		return new BeneficioVO(siglaEstadoStr, nomeCidadeStr, chaveCidade, nomeBeneficiado, codigoFoneticoNome, valor, anoCompetencia, mesCompetencia); 
	}
	
	public static String obterCodigosFoneticos(String nome) {
		String[] partesNome = nome.split(" ");
		StringBuilder retorno = new StringBuilder();
		boolean primeiro = true;
		for (String parteNomeLoop : partesNome) {
			if (primeiro) {
				primeiro = false;
			} else {
				retorno.append(" ");
			}
			String codigo = CACHE_CODIGOS.get(parteNomeLoop);
			if (codigo == null) {
				codigo = new MetaphonePtBrFrouxo(parteNomeLoop).toString();
				CACHE_CODIGOS.put(parteNomeLoop, codigo);
			}
			retorno.append(codigo);
		}
		return retorno.toString();
	}
	
}