package testes;

import java.util.ArrayList;
import java.util.List;

import br.tec.mboi.api.MiniPrevalencia;
import testes.consultas.auxbrasil.ConsultarBeneficiadosPorNomeFonetico;
import testes.consultas.auxbrasil.ContarBeneficiados;
import testes.consultas.auxbrasil.ListarBeneficiosPorCidade;
import testes.consultas.auxbrasil.SomarTotal;
import testes.consultas.auxbrasil.TotalizarValoresBeneficiosPorCidade;
import testes.entidades.auxbrasil.AuxilioBrasil;
import testes.util.MetaphonePtBrFrouxo;
import testes.util.ThreadConsulta;

public class TesteConcorrenciaConsultasAuxilioBrasil {
		
	public static void main(String[] args) throws Exception {
		System.out.println("Iniciando teste de concorrência em consultas... ");
		String DIRETORIO = System.getProperty("user.home") + "/AUX_BRASIL_TRANSACAO";
		MiniPrevalencia.configurar(DIRETORIO);
		MiniPrevalencia.prevalecer(AuxilioBrasil.class);//Força o carregamento em memória antes das threads iniciarem as consultas
		int quantidadeThreadsPorTipoConsulta = 2;//Mutiplicar por 5 p/ total de threads
		int repeticoesPorThread = 10;
		long tempoMinimoSlepAleatorioLoopThread = 100;
		long tempoMaximoSlepAleatorioLoopThread = 1000; 
		long tempoMaximoEspera = 5000L;
		List<ThreadConsulta> threadsConsutas = new ArrayList<ThreadConsulta>();
		String codRicardo = new MetaphonePtBrFrouxo("Ricardo").toString();
		String codSilva = new MetaphonePtBrFrouxo("Silvar").toString();
		System.out.println("Instanciando threads...");
		for (int i = 0; i < quantidadeThreadsPorTipoConsulta; i++) {
			threadsConsutas.add(new ThreadConsulta(new ConsultarBeneficiadosPorNomeFonetico(codRicardo, codSilva), tempoMaximoEspera, repeticoesPorThread, tempoMinimoSlepAleatorioLoopThread, tempoMaximoSlepAleatorioLoopThread));
		}
		for (int i = 0; i < quantidadeThreadsPorTipoConsulta; i++) {
			threadsConsutas.add(new ThreadConsulta(new ContarBeneficiados(), tempoMaximoEspera, repeticoesPorThread, tempoMinimoSlepAleatorioLoopThread, tempoMaximoSlepAleatorioLoopThread));
		}
		for (int i = 0; i < quantidadeThreadsPorTipoConsulta; i++) {
			threadsConsutas.add(new ThreadConsulta(new ListarBeneficiosPorCidade("PARANAVAI", "PR"), tempoMaximoEspera, repeticoesPorThread, tempoMinimoSlepAleatorioLoopThread, tempoMaximoSlepAleatorioLoopThread));
		}
		for (int i = 0; i < quantidadeThreadsPorTipoConsulta; i++) {
			threadsConsutas.add(new ThreadConsulta(new SomarTotal(), tempoMaximoEspera, repeticoesPorThread, tempoMinimoSlepAleatorioLoopThread, tempoMaximoSlepAleatorioLoopThread));
		}
		for (int i = 0; i < quantidadeThreadsPorTipoConsulta; i++) {
			threadsConsutas.add(new ThreadConsulta(new TotalizarValoresBeneficiosPorCidade(), tempoMaximoEspera, repeticoesPorThread, tempoMinimoSlepAleatorioLoopThread, tempoMaximoSlepAleatorioLoopThread));
		}
		System.out.println("Iniciando threads...");
		long tempoInicialTeste = System.currentTimeMillis();
		for (Thread threadLoop : threadsConsutas) {
			threadLoop.start();
		}
		System.out.println("Aguardando threads...");
		for (Thread threadLoop : threadsConsutas) {
			threadLoop.join();
		}
		System.out.println("Verificando sucesso...");
		boolean sucesso = true;
		for (ThreadConsulta threadLoop : threadsConsutas) {
			if (threadLoop.getErro() != null) {
				sucesso = false;
			}
			System.out.println(threadLoop + " erro = " + threadLoop.getErro());
		}				
		long tempoDecorrido = System.currentTimeMillis() - tempoInicialTeste;
		System.out.println("\n");
		System.out.println("Sucesso = " + sucesso);
		System.out.println("Teste de " + (quantidadeThreadsPorTipoConsulta * 5) + " thread com " + repeticoesPorThread + " repetições em cada uma, durou " + tempoDecorrido + " ms");
		System.out.println("Fim teste de concorrência em consultas!");
		System.exit(0);
	}
	
}