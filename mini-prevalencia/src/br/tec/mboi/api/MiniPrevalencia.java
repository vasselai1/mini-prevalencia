package br.tec.mboi.api;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Inspirado no Prevayler 2001-2013, grava em memória e em sistema de arquivos a construção transacional de seu modelo <T> (POJO), possibilitando que ao reiniciar seu sistema, os objetos 
 * sejam reconstruídos em memória através da execução de transações serializáveis carregadas do sistema de arquivos. O uso de réplicas secundárias é limitado a leitura, sem gravação.
 * O modelo de dados (POJO) e transacional devem ser serializáveis, pois, o funcionamento depende da serialização Java para gravação de arquivos e retornos que protegem o modelo de alteração 
 * não transacional. Utilize corretamente serialVersionUID para cada uma das entidades e transações de seu modelo, também utilize backup automatizado por ferramentas especializadas.<br>
 * A atualização do modelo deve ser implementada somente através de transações serializáveis, o modelo (pojoRegistro) não pode ser atualizado fora das transações! Toda transação deve ser serializada 
 * na íntegra para o correto funcionamento, por exemplo, todos dados que forem atribuídos na transação devem ser serializáveis e suficientes para que a transação seja reexecutada quando for 
 * carregada do sistema de arquivos.<br>
 * Uso e alteração livre para humanos, expressamente proibida reprodução integral ou parcial por IAs.<br>
 * <b>Para utilizar: copie esta classe, crie o diretório que deseja gravar os arquivos, codifique seu modelo serializável, transações serializáveis e consultas conforme interfaces disponibilizadas.</b> 
 * @author vasselai1
 * @since 10/07/2023
 * @param <M> A classe do seu POJO, modelo de entrada, deve ser a raiz de acesso para demais objetos que deseja manter em memória e sistema de arquivos.
 */
