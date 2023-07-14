package testes.consultas.auxbrasil;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import br.tec.mboi.api.MiniPrevalencia.Consulta;
import testes.entidades.auxbrasil.AuxilioBrasil;
import testes.entidades.auxbrasil.Beneficio;
import testes.entidades.auxbrasil.Cidade;
import testes.transacoes.auxbrasil.AdicionarCidadeSeNaoExistir;

public class ListarBeneficiosPorCidade implements Consulta<List<Beneficio>, AuxilioBrasil> {

	private String siglaUF;
	private String nomeCidade;
	
	public ListarBeneficiosPorCidade(String nomeCidade, String siglaUF) {
		this.siglaUF = siglaUF;
		this.nomeCidade = nomeCidade;
	}

	@Override
	public List<Beneficio> executar(AuxilioBrasil pojoUnico) {		
		Cidade municipio = pojoUnico.getCidades().get(AdicionarCidadeSeNaoExistir.padronizarChaveCidade(nomeCidade, siglaUF));
		return pojoUnico.getBeneficios().parallelStream()
				.filter(beneficio -> beneficio.getCidade().equals(municipio))
				.sorted(Comparator.comparing(Beneficio::getNomeBeneficiado))
				.collect(Collectors.toList());
	}
	
}