package testes.transacoes.exemplo;

import br.tec.mboi.api.MiniPrevalencia.TransacaoSemRetorno;
import testes.entidades.exemplo.ExemploModelo;
import testes.exceptions.ValidacaoCampoException;

public class NomearModeloExemplo implements TransacaoSemRetorno<ExemploModelo, ValidacaoCampoException> {
	
	private static final long serialVersionUID = 1L;
	
	private String novoNome;
	
	public NomearModeloExemplo(String novoNome) {
		this.novoNome = novoNome;
	}

	@Override
	public void validar(ExemploModelo pojoUnico) throws ValidacaoCampoException {
		if ((pojoUnico.getNome() != null) && !pojoUnico.getNome().isBlank()) {
			throw new ValidacaoCampoException("nome", "O nome já possui o valor " + pojoUnico.getNome());
		}		
	}

	@Override
	public void executar(ExemploModelo pojoUnico) {
		pojoUnico.setNome(novoNome);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NomearModeloExemplo [novoNome=");
		builder.append(novoNome);
		builder.append("]");
		return builder.toString();
	}
	
}