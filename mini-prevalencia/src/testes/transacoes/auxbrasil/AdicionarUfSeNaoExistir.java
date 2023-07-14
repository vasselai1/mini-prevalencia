package testes.transacoes.auxbrasil;

import br.tec.mboi.api.MiniPrevalencia.TransacaoSemRetorno;
import testes.entidades.auxbrasil.AuxilioBrasil;
import testes.entidades.auxbrasil.Estado;
import testes.exceptions.JaCadastradoException;

public class AdicionarUfSeNaoExistir implements TransacaoSemRetorno<AuxilioBrasil, JaCadastradoException> {

	private static final long serialVersionUID = 1L;

	private String sigla;
	
	public AdicionarUfSeNaoExistir(String sigla) {
		this.sigla = sigla;
	}

	@Override
	public void validar(AuxilioBrasil pojoRegistro) throws JaCadastradoException {
		if (pojoRegistro.getEstados().containsKey(padronizarSigla(sigla))) {
			throw new JaCadastradoException();
		}
	}

	@Override
	public void executar(AuxilioBrasil pojoRegistro) {		
		String siglaPadronizada = padronizarSigla(sigla);
		Estado estado = new Estado();
		estado.setSigla(siglaPadronizada);
		pojoRegistro.getEstados().put(siglaPadronizada, estado);
	}

	public static String padronizarSigla(String siglaUf) {
		return siglaUf.trim().toUpperCase();
	}
	
}