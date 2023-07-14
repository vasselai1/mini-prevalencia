package testes.entidades.exemplo;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

public class Pessoa implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private Long id;
	private String nome;
	private String email;
	private Date dataNascimento;
	private Set<String> apelidos;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public Date getDataNascimento() {
		return dataNascimento;
	}
	public void setDataNascimento(Date dataNascimento) {
		this.dataNascimento = dataNascimento;
	}
	public Set<String> getApelidos() {
		return apelidos;
	}
	public void setApelidos(Set<String> apelidos) {
		this.apelidos = apelidos;
	}
	
}