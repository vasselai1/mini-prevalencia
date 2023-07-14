package testes.consultas.auxbrasil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import br.tec.mboi.api.MiniPrevalencia.Consulta;
import testes.entidades.auxbrasil.AuxilioBrasil;
import testes.entidades.auxbrasil.Beneficio;
import testes.entidades.auxbrasil.Cidade;

public class TotalizarValoresBeneficiosPorCidade implements Consulta<Map<Cidade, Double>, AuxilioBrasil>{

	@Override
	public Map<Cidade, Double> executar(AuxilioBrasil pojoUnico) {
		return pojoUnico.getBeneficios()
				.parallelStream()
				.collect(Collectors.groupingBy(Beneficio::getCidade, Collectors.summingDouble(Beneficio::getValor))).entrySet()
				.parallelStream()
				.sorted(Map.Entry.comparingByValue())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,(velho, novo) -> velho, LinkedHashMap::new));
	}

}
