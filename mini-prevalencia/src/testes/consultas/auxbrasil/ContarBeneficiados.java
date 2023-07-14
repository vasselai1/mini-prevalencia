package testes.consultas.auxbrasil;

import br.tec.mboi.api.MiniPrevalencia.Consulta;
import testes.entidades.auxbrasil.AuxilioBrasil;

public class ContarBeneficiados implements Consulta<Integer, AuxilioBrasil> {

	@Override
	public Integer executar(AuxilioBrasil pojoUnico) {		
		return pojoUnico.getBeneficios().size();
	}

}
