package testes.entidades.auxbrasil;

public class Negocio {

	public Estado obterEstado(AuxilioBrasil auxilioBrasil, String sigla) {
		if (auxilioBrasil.getEstados().containsKey(sigla)) {
			return auxilioBrasil.getEstados().get(sigla);
		}
		Estado estado = new Estado();
		estado.setSigla(sigla);
		auxilioBrasil.getEstados().put(sigla, estado);
		return estado;
	}
	
	public Cidade obterCidade(AuxilioBrasil auxilioBrasil, String nome, String siglaEstado) {
		if (auxilioBrasil.getCidades().containsKey(nome + "-" + siglaEstado)) {
			return auxilioBrasil.getCidades().get(nome);
		}
		Estado estado = obterEstado(auxilioBrasil, siglaEstado);
		Cidade cidade = new Cidade();
		cidade.setEstado(estado);
		cidade.setNome(nome);
		auxilioBrasil.getCidades().put(nome + "-" + siglaEstado, cidade);
		return cidade;
	}
	
	
	
}
