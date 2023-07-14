package testes.transacoes.exemplo;

import java.io.Serializable;
import java.util.Date;

public class PessoaVO implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private Long id;
	private String nome;
	private String email;
	private Date dataNascimento;
	
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
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PessoaVO [id=");
		builder.append(id);
		builder.append(", nome=");
		builder.append(nome);
		builder.append(", email=");
		builder.append(email);
		builder.append(", dataNascimento=");
		builder.append(dataNascimento);
		builder.append("]");
		return builder.toString();
	}
	
}