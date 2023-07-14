package testes.entidades.auxbrasil;

import java.io.Serializable;

public class Estado implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String sigla;
	private String nome;

	public String getSigla() {
		return sigla;
	}
	public void setSigla(String sigla) {
		this.sigla = sigla;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	
}
