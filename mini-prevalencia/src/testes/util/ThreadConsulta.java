package testes.util;

import java.util.Random;

import br.tec.mboi.api.MiniPrevalencia;
import br.tec.mboi.api.MiniPrevalencia.Consulta;
import testes.entidades.auxbrasil.AuxilioBrasil;

public class ThreadConsulta extends Thread {
	private Consulta<? extends Object, AuxilioBrasil> consulta;
	private long milissegundosMaximo = 1000;
	private long milissegundosMinimoSleep = 100;
	private long milissegundosMaximoSleep = 1000;
	private int quantidadeRepeticoes = 50;
	private String erro = null;
	private Random random = new Random();
	
	public ThreadConsulta(Consulta<? extends Object, AuxilioBrasil> consulta, long milisegundosMaximo, int quantidadeRepeticoes, long milissegundosMinimoSleep, long milissegundosMaximoSleep) {
		this.consulta = consulta;
		this.milissegundosMaximo = milisegundosMaximo;
		this.quantidadeRepeticoes = quantidadeRepeticoes;
		this.milissegundosMinimoSleep = milissegundosMinimoSleep;
		this.milissegundosMaximoSleep = milissegundosMaximoSleep;
	}
	
	public void run() {
		MiniPrevalencia<AuxilioBrasil> prevalencia = MiniPrevalencia.prevalecer(AuxilioBrasil.class);
		for (int q = 0; q < quantidadeRepeticoes; q++) {
			try {
				sleep(random.nextLong(milissegundosMinimoSleep, milissegundosMaximoSleep));
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			long tempoIncial = System.currentTimeMillis();
			try {
				prevalencia.executar(consulta);
			} catch (Exception e) {
				erro = e.getMessage();
				return;
			}
			long tempoDecorrido = System.currentTimeMillis() - tempoIncial;
			if (tempoDecorrido > milissegundosMaximo) {
				erro = "Tempo de execução (" + consulta + ") : " + tempoDecorrido + " ms";
				return;
			}
		}
	}
	public String getErro() {
		return erro;
	}
	 
}