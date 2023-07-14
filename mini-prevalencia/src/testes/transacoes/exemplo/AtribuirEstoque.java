package testes.transacoes.exemplo;

import br.tec.mboi.api.MiniPrevalencia.TransacaoSemRetorno;
import testes.entidades.exemplo.ExemploModelo;
import testes.exceptions.ValidacaoCampoException;

public class AtribuirEstoque implements TransacaoSemRetorno<ExemploModelo, ValidacaoCampoException> {
	private static final long serialVersionUID = 1L;

	private Integer quantidade;
	
	public AtribuirEstoque(Integer quantidade) {
		this.quantidade = quantidade;
	}

	@Override
	public void validar(ExemploModelo pojoUnico) throws ValidacaoCampoException {
	}

	@Override
	public void executar(ExemploModelo pojoUnico) {
		pojoUnico.setEstoque(quantidade);
	}

}