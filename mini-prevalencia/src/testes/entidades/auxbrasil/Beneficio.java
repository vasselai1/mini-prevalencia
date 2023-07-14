package testes.entidades.auxbrasil;

import java.io.Serializable;

public class Beneficio implements Serializable {

	private static final long serialVersionUID = 1L;

	private Cidade cidade;
	private String nomeBeneficiado;
	private String codigoFoneticoNome;
	private float valor;	
	private short anoCompetencia;
	private short mesCompetencia;
	
	public Cidade getCidade() {
		return cidade;
	}
	public void setCidade(Cidade cidade) {
		this.cidade = cidade;
	}
	public String getNomeBeneficiado() {
		return nomeBeneficiado;
	}
	public String getCodigoFoneticoNome() {
		return codigoFoneticoNome;
	}
	public void setCodigoFoneticoNome(String codigoFoneticoNome) {
		this.codigoFoneticoNome = codigoFoneticoNome;
	}
	public void setNomeBeneficiado(String nomeBeneficiado) {
		this.nomeBeneficiado = nomeBeneficiado;
	}
	public Float getValor() {
		return valor;
	}
	public void setValor(float valor) {
		this.valor = valor;
	}
	public float getAnoCompetencia() {
		return anoCompetencia;
	}
	public void setAnoCompetencia(short anoCompetencia) {
		this.anoCompetencia = anoCompetencia;
	}
	public short getMesCompetencia() {
		return mesCompetencia;
	}
	public void setMesCompetencia(short mesCompetencia) {
		this.mesCompetencia = mesCompetencia;
	}
	
}