package testes.consultas.exemplo;

import br.tec.mboi.api.MiniPrevalencia.Consulta;
import testes.entidades.exemplo.ExemploModelo;

public class ObterEstoque implements Consulta<Integer, ExemploModelo> {

	@Override
	public Integer executar(ExemploModelo pojoUnico) {
		return pojoUnico.getEstoque();
	}

}
