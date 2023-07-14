package testes.transacoes.auxbrasil;

import br.tec.mboi.api.MiniPrevalencia.TransacaoSemRetorno;
import testes.entidades.auxbrasil.AuxilioBrasil;
import testes.entidades.auxbrasil.Cidade;
import testes.entidades.auxbrasil.Estado;
import testes.exceptions.JaCadastradoException;

public class AdicionarCidadeSeNaoExistir implements TransacaoSemRetorno<AuxilioBrasil, JaCadastradoException> {

	private static final long serialVersionUID = 1L;

	private String siglaEstado;
	private String nomeCidade;
	
	public AdicionarCidadeSeNaoExistir(String siglaEstado, String nomeCidade) {
		this.siglaEstado = siglaEstado;
		this.nomeCidade = nomeCidade;
	}

	@Override
	public void validar(AuxilioBrasil pojoRegistro) throws JaCadastradoException {
		if (pojoRegistro.getCidades().containsKey(padronizarChaveCidade(nomeCidade, siglaEstado))) {
			throw new JaCadastradoException();
		}
		if (!pojoRegistro.getEstados().containsKey(siglaEstado)) {
			throw new RuntimeException("Estado " + siglaEstado + " não cadastrado!");
		}
		if ((nomeCidade == null) || nomeCidade.isBlank()) {
			throw new RuntimeException("Nome da cidade inválido!");
		}
	}

	@Override
	public void executar(AuxilioBrasil pojoRegistro) {
		String chaveCidade = padronizarChaveCidade(nomeCidade, siglaEstado);		
		Estado estado = pojoRegistro.getEstados().get(siglaEstado);
		Cidade cidade = new Cidade();
		cidade.setEstado(estado);
		cidade.setNome(padronizarNomeCidade(nomeCidade));
		pojoRegistro.getCidades().put(chaveCidade, cidade);				
	}
	
	public static String padronizarNomeCidade(String nome) {
		return nome.trim().toUpperCase();
	}
	
	public static String padronizarChaveCidade(String nome, String siglaUf) {
		return padronizarNomeCidade(nome) + "-" + AdicionarUfSeNaoExistir.padronizarSigla(siglaUf);
	}
	
}