package testes.entidades.exemplo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExemploModelo implements Serializable {

	private static final long serialVersionUID = 1L;

	private String nome;
	private Map<Long, Pessoa> mapaPessoasId = new HashMap<Long, Pessoa>();
	private Map<String, Pessoa> mapaPessoasEmail = new HashMap<String, Pessoa>();
	private List<String> textoFragmentado = new ArrayList<String>();
	private Integer estoque = 0;
	
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public Map<Long, Pessoa> getMapaPessoasId() {
		return mapaPessoasId;
	}
	public void setMapaPessoasId(Map<Long, Pessoa> mapaPessoasId) {
		this.mapaPessoasId = mapaPessoasId;
	}
	public Map<String, Pessoa> getMapaPessoasEmail() {
		return mapaPessoasEmail;
	}
	public void setMapaPessoasEmail(Map<String, Pessoa> mapaPessoasEmail) {
		this.mapaPessoasEmail = mapaPessoasEmail;
	}
	public List<String> getTextoFragmentado() {
		return textoFragmentado;
	}
	public void setTextoFragmentado(List<String> textoFragmentado) {
		this.textoFragmentado = textoFragmentado;
	}
	public Integer getEstoque() {
		return estoque;
	}
	public void setEstoque(Integer estoque) {
		this.estoque = estoque;
	}
	
}