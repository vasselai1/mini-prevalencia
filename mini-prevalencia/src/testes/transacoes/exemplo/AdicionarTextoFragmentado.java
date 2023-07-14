package testes.transacoes.exemplo;

import br.tec.mboi.api.MiniPrevalencia.TransacaoSemRetorno;
import testes.entidades.exemplo.ExemploModelo;
import testes.exceptions.ValidacaoCampoException;

public class AdicionarTextoFragmentado implements TransacaoSemRetorno<ExemploModelo, ValidacaoCampoException> {

	private static final long serialVersionUID = 1L;
	
	private String parteDoTexto;
	
	public AdicionarTextoFragmentado(String parteDoTexto) {	
		this.parteDoTexto = parteDoTexto;
	}

	@Override
	public void validar(ExemploModelo pojoUnico) throws ValidacaoCampoException {
		
	}

	@Override
	public void executar(ExemploModelo pojoUnico) {
		pojoUnico.getTextoFragmentado().add(parteDoTexto);
	}

}