package testes.transacoes.exemplo;

import br.tec.mboi.api.MiniPrevalencia.TransacaoComRetorno;
import testes.entidades.exemplo.ExemploModelo;
import testes.exceptions.ValidacaoCampoException;

public class BaixarEstoque implements TransacaoComRetorno<Integer, ExemploModelo, ValidacaoCampoException> {
	private static final long serialVersionUID = 1L;
	
	private Integer quantidade;
	
	public BaixarEstoque(Integer quantidade) {
		this.quantidade = quantidade;
	}

	@Override
	public void validar(ExemploModelo pojoUnico) throws ValidacaoCampoException {		
		if (calcular(pojoUnico) < 0) {
			throw new ValidacaoCampoException("estoque", "Quantidade inválida!");
		}
		
	}

	@Override
	public Integer executar(ExemploModelo pojoUnico) {
		Integer resultado = calcular(pojoUnico);
		pojoUnico.setEstoque(resultado);
		return resultado;
	}

	private Integer calcular(ExemploModelo pojoUnico) {
		return pojoUnico.getEstoque() - quantidade;
	}
	
}