package testes.transacoes.auxbrasil;

import java.util.List;

import br.tec.mboi.api.MiniPrevalencia.TransacaoSemRetorno;
import testes.entidades.auxbrasil.AuxilioBrasil;
import testes.entidades.auxbrasil.Beneficio;
import testes.entidades.auxbrasil.Cidade;

public class AdicionarBeneficiosEmLote implements TransacaoSemRetorno<AuxilioBrasil, Exception> {

	private static final long serialVersionUID = 1L;

	private List<BeneficioVO> beneficios;
	
	public AdicionarBeneficiosEmLote(List<BeneficioVO> beneficios) {
		this.beneficios = beneficios;
	}
	
	@Override
	public void validar(AuxilioBrasil pojoRegistro) throws Exception {
		for (BeneficioVO beneficioLoop : beneficios) {						
			if (!pojoRegistro.getCidades().containsKey(beneficioLoop.getChaveCidade())) {
				throw new Exception("Chave cidade " + beneficioLoop.getChaveCidade() + " inválida!");
			}
			if ((beneficioLoop.getNomeBeneficiado() == null) || beneficioLoop.getNomeBeneficiado().isEmpty()) {
				throw new Exception("Nome do beneficiado " + beneficioLoop.getNomeBeneficiado() + " invalido!");			
			}
			if ((beneficioLoop.getCodigoFoneticoNome() == null) || beneficioLoop.getCodigoFoneticoNome().isEmpty()) {
				throw new Exception("Codigo fonético " + beneficioLoop.getCodigoFoneticoNome() + " invalido!");			
			}
			if (beneficioLoop.getValor() < 0) {
				throw new Exception("Valor " + beneficioLoop.getValor() + " invalido!");			
			}
			if (beneficioLoop.getAnoCompetencia() < 2000) {
				throw new Exception("Ano " + beneficioLoop.getAnoCompetencia() + " invalido!");			
			}
			if ((beneficioLoop.getMesCompetencia() < 0) || (beneficioLoop.getMesCompetencia() > 12)) {
				throw new Exception("Mês " + beneficioLoop.getMesCompetencia() + " invalido!");			
			}
		}
	}
	
	@Override
	public void executar(AuxilioBrasil pojoUnico) {
		for (BeneficioVO beneficioLoop : beneficios) {
			Cidade cidade = pojoUnico.getCidades().get(beneficioLoop.getChaveCidade());			
			Beneficio beneficio = new Beneficio();		
			beneficio.setAnoCompetencia(beneficioLoop.getAnoCompetencia());
			beneficio.setCidade(cidade);
			beneficio.setCodigoFoneticoNome(beneficioLoop.getCodigoFoneticoNome());
			beneficio.setMesCompetencia(beneficioLoop.getMesCompetencia());
			beneficio.setNomeBeneficiado(beneficioLoop.getNomeBeneficiado());
			beneficio.setValor(beneficioLoop.getValor());
			pojoUnico.getBeneficios().add(beneficio);
		}
	}	
	
}