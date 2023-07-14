package testes.consultas.auxbrasil;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import br.tec.mboi.api.MiniPrevalencia.Consulta;
import testes.entidades.auxbrasil.AuxilioBrasil;
import testes.entidades.auxbrasil.Beneficio;

public class ConsultarBeneficiadosPorNomeFonetico implements Consulta<List<Beneficio>, AuxilioBrasil> {

	private String[] codigosFoneticosBusca;
	
	public ConsultarBeneficiadosPorNomeFonetico(String... codigosFoneticosBusca) {		
		this.codigosFoneticosBusca = codigosFoneticosBusca;
	}

	@Override
	public List<Beneficio> executar(AuxilioBrasil pojoUnico) {		
		return pojoUnico.getBeneficios().parallelStream()
				.filter(beneficio -> contemCodigosFoneticos(beneficio.getCodigoFoneticoNome(), codigosFoneticosBusca))
				.sorted(Comparator.comparing(Beneficio::getNomeBeneficiado))
				.collect(Collectors.toList());
	}

	private static boolean contemCodigosFoneticos(String codigoFoneticoNome, String[] codigosFoneticosConferir) {
		for (String codigoConferir : codigosFoneticosConferir) {
			if (!codigoFoneticoNome.contains(codigoConferir)) {
				return false;
			}
		}
		return true;
	}
	
}
