package testes.consultas.exemplo;

import br.tec.mboi.api.MiniPrevalencia.Consulta;
import testes.entidades.exemplo.ExemploModelo;
import testes.entidades.exemplo.Pessoa;

public class ObterPessoaPorId implements Consulta<Pessoa, ExemploModelo> {

	private Long id;
	
	public ObterPessoaPorId(Long id) {	
		this.id = id;
	}

	@Override
	public Pessoa executar(ExemploModelo pojoUnico) {
		return pojoUnico.getMapaPessoasId().get(id);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ObterPessoaPorId [id=");
		builder.append(id);
		builder.append("]");
		return builder.toString();
	}
	
}