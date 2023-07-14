package testes.consultas.exemplo;

import java.util.List;
import java.util.stream.Collectors;

import br.tec.mboi.api.MiniPrevalencia.Consulta;
import testes.entidades.exemplo.ExemploModelo;

public class ListarNomesPessoas implements Consulta<List<String>, ExemploModelo>{

	@Override
	public List<String> executar(ExemploModelo seuPojoUnico) {
 		return seuPojoUnico.getMapaPessoasEmail().values().stream().map(p -> p.getNome()).collect(Collectors.toList());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ListarNomesPessoas []");
		return builder.toString();
	}
	
}
