package testes.consultas.exemplo;

import br.tec.mboi.api.MiniPrevalencia.Consulta;
import testes.entidades.exemplo.ExemploModelo;

public class ObterNomeExemploModelo implements Consulta<String, ExemploModelo> {

	@Override
	public String executar(ExemploModelo seuPojoUnico) {
		return seuPojoUnico.getNome();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ObterNomeExemploModelo []");
		return builder.toString();
	}

}
