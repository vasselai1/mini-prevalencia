package testes.entidades.auxbrasil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuxilioBrasil implements Serializable {

	private static final long serialVersionUID = 1L;

	private Map<String, Estado> estados = new HashMap<String, Estado>();
	private Map<String, Cidade> cidades = new HashMap<String, Cidade>(); 
	private List<Beneficio> beneficios = new ArrayList<Beneficio>();
	
	public AuxilioBrasil() {
	}
	
	public Map<String, Estado> getEstados() {
		return estados;
	}
	public void setEstados(Map<String, Estado> estados) {
		this.estados = estados;
	}
	public Map<String, Cidade> getCidades() {
		return cidades;
	}
	public void setCidades(Map<String, Cidade> cidades) {
		this.cidades = cidades;
	}
	public List<Beneficio> getBeneficios() {
		return beneficios;
	}
	public void setBeneficios(List<Beneficio> beneficios) {
		this.beneficios = beneficios;
	}
		
}