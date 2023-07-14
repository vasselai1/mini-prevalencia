package testes.consultas.auxbrasil;

import java.util.stream.Collectors;

import br.tec.mboi.api.MiniPrevalencia.Consulta;
import testes.entidades.auxbrasil.AuxilioBrasil;
import testes.entidades.auxbrasil.Beneficio;

public class SomarTotal implements Consulta<Double, AuxilioBrasil> {

	@Override
	public Double executar(AuxilioBrasil pojoUnico) {
		return pojoUnico.getBeneficios().parallelStream().collect(Collectors.summingDouble(Beneficio::getValor));
	}

}