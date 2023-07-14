package testes.entidades.auxbrasil;

import java.io.Serializable;

public class Cidade implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private Estado estado;
	private String nome;
	
	public Estado getEstado() {
		return estado;
	}
	public void setEstado(Estado estado) {
		this.estado = estado;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	
}