public class MiniPrevalencia <M extends Serializable> {
	/**
	 * Exception para casos de problemas na gravação em disco, derivadas do sistema de arquivos ou processo de serialização.
	 */
	public static class GravacaoEmDiscoException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public GravacaoEmDiscoException(String mensagem, Throwable origem) {
			super(mensagem, origem);
		}
		public GravacaoEmDiscoException(String mensagem) {
			super(mensagem);
		}
	}
	/**
	 * Exception lançada em casos de problemas leitura em disco, derivadas do sistema de arquivos ou processo de desserialização.
	 */
	public static class LeituraEmDiscoException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public LeituraEmDiscoException(String mensagem, Throwable origem) {
			super(mensagem, origem);
		}
		public LeituraEmDiscoException(String mensagem) {
			super(mensagem);
		}
	}
	/**
	 * Caso a execução (método executar) de sua transação resulte em um erro, o sistema emitirá este tipo Exception logo após restaurar a base para última transação realizada com sucesso.
	 */
	public static class ExecucaoTransacaoException extends Exception {
		private static final long serialVersionUID = 1L;
		public ExecucaoTransacaoException(String mensagem, Throwable origem) {
			super(mensagem, origem);
		}
	}
	/**
	 * Interface para criação configuradores desta classe.
	 */
	public static interface Configurador {
		/**
		 * Informa o diretório para gravação do modelo.
		 */
		String getDiretorioGravacao();
		/**
		 * Informa se as instâncias fornecidas pela classe serão primárias (TRUE) ou réplicas (FALSE). 
		 */
		boolean isPrimario();
		/**
		 * Informa se todos retornos de transações ou consultas serão copiados para evitar alteração do modelo fora de transações. FALSE é recomendado para melhorar o desempenho, TRUE para não 
		 * se preocupar com assunto e consumir muito mais memória.
		 */
		boolean isRetornoProjegidoPorCopia();
		/**
		 * Quantidade de segundos sem transação para iniciar a gravação do arquivo acelerador. O ajuste é importante porque a gravação bloqueia temporariamente a execução de transações, sem
		 * impedir consultas. NULL desabilita a gravação automática do arquivo acelerador. 
		 */
		Integer getSegundosInatividadeParaIniciarGravacaoAcelerador();
		/**
		 * TRUE caso deseje apagar arquivos de transação já internalizados pelo arquivo acelerador, FALSE caso contrário.
		 */
		boolean isApagarTransacoesInternalizadasPeloAcelerador(); 
	}
	/**
	 * Interface para criação e execução de transações atômicas, consistentes, isoladas e duráveis que atualizam o estado do seu modelo. Podem ser utilizadas para consultas síncronas e devem 
	 * ser utilizadas para atualizar o modelo (pojoUnico).  Exceptions relacionadas a transação utilizam o método toString em seu detalhamento. Transações além de serializáveis precisam ser 
	 * repetíveis após sua desserialização, desta forma, garanta que a implementação de transações que alterem dados sejam autocontidas para correta serialização.<br>
	 * <b>Durante o crescimento do seu sistema, recomendo que transações já executadas não sejam alteradas, crie uma nova classe para uma transação nova, mantendo antiga dentro do sistema.</b>
	 * @param <R> Tipo de retorno após execução.
	 * @param <T> Seu modelo de dados mantido em memória e sistema de arquivo.
	 * @param <E> Sua exception que pode ser lançada no processo de validação pré execução.
	 */
	public abstract interface Transacao <T extends Serializable, E extends Throwable> {
		/**
		 * Devido ao processo de reversão (roolback) ser lento consumir memória, e também a gravação de transações custosa, a implementação deste método é útil para verificar pré-condições 
		 * que invalidem a execução da transação, levantar uma exception dentro deste método não implica em reversão da base, apenas impede a execução da transação. A validação, assim como 
		 * executar, é executada em um bloco sincronizado, desta forma você pode ter certeza do isolamento e da consistência de sua validação.<br>
		 * OBS: não alterar dados do seu modelo neste método, e muito menos fora de transações! Aqui é o momento para impedir a execução de uma transação lançando sua Exception, não deixe 
		 * para abortar uma transação dentro do método executar, pois, no método executar um arquivo de transação já foi gravado e a base passará por reversão. Também evite retornos
		 * sem alterar o estado do seu modelo, isso degrada o desempenho com carregamento de transações inúteis. 
		 * @param pojoUnico Seu modelo de dados.
		 * @throws E Sua lógica de negócio, ao identificar um impeditivo na execução, deve lançar uma exception abortando a execução da transação.
		 */
		void validar(T pojoRegistro) throws E;		
	}	
	/**
	 * Interface para criação e execução de transações ACID com retorno.
	 * @param <R> O tipo de retorno.
	 * @param <T> O tipo do seu modelo (pojoRegistro).
	 * @param <E> O tipo de exception que sua validação pode levantar.
	 */
	public interface TransacaoComRetorno <R, T extends Serializable, E extends Throwable> extends Transacao<T, E>, Serializable {
		/**
		 * Os atributos (serializáveis) da sua instância de transação devem ser utilizados dentro deste método para atualização de seu modelo. 
		 * Este método, em conjunto com validar, são executados de forma síncrona para garantir atomicidade e isolamento.<br>
		 * Caso um erro ocorra nesta fase a base será revertida para última versão válida, ou seja ela será recarregada do sistema arquivos, assim a reversão consumirá o 
		 * mesmo tempo que a inicialização. Alteração no modelo só pode ocorrer dentro da implementação deste método, após o retorno, fora deste método, se 
		 * ocorrer qualquer atualização no modelo, o novo estado não estará gravado em sistema de arquivos, invalidando transações sequentes.<br>
		 * OBS: não abortar a transação dentro da implementação deste método com returns, exceptions ou qualquer outro artifício, se precisar aborte a transação durante a validação.
		 * @param pojoUnico O modelo mantido em memória.
		 * @return Seu retorno.
		 */
		R executar(T pojoRegistro);
	}
	/**
	 * Interface para criação e execução de transações ACID sem retorno.
	 * @param <T> Seu modelo de dados mantido em memória e sistema de arquivo.
	 * @param <E> Sua exception que pode ser lançada no processo de validação pré execução.
	 */
	public interface TransacaoSemRetorno <T extends Serializable, E extends Throwable> extends Transacao<T, E>, Serializable {
		/**
		 * Mesmos requisitos de transação com retorno para ser executada, mas não tem retorno.
		 * @see TransacaoComRetorno
		 * @param pojoUnico O modelo mantido em memória.
		 */
		void executar(T pojoUnico);
	}	
	/**
	 * Interface para criação e execução de consultas. OBS: não faça atribuições nos objetos retornados pela sua pesquisa!
	 * @param <R> Seu tipo de retorno
	 * @param <T> Seu modelo de dados mantido em memória e sistema de arquivo.
	 */
	public interface Consulta<R, T extends Serializable> {
		R executar(T pojoUnico);
	}
	/**
	 * Interface para observadores monitorarem transações executadas.
	 */
	public interface Observador <T extends Serializable> {
		void receberAvisoExecucao(Transacao<T, ? extends Throwable> transacao);
	}
	
	/**
	 * Configurador da classe para gerar novas instâncias. 
	 */
	private static Configurador configurador;
	/**
	 * Mapa das bases inicializadas, vários modelos de dados independentes entre si podem ser mantidos em um diretório e também em memória. 
	 */
	private static Map<Class<? extends Serializable>, MiniPrevalencia<?>> mapaPrevalencias = new HashMap<Class<? extends Serializable>, MiniPrevalencia<?>>();
	/**
	 * Caso TRUE as instâncias só efetuam consultas.
	 */
	private static Boolean replica = null;
	/**
	 * Diretório de gravação dos dados, sequencias, transações etc.
	 */
	private static String diretorio = null;
	/**
	 * Com TRUE os retornos são copiados em novas instâncias para evitar a alteração dos registros internos fora das transações. No entanto, este processo
	 * consome memória e processamento, como alternativa pode ser desativado para melhorar o desempenho e economizar memória. Caso seu código altere objetos
	 * fora das transações sua base se tornará inconsistente. Outra alternativa após desabilitar a cópia, é a implementação manual da cópia dentro das consultas
	 * e transações para os retornos sensíveis (entidades). Para uso performático desative (FALSE) a proteção e não altere entidades fora de transações.
	 */
	private static boolean protegerRetornoCopiandoObjetos = false;
	/**
	 * Por padrão todos arquivos de transação são mantidos em sistema de arquivos por poucos segundos, no entanto durante a configuração inicial é possível optar por não apagar 
	 * as transações que já foram internalizadas pelo arquivo acelerador de inicialização.<br>
	 * TRUE para apagar transações aceleradas.<br>
	 * FALSE para manter todas transações em sistema de arquivo, mesmo as que já estão internalizadas no arquivo acelerador.
	 */
	private static boolean apagarTransacoesAceleradas = true;
	/**
	 * Quantidade de milissegundos de inatividade para disparar a gravação do arquivo acelerador e a eliminação de arquivo transacionais internalizados.
	 */
	private static Long milisegundosInatividadeTransacionalDispararGravacaoAcelerador;
	
	/**
	 * Seu POJO, objeto de entrada para que será mantido pelo sistema de prevalência.
	 */
	private M pojoRegistro;
	/**
	 * O tipo de registro que a instância de prevalência está mantendo.
	 */
	private Class<M> tipoRegistro;
	/**
	 * Serial da ultima transação executada. 
	 */
	private Long ultimaTransacaoExecutada = 0L;
	/**
	 * Serial da ultima transação internalizada pelo arquivo acelerador.
	 */
	private Long ultimaTransacaoAcelerada = 0L;
	/**
	 * Lista de observadores registrados.
	 */
	private HashSet<Observador<M>> observadores = new HashSet<Observador<M>>();
	/**
	 * Logger para operações executadas por threads, demais exceptions são apenas lançadas.
	 */
	private final Logger logger = Logger.getLogger(getClass().getName());
	/**
	 * Indicador de solicitação de finalização;
	 */
	private boolean finalizar = false;
	/**
	 * Indica quando a instância está executando transações para carregar o modelo, com objetivo de impedir operações.
	 */
	private boolean inicializando = false;	
	/**
	 * Momento de inicialização ou ultima transação.
	 */
	private Long momentoInicializacaoOuUltimaTransacao;
	/**
	 * Comparador nome de arquivo como número.
	 */
	private Comparator<File> fileNomeNumeroTransacao = new Comparator<File>() {
		@Override
		public int compare(File o1, File o2) {
			Long lo1 = converterNomeArquivo(o1);
			Long lo2 = converterNomeArquivo(o2);
			return lo1.compareTo(lo2);
		}
	};
	/**
	 * Atribui uma implementação de configurador para inicializar novas instâncias da prevalência.
	 * @param conf Uma implementação de configurador.
	 * @return TRUE quando o configurado é atribuído, false quando não é, isso significa que um configurador já foi atribuído anteriormente.
	 */
	public static boolean setConfigurador(Configurador conf) {
		if (configurador != null) {
			return false;
		}
		configurador = conf;
		return true;
	}
	/**
	 * Informa se ambiente já foi configurado.
	 * @return TRUE caso positivo.
	 */
	public static boolean jaConfigurado() {
		return (replica != null) || (diretorio != null);
	}
	/**
	 * Configuração padrão para inicialização de prevalências, não protege retorno com cópias, após 10 segundos de inatividade transacional o arquivo acelerador é gravado e as 
	 * transações internalizadas pelo acelerador são apagadas. Essa é a configuração mais recomendada para qualquer base.
	 * @param diretorioGravacao Diretório de gravação da base.
	 */
	public static void configurar(String diretorioGravacao) {
		configurar(diretorioGravacao, true, false, 10, true);
	}
	/**
	 * Se por algum motivo precisar alterar os objetos retornados pela base fora de transações, mas sem almejar sua gravação, ultize este método de configuração informando TRUE para
	 * proteger com cópias o retorno. No entanto o uso de memória sobe circunstancialmente.
	 * @param diretorioGravacao Diretório de gravação da base. 
	 * @param protegerRetornosComCopia TRUE para proteger com cópia todos retornos, seja de consultas ou transações com retorno, tudo que sair da base será copiado para evitar que base
	 * seja corrompida por alterações de estado não transacionais.
	 */
	public static void configurar(String diretorioGravacao, boolean protegerRetornosComCopia) {
		configurar(diretorioGravacao, true, protegerRetornosComCopia, 10, true);
	}	
	/**
	 * Configura o funcionamento da classe para gerar instâncias que efetuam gravação no sistema de arquivo. A configuração pode ser customizada para casos específicos, por exemplo 
	 * durante cargas em lote é util disparar manualmente a gravação do acelerador, bem como manter arquivos de transação para inspecionar transações. No entanto, por não haver balanceamento
	 * de diretórios para gravação de transações, pouco mais de 500.000 arquivos de transações são o suficiente para degradar o desempenho caso o parâmetro apagarTransacoesAceleradas = false
	 * seja mantido por muito tempo. 
	 * @param diretorioGravacao Diretório de gravação dos dados.
 	 * @param protegerRetornosComCopia TRUE para efetuar cópia, assim mudanças de estado nos objetos retornados não afetam os objetos da base, no entanto consome memória e processamento. FALSE para
	 * retorno dos objetos da base, mais rápido com menos processamento, mas caso o estado dos objetos retornados sejam alterados fora de transações a base será corrompida.
	 * @param segundosInatividadeParaGravarAcelerador Quantidade de segundos sem execução de transação para ínicio da gravação do arquivo acelerador de inicialização.
	 * @param apagarTransacoesAceleradas TRUE para apagar as transacoes que já foram internalizadas no arquivo acelerador. FALSE para manter todas transações individualmente em sistema arquivo, não 
	 * recomendado para bases com mais de 500.000 transações.
	 */
	public static void configurar(String diretorioGravacao, boolean protegerRetornosComCopia, Integer segundosInatividadeParaGravarAcelerador, boolean apagarTransacoesAceleradas) {
		configurar(diretorioGravacao, true, protegerRetornosComCopia, segundosInatividadeParaGravarAcelerador, apagarTransacoesAceleradas);
	}
	/**
	 * Configuração de réplica recomendada, sem proteção por cópia dos retornos.
	 */
	public static void configurarReplica(String diretorioLeitura) {
		configurar(diretorioLeitura, false, false, null, false);
	}
	/**
	 * Configura a classe para gerar instâncias de réplicas somente leitura. Para criação de réplicas, basta que elas tenham acesso ao sistema de arquivos em um compartilhamento de rede.
	 * @param diretorioLeitura O diretório onde os dados estão sendo gravados pela instância primária.
	 * @param protegerRetornosComCopia TRUE para efetuar cópia de proteção dos dados retornados, assim mudanças de estado nos objetos retornados não afetam os objetos da base, no entanto consome 
	 * memória e processamento. FALSE para retorno dos objetos da base, mais rápido e usa menos memória, mas caso o estado dos objetos retornados sejam alterados a instância 
	 * se torna diferente do sistema de arquivos.
	 */
	public static void configurarReplica(String diretorioLeitura, boolean protegerRetornosComCopia) {
		configurar(diretorioLeitura, false, protegerRetornosComCopia, null, false);
	}	
	/**
	 * Configura o funcionamento de novas instâncias conforme parâmetros informados. Este método da classe deve ser executado apenas um vez, como alternativa a blocos estáticos e 
	 * para não precisar controlar a quantidade de chamadas deste método, crie uma configurador informe o configurador para classe. 
	 * @param diretorioGravacao O diretório que você criou e deseja gravar seus dados.
	 * @param primario TRUE para primário (efetua gravação), FALSE para réplica (somente leitura).
	 * @param protegerRetornosComCopia TRUE para proteger que retornos atribuam dados o seu modelo, FALSE desabilita a proteção de alteração. No entanto é recomendável desabilitar,
	 * e ao alterar seu modelo somente dentro das transações, isso reduz o uso de memória e aumenta a velocidade de resposta.
	 * @param segundosInatividadeParaGravarAcelerador Quantidade de segundos sem execução de transações para disparar a gravação do arquivo acelerador, NULL desativa a gravação automática do acelerador.
	 * @throws IllegalStateException quando o ambiente foi configurado anteriormente, executar apenas uma vez este métodos!
	 * @see Configurador
	 */
	private static void configurar(String diretorioGravacao, boolean primario, boolean protegerRetornosComCopia, Integer segundosInatividadeParaGravarAcelerador, boolean apagarTransacoesJaAceleradas) {
		if (jaConfigurado()) {
			throw new IllegalStateException("Ambiente já configurado!");
		}
		if ((segundosInatividadeParaGravarAcelerador != null) && (segundosInatividadeParaGravarAcelerador < 10)) {
			throw new IllegalArgumentException("O tempo de inatividade deve ser maior ou igual a 10 segundos.");
		}
		diretorio = diretorioGravacao;		
		replica = !primario;
		protegerRetornoCopiandoObjetos = protegerRetornosComCopia;
		milisegundosInatividadeTransacionalDispararGravacaoAcelerador = (segundosInatividadeParaGravarAcelerador != null) ? (segundosInatividadeParaGravarAcelerador * 1000L) : null;
		apagarTransacoesAceleradas = apagarTransacoesJaAceleradas;
	}
	
	/**
	 * Construtor para novos tipos de registro.
	 * @param tipoRegistro O tipo de registro serializável.
	 */
	private MiniPrevalencia(Class<M> tipoRegistro) {
		this.tipoRegistro = tipoRegistro; 
		momentoInicializacaoOuUltimaTransacao = System.currentTimeMillis();
	}	
	
	/**
	 * Constrói a raiz da prevalência criando uma instância do seu modelo, que é a entrada de acesso para os demais objetos. Seu modelo precisa ser um POJO com um construtor padrão. 	
	 * @return Uma instância do POJO raiz.
	 */
	private M construirNovoPojoUnico() {
		if (tipoRegistro == null) {
			throw new IllegalArgumentException("A classe do seu tipo de registro (seu modelo) não não foi informada!");
		}
		try {
			if (tipoRegistro.getDeclaredConstructor() == null) {
				throw new IllegalArgumentException("A classe " + tipoRegistro.getName() + " não tem um construtor padrão (vazio)!");
			}
			return tipoRegistro.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Não foi possível obter o construtor da classe " + tipoRegistro.getName() + ", verifique se ele é publico!", e);
		}		
	}
	
	/**
	 * Obtém uma instância pronta para manter a prevalência do modelo informado, instânciando apenas uma vez para cada tipo de registro. Aqui os dados gravados do 
	 * pojoRegistro (seu modelo informado) são desserializados do arquivo acelerador e carregados em memória, na sequencia todas transações que ainda não foram internalizadas pelo 
	 * arquivo acelerador são carregadas para o modelo, através da re-execução de transações.<br>
	 * OBS: usualmente apenas um modelo é utilizado por sistema, no entanto é possível utilizar mais de um modelo, mas lembre-se um não se relacionará com outro.
	 * @param <Z> A classe do seu modelo, deve ser serializável.
	 * @param tipoRegistro Tipo do registro desejado, deve ser um POJO serializável, o contrutor deve padrão (sem parâmetros), e a raiz de acesso para demais objetos do seu modelo.
	 * @return Uma instância ACID desta classe que controla a gravação do modelo em memória e em sistema de arquivos, inicializado com todos dados carregados do sistema de arquivos (quando houver).
	 * @throws LeituraEmDiscoException Caso existam registros em sistema de arquivo e eles não possam ser lidos, caso o principal arquivo acelerador não possa ser lido ou desserializado, caso
	 * alguma transação não possa ser lida ou desserializada.
	 */
	@SuppressWarnings("unchecked")
	public synchronized static <Z extends Serializable> MiniPrevalencia<Z> prevalecer(Class<Z> tipoRegistro) throws LeituraEmDiscoException {
		if ((configurador != null) && !jaConfigurado()) {
			configurar(configurador.getDiretorioGravacao(), 
					   configurador.isPrimario(), 
					   configurador.isRetornoProjegidoPorCopia(), 
					   configurador.getSegundosInatividadeParaIniciarGravacaoAcelerador(),
					   configurador.isApagarTransacoesInternalizadasPeloAcelerador());
		}
		if ((replica == null) || (diretorio == null)) {
			throw new IllegalStateException("Classe não configurada para o ambiente, é necessário executar o método configurar na classe.");
		}		
		if (mapaPrevalencias.containsKey(tipoRegistro)) {
			mapaPrevalencias.get(tipoRegistro).validarFinalizacao();
			return (MiniPrevalencia<Z>) mapaPrevalencias.get(tipoRegistro);
		}
		MiniPrevalencia<Z> memoriaParaArquivo = new MiniPrevalencia<Z>(tipoRegistro);		
		mapaPrevalencias.put(tipoRegistro, memoriaParaArquivo);		
		try {
			memoriaParaArquivo.carregarDadosDoSistemaDeArquivo();			
		} catch (Exception e) {
			throw new LeituraEmDiscoException("Erro ao preparar ou carregar os objetos do sistema de arquivos, não foi possível iniciar a prevalência.", e);
		}
		if (replica) {
			memoriaParaArquivo.criarOuvinteGravacaoParaExecutarTransacoes();
		} else if (milisegundosInatividadeTransacionalDispararGravacaoAcelerador != null) {
			memoriaParaArquivo.iniciarMonitorGravacaoAutomaticaArquivoAcelerador();
		}	
		return (MiniPrevalencia<Z>) mapaPrevalencias.get(tipoRegistro);
	}
	
	/**
	 * Executa sua transação com retorno, também pode ser utilizado para consultas "limpas" sem alteração do modelo.
	 * @param transacao Sua transação.
	 * @throws ExecucaoTransacaoException Caso a execução de sua transação resulte em erro, revise seu código, pois, ele é o único resposável por este erro. Mas não se preocupe, 
	 * o código da transação só é efetivado se for executado na íntegra sem erro, por ser uma transação atômica.
	 */
	@SuppressWarnings("unchecked")
	public <R, E extends Throwable> R executar(TransacaoComRetorno<R, M, E> transacao) throws E, GravacaoEmDiscoException, LeituraEmDiscoException, ExecucaoTransacaoException {
		return (R) executarTransacao(transacao);
	}
	/**
	 * Executa sua transação sem retorno.
	 * @param transacao Sua transação.
	 * @throws ExecucaoTransacaoException Caso a execução de sua transação resulte em erro, revise seu código, pois, ele é o único resposável por este erro. Mas não se preocupe, 
	 * o código da transação só é efetivado se for executado na íntegra sem erro, por ser uma transação atômica.
	 */
	public <E extends Throwable> void executar(TransacaoSemRetorno<M, E> transacao) throws E, GravacaoEmDiscoException, LeituraEmDiscoException, ExecucaoTransacaoException {
		executarTransacao(transacao);
	}
	/**
	 * Executa sua consulta, aqui podem ocorrer leituras "sujas", que podem passar por reversão. Caso precise de leituras "limpas" utilize uma transação com retorno.
	 * @param <R> Seu tipo de retorno
	 * @param consulta A consulta desejável.
	 * @return Seu retorno.
	 */
	public <R> R executar(Consulta<R, M> consulta) {		
		validarInicializacao();
		validarFinalizacao();
		return copiarObjeto(consulta.executar(pojoRegistro), null, consulta);
	}
	/**
	 * Executa sua transação Atômica, Consistente, Isolada e Durável através de um bloco sincronizado pela única instância do seu modelo. Apesar de sicronizado, mais de 2000 transações 
	 * por segundo foram executadas em um notebook i5 de primeira geração, as transações de teste envolveram consultas para validação e alteração de dados. Como sugestão mantenha suas 
	 * trasações pequenas e simples para tornar sua execução mais rápida.
	 * @param <R> Seu tipo de retorno.
	 * @param <E> Seu tipo de exception que pode ser lançada durante a validação.
	 * @param transacao Sua transação totalmente serializável consequentemente repetível após desserialização, não pode depender do que não foi serializado!
	 * @return Seu retorno
	 * @throws E Caso sua implementação levante uma exception durante o método de validação.
	 * @throws GravacaoEmDiscoException Caso ocorra um problema na gravação ou serialização da transação. Não precisa ser verificada em blocos try catch.
	 * @throws LeituraEmDiscoException Caso ocorra um erro no processo de reversão da base, neste caso a base é finalizada e considerada corrompida até que transação com problema seja corrigida.
	 * Não precisa ser verificada em blocos try catch.
	 * @throws ExecucaoTransacaoException Caso seu código (método executar da transação) resulte em erro, neste caso a base será revertida para o estado anterior
	 * a transação, a transação não é executada pela metade, ou é executada totalmente ou a base é revertida e na sequencia esta exception é lançada.
	 */
	private <E extends Throwable> Object executarTransacao(Transacao<M, E> transacao) throws E, GravacaoEmDiscoException, LeituraEmDiscoException, ExecucaoTransacaoException  {
		validarReplica();
		validarInicializacao();
		validarFinalizacao();		
		Object retorno = null;
		File arquivoTransacaoTemporaria = null;
		File arquivoTransacao = null;
		synchronized (pojoRegistro) {			
			transacao.validar(pojoRegistro);//Exception aqui não reverte a base e ainda não gravou transação.
			try {//Escreve uma transação temporária em disco
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				new ObjectOutputStream(baos).writeObject(transacao);
				arquivoTransacaoTemporaria = new File(obterDiretorioTransacoes(), (ultimaTransacaoExecutada + 1) + ".tmp");
				Files.write(arquivoTransacaoTemporaria.toPath(), baos.toByteArray(), StandardOpenOption.CREATE);
			} catch (Exception e) {
				excluirArquivosTransacao(arquivoTransacaoTemporaria, arquivoTransacao);
				throw new GravacaoEmDiscoException("Erro ao gravar arquivo temporário da transação: " + transacao, e);
			}
			try  {//Atualiza seu modelo (POJO)
				retorno = atualizarPojoUnico(transacao);//Consultas não transacionais já pegam as alterações em memória				
				ultimaTransacaoExecutada++;//agora memória considera como executado
			} catch (Exception e) {
				excluirArquivosTransacao(arquivoTransacaoTemporaria, arquivoTransacao);
				carregarDadosDoSistemaDeArquivo();//Reversão da base
				throw new ExecucaoTransacaoException("Erro ao executar a transação: " + transacao, e);
			}
			try {//Renomeia a transação temporária para definitiva
				arquivoTransacao = new File(obterDiretorioTransacoes(), ultimaTransacaoExecutada + ".bin");
				boolean ok = arquivoTransacaoTemporaria.renameTo(arquivoTransacao);//Sistema de arquivos considera executado
				if (!ok) {
					excluirArquivosTransacao(arquivoTransacaoTemporaria, arquivoTransacao);
					carregarDadosDoSistemaDeArquivo();//Reversão da base
					throw new GravacaoEmDiscoException("Erro ao renomear o temporário da transação: " + transacao);
				}
			}	catch (Exception e) {
				excluirArquivosTransacao(arquivoTransacaoTemporaria, arquivoTransacao);
				carregarDadosDoSistemaDeArquivo();//Reversão da base
				throw new GravacaoEmDiscoException("Erro ao renomear o temporário da transação: " + transacao, e);
			}
			try {//Renomeia o arquivo de orientação das réplicas
				atualizarOrientacaoReplicas();
			} catch (Exception e) {
				excluirArquivosTransacao(arquivoTransacaoTemporaria, arquivoTransacao);
				carregarDadosDoSistemaDeArquivo();//Reversão da base
				throw new GravacaoEmDiscoException("Erro ao gravar orientação das réplicas: " + transacao, e);
			}
		}
		momentoInicializacaoOuUltimaTransacao = System.currentTimeMillis();
		notificarObservadores(transacao);
		return copiarObjeto(retorno, transacao, null);
	}
	/**
	 * Copia o objeto informado utilizando Serialização Java. Atenção: caso execute uma cópia da instância raíz, o dobro de memória já consumida será necessária!
	 * @param <O> Seu objeto.
	 * @param objeto Objeto que será copiado.
	 * @param transacao Transação que solicitou a cópia ou NULL caso não seja.
	 * @param consulta Consulta que solicitou a cópia ou NULL caso não seja.
	 * @return Cópia do seu objeto.
	 */
	@SuppressWarnings("unchecked")
	private static<O> O copiarObjeto(O objeto, Transacao<? extends Serializable, ? extends Throwable> transacao, Consulta<? extends Object, ? extends Serializable> consulta) {		
		if ((!protegerRetornoCopiandoObjetos) || (objeto == null)) {
			return objeto;//Melhor saída!
		}
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);			
			oos.writeObject(objeto);
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
			return (O) ois.readObject();
		} catch (Exception e) {
			Object resposavelExecucao = (transacao != null) ? transacao : consulta;
			throw new IllegalArgumentException("Erro ao copiar o objeto " + objeto + " utilizando serialização Java. Verifique se o retorno de sua transação ou consulta " + resposavelExecucao + " é serializável.", e);
		}

	}
	/**
	 * Em caso de falha na execução da transação os arquivos .tmp e .bin são excluídos do sistema de arquivos.
	 * @param arquivoTransacaoTemporaria Arquivo temporário
	 * @param arquivoTransacao Arquivo definitivo.
	 */
	private void excluirArquivosTransacao(File arquivoTransacaoTemporaria, File arquivoTransacao) {
		if ((arquivoTransacaoTemporaria != null) && arquivoTransacaoTemporaria.exists()) {
			boolean ok = arquivoTransacaoTemporaria.delete();
			if (!ok) {
				logger.log(Level.SEVERE, "Erro ao excluir o arquivo temporário de transação" + arquivoTransacaoTemporaria.getName() + ", exclua manualmente este arquivo.");
			}
		}
		if ((arquivoTransacao != null) && arquivoTransacao.exists()) {
			boolean ok = arquivoTransacao.delete();			
			if (!ok) {
				finalizarPrevalencia();
				throw new GravacaoEmDiscoException("Erro ao excluir o arquivo " + arquivoTransacao.getName() + ", exclua manualmente este arquivo.");
			}
		}
	}
	/**
	 * Atualiza seu modelo executando sua transação.
	 * @param transacao A transação que atualizará o modelo.
	 * @return O seu retorno, caso seja uma transação com retorno, caso contrário null.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <E extends Throwable> Object atualizarPojoUnico(Transacao<M, E> transacao) {
		if (transacao instanceof TransacaoSemRetorno) {
			((TransacaoSemRetorno) transacao).executar(pojoRegistro);
			return null;
		}
		return ((TransacaoComRetorno) transacao).executar(pojoRegistro);
	}
	
	/**
	 * Durante a inicialização e reversão, para evitar que todas transações sejam re-executadas, é possível contornar este processo criando um arquivo que é uma foto (idem snapshot Prevayler) do 
	 * seu modelo (pojoRegistro), desta forma somente as transações subsequentes a atualização deste arquivo precisam ser reexecutadas para inicialização de sua prevalência.
	 * Só é necessária a execução manual deste método quando a classe foi configurada com NULL para quantidade de segundos de inatividade para gravação do acelerador, caso tenha 
	 * informado uma quantidade de segundos, a gravação se torna automática e periódica assim que houver inatividade transacional.
	 * É interessante atualizar este arquivo quando seu sistema não estiver em uso, pois, o bloco sincronizado impede que transações sejam executadas até o término da gravação.
	 * OBS: o funcionamento da prevalência sem o arquivo acelerador implica em inicializações reversões mais lentas em ordens de grandeza, conforme quantidade de transações.
	 * @throws GravacaoEmDiscoException Caso não seja possível gravar o arquivo acelerador.
	 */
	public void atualizarArquivoAceleradorInicializacao() throws GravacaoEmDiscoException {		
		if (ultimaTransacaoExecutada == ultimaTransacaoAcelerada) {
			return;
		}
		validarReplica();
		validarInicializacao();
		validarFinalizacao();
		File arquivoAceleradorAntigo = null;
		if (obterDiretorioAcelerador().list().length > 0) {
			arquivoAceleradorAntigo = obterDiretorioAcelerador().listFiles()[0];
		}
		synchronized (pojoRegistro) {
			File arquivoNovo = null;
			try {
				arquivoNovo = new File(obterDiretorioAcelerador(), ultimaTransacaoExecutada + ".bin");
				arquivoNovo.createNewFile();
				ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(arquivoNovo), 8192000));//Se desejar pode ajustar o tamanho do buffer para suas gravações
				oos.writeObject(pojoRegistro);
				oos.flush();
				oos.close();
				ultimaTransacaoAcelerada = ultimaTransacaoExecutada;
			} catch (Exception e) {
				String textoExclusaoArquivo = ".";
				if (arquivoNovo != null) {
					textoExclusaoArquivo = (arquivoNovo.delete()) ? "." : ", excluir o arquivo acelerador '" + arquivoNovo.getName() + "' manualmente.";
				}
				throw new GravacaoEmDiscoException("Erro ao gravar o arquivo de acelerador de inicialização" + textoExclusaoArquivo, e);
			}
		}
		if (arquivoAceleradorAntigo != null) {			
			if (!arquivoAceleradorAntigo.delete()) {
				throw new GravacaoEmDiscoException("Erro ao excluir o arquivo acelerador antigo, é necessário excluir manualmente o arquivo acelerador " + arquivoAceleradorAntigo.getName());
			}
		}
		if (apagarTransacoesAceleradas) {
			apagarTransacoesInternalizadasNoArquivoAcelerador();
		}
	}
	/**
	 * Apaga todas transações que ja foram internalizadas no arquivo acelerador. A configucação padrão executa este método logo após a gravação do arquivo acelerador.
	 * @throws GravacaoEmDiscoException Caso não seja possível apagar todos arquivos de transação.
	 */
	private void apagarTransacoesInternalizadasNoArquivoAcelerador() throws GravacaoEmDiscoException {		
		validarReplica();
		validarInicializacao();
		validarFinalizacao();
		FileFilter filtroArquivoPorNumero = new FileFilter() {
			@Override
			public boolean accept(File file) {
				if (!file.getName().contains(".bin")) {					
					return false;
				}
				return converterNomeArquivo(file) <= ultimaTransacaoAcelerada;
			}
		};		
		for (File transacaoApagarLoop : obterDiretorioTransacoes().listFiles(filtroArquivoPorNumero)) {
			if (!transacaoApagarLoop.delete()) {
				throw new GravacaoEmDiscoException("Não foi possível apagar o arquivo de transação " + transacaoApagarLoop.getName() + ". Verififique as pemissões do arquivo e tente nomamente.");
			}
		}
	}
	/**
	 * Monitor para disparar a gravação automática do arquivo acelerador.
	 */
	private void iniciarMonitorGravacaoAutomaticaArquivoAcelerador() {
		new Thread() {
			public void run() {
				while (!finalizar) {
					try {
						sleep(1000);
						if (finalizar) {
							return;
						}
						Long tempoDecorridoDesdeUltimaTransacao = System.currentTimeMillis() - momentoInicializacaoOuUltimaTransacao;
						if (tempoDecorridoDesdeUltimaTransacao >= milisegundosInatividadeTransacionalDispararGravacaoAcelerador) {
							atualizarArquivoAceleradorInicializacao();
						}
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Erro ao gravar arquivo acelerador no monitor.", e);
					}
				}
			}
		}.start();
	}
	/**
	 * Atualiza o contador de transações executadas em sistema de arquivos para orientar as réplicas a executarem novas transações.
	 * @throws GravacaoEmDiscoException Quando não for possível criar ou renomear o arquivo de orientação das réplicas. 
	 */
	private void atualizarOrientacaoReplicas() throws GravacaoEmDiscoException {
		File dirReplicas = obterDiretorioReplica();		
		boolean ok = false;
		if (dirReplicas.list().length == 0) {
			File arquivoOrientacaoReplicas = new File(obterDiretorioReplica(), "" + ultimaTransacaoExecutada);
			try {
				ok = arquivoOrientacaoReplicas.createNewFile();
			} catch (IOException e) {
				throw new GravacaoEmDiscoException("Erro ao criar o arquivo de orientação de réplicas.", e);
			}			
		} else {
			File arquivoOrientacaoReplicas = dirReplicas.listFiles()[0]; 
			ok = arquivoOrientacaoReplicas.renameTo(new File(obterDiretorioReplica(), "" + ultimaTransacaoExecutada));
		}
		if (!ok) {
			throw new GravacaoEmDiscoException("Não foi possível criar/renomear o arquivo de orientação das réplicas, verifique as permissões no sistema de arquivos.");
		}
	}
	/**
	 * Inicializa um ouvinte para atualizar os dados em memória sempre que um novo registro for adicionado ao sistema de arquivos. <br>
	 * OBS: somente réplicas possuem estes ouvintes.<br>
	 * ATENÇÂO: a instância da réplica deve conter as versões das mesmas classes (POJOs e Transações) utilizadas na gravação para possibilitar a desserialização Java.
	 * @throws IOException Caso ocorra um problema na leitura do arquivo.
	 * @throws InterruptedException Caso ocorra um problema no ouvinte do sistema de arquivos.
	 */
	private void criarOuvinteGravacaoParaExecutarTransacoes() {
		new Thread() {			
			public void run() {
				try {
					File dirReplica = obterDiretorioReplica();
					File dirAcelerador = obterDiretorioAcelerador();
					while (!finalizar) {						
						sleep(100);
						Long contadorArquivoAcelerador = (dirAcelerador.list().length > 0) ? converterNomeArquivo(dirAcelerador.listFiles()[0]) : 0L;
						if (contadorArquivoAcelerador > ultimaTransacaoExecutada) {
							//Quando ocorre a gravação do arquivo acelerador os arquivos de transações pode ter sido excluídos. Observadores de escravos não recebem aviso...
							carregarDadosDoSistemaDeArquivo();
						}
						Long contadorTransacoesArquivo = (dirReplica.list().length > 0) ? converterNomeArquivo(dirReplica.listFiles()[0]) : 0L;
						if (!contadorTransacoesArquivo.equals(ultimaTransacaoExecutada)) {
							//Os escravos precisam ler as transações antes de serem excluídas, por isso o tempo mínimo é 10 segundos para apagar arquivos de transação
							executarTransacoesPendentesCarregamento();
						}
					}
				} catch (Exception e) {
					finalizarPrevalencia();
					logger.log(Level.SEVERE, "Erro ao executar transações na réplica, instância comprometida, os dados não correspondem mais a instância primária.", e);					
				}
			}
		}.start();
	}
	
	/**
	 * Carrega seu modelo executando todas transações gravadas em sistema de arquivo, utilizado para iniciar a prevalência e também para reversão (roolback) da base.
	 * O arquivo acelerador é utilizado durante o processo de inicialização para carregar a base até o momento de sua gravação, e na sequencia  todas transações posteriores 
	 * são executadas, levando a base ao último estado gravado em arquivo. Quando não existir um arquivo acelerador, todas transações são reexecutadas até base ser carregada para última versão.
	 * Quanto mais transações gravadas, maior o tempo para reexecutá-las, assim quanto maior a base mais tempo será necessário para executar sua leitura e execução completa. Assim
	 * considere o uso da atualização do arquivo acelerador com uma frequencia padrão ou adequada ao seu negócio. Ex: uma base com acelerador de 1.4 gb demora 40 segundos para ser carrada.  
	 * Backup frequente das transações, aceleradores e sequencias são importantes para todas as bases.
	 * @throws GravacaoEmDiscoException Caso a estrutura inicial não possa ser criada.
	 * @throws LeituraEmDiscoException Caso ocorra um problema na leitura ou desserialização Java. Sugiro fortemente o uso do SerialVersionUid mantendo o mesmo número para versões diferentes 
	 * mas compativeis no modelo, quando não for mais  possível a compatibilidade, uma nova classe deve ser criada e os registros devem ser migrados de um tipo para outro (novo) via transação 
	 * em código Java, também incremente SerialVersionUid para que todas replicas sejam forçadas a atualizarem seus condigos fontes. Também sugiro que o código fonte de transações que ja foram 
	 * executadas não sejam mais alterados, ao custo de inutilização da base.
	 * @throws ExecucaoTransacaoException Caso a execução do método executar de uma transação resulte em erro.
	 */
	private synchronized void carregarDadosDoSistemaDeArquivo() throws GravacaoEmDiscoException, LeituraEmDiscoException, ExecucaoTransacaoException {		
		System.out.println("Iniciando carregamento...");
		inicializando = true;
		ultimaTransacaoExecutada = 0L;
		pojoRegistro = construirNovoPojoUnico();
		lerArquivoAceleradorInicializacao();		
		executarTransacoesPendentesCarregamento();
		inicializando = false;
		System.out.println("Carregamento OK!");
	}
	@SuppressWarnings("unchecked")
	private void executarTransacoesPendentesCarregamento() {		
		FileFilter filtroArquivoPorNumero = new FileFilter() {
			@Override
			public boolean accept(File file) {
				if (!file.getName().contains(".bin")) {					
					return false;
				}
				return converterNomeArquivo(file) > ultimaTransacaoExecutada;
			}
		};
		List<File> arquivosTransacao = Stream.of(obterDiretorioTransacoes().listFiles(filtroArquivoPorNumero)).parallel().sorted(fileNomeNumeroTransacao).collect(Collectors.toList());		
		for (File arquivoTransacao : arquivosTransacao) {
			Long numeroTransacao = converterNomeArquivo(arquivoTransacao);						
			try {
				Transacao<M, Throwable> transacao = (Transacao<M, Throwable>) new ObjectInputStream(new ByteArrayInputStream(Files.readAllBytes(arquivoTransacao.toPath()))).readObject(); 
				atualizarPojoUnico(transacao);
				ultimaTransacaoExecutada = numeroTransacao;
				if (replica && !inicializando) {
					notificarObservadores(transacao);
				}
				System.out.println("Arquivo transacao executado = " + numeroTransacao);
			} catch (Exception e) {
				finalizarPrevalencia();
				throw new LeituraEmDiscoException("Erro ao ler arquivo de transação " + arquivoTransacao.getName() + ", verifique se o arquivo está acessível ou se alguma transação se tornou incompatível com a desserialização após sua modificação.", e);
			}
		}		
	}
	@SuppressWarnings("unchecked")
	private void lerArquivoAceleradorInicializacao() throws LeituraEmDiscoException {
		if (obterDiretorioAcelerador().list().length == 0) {
			return;
		}		
		try {
			File arquivoAcelerador = obterDiretorioAcelerador().listFiles()[0];
			Long ultimaTransacaoAceleradaArquivo = converterNomeArquivo(arquivoAcelerador);
			ObjectInputStream oins = new ObjectInputStream(new BufferedInputStream(new FileInputStream(arquivoAcelerador), 8192000));
			pojoRegistro = (M) oins.readObject();		
			ultimaTransacaoExecutada = ultimaTransacaoAceleradaArquivo;
			ultimaTransacaoAcelerada = ultimaTransacaoAceleradaArquivo;
			System.out.println("Ultima transacao acelerada = " + ultimaTransacaoExecutada);
			oins.close();			
		} catch (Exception e) {
			finalizarPrevalencia();
			throw new LeituraEmDiscoException("Erro ao ler arquivo acelerador, verifique se o arquivo está acessível ou se alguma entidade se tornou incompatível com a desserialização após sua alteração, ele também pode ser excluído ou atualizado.", e);
		}		
	}
	
	/**
	 * Converte para um número o nome do arquivo.
	 * @param arquivo O arquivo com nome numérico sequencial.
	 * @return o valor do número contido no nome do arquivo.
	 */
	private Long converterNomeArquivo(File arquivo) {
		return Long.parseLong(arquivo.getName().replace(".bin", ""));
	}
	
	/**
	 * Obtém o diretório base conforme configuração inicial.
	 * @throws GravacaoEmDiscoException Caso o diretório base não exista.
	 */
	private File obterDiretorioBase() throws GravacaoEmDiscoException {
		File diretorioBase = new File(diretorio);
		if (!diretorioBase.exists()) {
			throw new GravacaoEmDiscoException("O diretório " + diretorio + " não existe.");
		}
		return diretorioBase;
	}
	private File obterDiretorioRegistro() throws GravacaoEmDiscoException {
		return obterSubDiretorio(obterDiretorioBase(), pojoRegistro.getClass().getCanonicalName());
	}
	/**
	 * Obtém o subdiretório, criando caso ainda não exista.
	 * @param diretorioBase O diretório base onde se encontra o subdiretório.
	 * @param nomeDiretorio O nome do diretório.
	 * @return O subdiretório desejado.
	 * @throws GravacaoEmDiscoException Caso não exista o diretório base.
	 */
	private File obterSubDiretorio(File diretorioBase, String nomeDiretorio) throws GravacaoEmDiscoException {
		File diretorio = new File(diretorioBase, nomeDiretorio);
		if (!diretorio.exists()) {
			diretorio.mkdir();
		}
		return diretorio;
	}
	private File obterDiretorioTransacoes() throws GravacaoEmDiscoException {
		return obterSubDiretorio(obterDiretorioRegistro(), "TRANSACOES");		
	}
	private File obterDiretorioReplica() throws GravacaoEmDiscoException {
		return obterSubDiretorio(obterDiretorioRegistro(), "REPLICAS");		
	}
	private File obterDiretorioSeguencia() throws GravacaoEmDiscoException {
		return obterSubDiretorio(obterDiretorioRegistro(), "SEQUENCIAS");
	}
	private File obterDiretorioAcelerador() throws GravacaoEmDiscoException {
		return obterSubDiretorio(obterDiretorioRegistro(), "ACELERADOR");
	}
	/**
	 * Registra o observador que receberá notificação de cada transação executada. 
	 * @param observador Observador de transações executadas.
	 */
	public void registrarObservador(Observador<M> observador) {		
		observadores.add(observador);		
	}
	/**
	 * Remove um observador.
	 * @param observador O observador que será removido.
	 */
	public void removerObservador(Observador<M> observador) {
		observadores.remove(observador);
	}
	/**
	 * Avisa os observadores que uma transação foi executada.
	 * @param transacao A transação que foi executada.
	 */
	private void notificarObservadores(Transacao<M, ? extends Throwable> transacao) {
		new Thread() {
			 public void run() {
				 for (Observador<M> observadorLoop : observadores) {
					 try {
						 observadorLoop.receberAvisoExecucao(transacao);
					 } catch (Exception e) {
						 logger.log(Level.WARNING, "Erro ao notificar transação " + transacao + " para o observador " + observadorLoop, e);
					}
				 }
			 }	
		}.start();		
	}	
	
	/**
	 * Obtém um número sequencial, útil para identificar objetos, para classe de entidade informada, a cada chamada um contador em arquivo é incrementado para cada classe. 
	 * Para atualizar um objeto, é necessário identificá-lo como único, não obrigatóriamente com um número sequencial, pode ser adotado UUID, chaves
	 * naturais ou qualquer outra técnica que identifique o objeto como único a cada reinicialização da base.
	 * @param <Z> Uma classe de seu modelo.
	 * @param classe A classe de sua entidade serializável.
	 * @return O valor atual da sequencia que ainda não foi utilizado.
	 * @throws GravacaoEmDiscoException Caso ocorra um problema na gravação da sequência.
	 * @throws LeituraEmDiscoException Caso ocorra um erro na leitura da sequência.
	 */
	public <Z extends Serializable> Long obterProximoIdSequencial(Class<Z> classe) throws GravacaoEmDiscoException, LeituraEmDiscoException {
		validarReplica();
		validarInicializacao();
		validarFinalizacao();
		synchronized (classe) {
			File arquivoSequencia = new File(obterDiretorioSeguencia(), classe.getCanonicalName() + ".seq");
			Long retorno = 1L;
			if (arquivoSequencia.exists()) {
				try {
					retorno = Long.parseLong(new String(Files.readAllBytes(arquivoSequencia.toPath()))) + 1;
				} catch (NumberFormatException | IOException e) {
					throw new LeituraEmDiscoException("Erro ao ler o arquivo de sequencia " + arquivoSequencia.getName(), e);
				}
			}
			try {
				Files.write(arquivoSequencia.toPath(), retorno.toString().getBytes(), StandardOpenOption.CREATE);
			} catch (IOException e) {
				throw new GravacaoEmDiscoException("Erro ao escrever o valor " + retorno + " no arquivo de sequencia " + arquivoSequencia.getName(), e);
			}
			return retorno;			
		}
	}
	/**
	 * Descarta essa instância de prevalência impedindo operações, util para finalizar com segurança, impedindo que a gravação de uma transação (apesar de muito rápida) seja 
	 * interrompida antes de sua conclusão.     
	 */
	public void finalizarPrevalencia() {
		finalizar = true;//Impede a execução de novas transações e operações, não interferindo na gravação corrente (se houver).
		mapaPrevalencias.remove(tipoRegistro);
		synchronized (pojoRegistro) {
			pojoRegistro = null;			
		}
		System.gc();
	}
	/**
	 * Impede operações com uma exception quando a instância é finalizada.
	 */
	private void validarFinalizacao() {
		if (finalizar) {
			throw new IllegalStateException("Instância finalizada, obtenha uma nova instância.");
		}
	}
	/**
	 * Impede operações com uma exception enquanto a instância esta sendo iniciada ou reiniciada (roolback).
	 */
	private void validarInicializacao() {
		if (inicializando) {
			throw new IllegalStateException("Inicializando instância, aguarde alguns segundos e tente novamente.");
		}
	}
	/**
	 * Impede operações em instâncias réplicas.
	 */
	private void validarReplica() {
		if (replica) {
			throw new IllegalStateException("Operação proibida para réplicas.");
		}
	}	
	/**
	 * Obtém o identificador sequencial da última transação executada.
	 * @return O identificador sequencial da última transação.
	 */
	public Long getIdentificadorUltimaTransacaoExecutada() {
		validarFinalizacao();
		return ultimaTransacaoExecutada;
	}
	/**
	 * Obtém o identificador sequencial da última transação internalizada pelo arquivo acelerador.
	 * @return O identificador sequencial da última transação internalizada.
	 */
	public Long getIdentificadorUltimaTransacaoAcelerada() {
		validarFinalizacao();		
		return ultimaTransacaoAcelerada;
	}
	
}