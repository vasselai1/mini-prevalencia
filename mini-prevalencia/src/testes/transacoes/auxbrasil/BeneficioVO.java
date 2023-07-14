package testes.transacoes.auxbrasil;

import java.io.Serializable;

public class BeneficioVO implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String siglaUf;
	private String nomeCidade;
	private String chaveCidade;
	private String nomeBeneficiado;
	private String codigoFoneticoNome;
	private float valor;	
	private short anoCompetencia;
	private short mesCompetencia;
	
	public BeneficioVO(String siglaUf, String nomeCidade, String chaveCidade, String nomeBeneficiado, String codigoFoneticoNome, float valor, short anoCompetencia, short mesCompetencia) {		
		this.siglaUf = siglaUf;
		this.nomeCidade = nomeCidade;
		this.chaveCidade = chaveCidade;
		this.nomeBeneficiado = nomeBeneficiado;
		this.codigoFoneticoNome = codigoFoneticoNome;
		this.valor = valor;
		this.anoCompetencia = anoCompetencia;
		this.mesCompetencia = mesCompetencia;
	}
	
	public String getSiglaUf() {
		return siglaUf;
	}
	public String getNomeCidade() {
		return nomeCidade;
	}
	public String getChaveCidade() {
		return chaveCidade;
	}
	public String getNomeBeneficiado() {
		return nomeBeneficiado;
	}
	public String getCodigoFoneticoNome() {
		return codigoFoneticoNome;
	}
	public float getValor() {
		return valor;
	}
	public short getAnoCompetencia() {
		return anoCompetencia;
	}
	public short getMesCompetencia() {
		return mesCompetencia;
	}
	
}