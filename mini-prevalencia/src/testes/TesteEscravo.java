package testes;

import java.util.Scanner;

import br.tec.mboi.api.MiniPrevalencia;
import br.tec.mboi.api.MiniPrevalencia.Observador;
import br.tec.mboi.api.MiniPrevalencia.Transacao;
import testes.consultas.exemplo.ListarNomesPessoas;
import testes.consultas.exemplo.ObterNomeExemploModelo;
import testes.entidades.exemplo.ExemploModelo;

public class TesteEscravo implements Observador<ExemploModelo> {

	private static final String DIRETORIO = System.getProperty("user.home") + "/ExemploModelo_Transacao";
	
	static {
		//MiniPrevalencia.configurarReplica(DIRETORIO, true);
		MiniPrevalencia.configurarReplica(DIRETORIO);
	}
	
	private int contador = 0;
	
	public static void main(String[] args) throws InterruptedException {
		System.out.println("Inciando Escravo");
		TesteEscravo observador = new TesteEscravo();
		System.out.println("Inciando prevalencia");
		MiniPrevalencia<ExemploModelo> prevalencia = MiniPrevalencia.prevalecer(ExemploModelo.class);
		prevalencia.registrarObservador(observador);
		System.out.println("QTD pessoas: " + prevalencia.executar(new ListarNomesPessoas()).size());
		System.out.println("Prevalencia Iniciada");
		System.out.println("Observador registrado");
		Scanner scanner = new Scanner(System.in);	
		System.out.println("Digite 'fim' para teminar");
		while(!scanner.nextLine().equalsIgnoreCase("fim")) {
			System.out.println("executando");
		}
		scanner.close();		
		prevalencia.removerObservador(observador);
		prevalencia.finalizarPrevalencia();
		System.out.println("Finalizado");
		System.exit(0);
	}

	@Override
	public synchronized void receberAvisoExecucao(Transacao<ExemploModelo, ? extends Throwable> transacao) {		
		contador++;
		System.out.println("\n\n");
		System.out.println("Evento " + contador + " -------- ");
		MiniPrevalencia<ExemploModelo> prevalencia = MiniPrevalencia.prevalecer(ExemploModelo.class);
		System.out.println("Transação observada = " + transacao);
		System.out.println("Nome modelo = " + prevalencia.executar(new ObterNomeExemploModelo()));
		System.out.println("QTD Pessoas = " + prevalencia.executar(new ListarNomesPessoas()).size());
		System.out.println("----------------------------------");
	}

}
