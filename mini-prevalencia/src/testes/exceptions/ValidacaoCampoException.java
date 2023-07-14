package testes.exceptions;

public class ValidacaoCampoException extends Exception {

	private static final long serialVersionUID = 1L;

	private String nomeCampo;
	
	public ValidacaoCampoException(String nomeCampo, String mensagem) {
		super(mensagem);
		this.nomeCampo = nomeCampo;
	}
	
	public String getNomeCampo() {
		return nomeCampo;
	}	
}
