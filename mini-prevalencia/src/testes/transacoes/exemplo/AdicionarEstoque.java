package testes.transacoes.exemplo;

import br.tec.mboi.api.MiniPrevalencia.TransacaoSemRetorno;
import testes.entidades.exemplo.ExemploModelo;
import testes.exceptions.ValidacaoCampoException;

public class AdicionarEstoque implements TransacaoSemRetorno<ExemploModelo, ValidacaoCampoException> {
	
	private static final long serialVersionUID = 1L;
	
	public static final int QTD_MAXIMA = 10000;
	
	
	private Integer quantidade;
	
	public AdicionarEstoque(Integer quantidade) {
		this.quantidade = quantidade;
	}

	@Override
	public void validar(ExemploModelo pojoUnico) throws ValidacaoCampoException {
		if (calcular(pojoUnico) > QTD_MAXIMA) {
			throw new ValidacaoCampoException("estoque", "Quantidade não suportada!");
		}
	}

	@Override
	public void executar(ExemploModelo pojoUnico) {
		pojoUnico.setEstoque(calcular(pojoUnico));		
	}
	
	public Integer calcular(ExemploModelo pojoUnico) {
		return pojoUnico.getEstoque() + quantidade;
	}
	
}