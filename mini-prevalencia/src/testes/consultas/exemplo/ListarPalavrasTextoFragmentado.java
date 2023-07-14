package testes.consultas.exemplo;

import java.util.List;

import br.tec.mboi.api.MiniPrevalencia.Consulta;
import testes.entidades.exemplo.ExemploModelo;

public class ListarPalavrasTextoFragmentado implements Consulta<List<String>, ExemploModelo> {

	@Override
	public List<String> executar(ExemploModelo pojoUnico) {		
		return pojoUnico.getTextoFragmentado();
	}

}
