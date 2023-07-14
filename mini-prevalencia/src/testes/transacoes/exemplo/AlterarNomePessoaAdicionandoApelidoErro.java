package testes.transacoes.exemplo;

import br.tec.mboi.api.MiniPrevalencia.TransacaoSemRetorno;
import testes.entidades.exemplo.ExemploModelo;
import testes.entidades.exemplo.Pessoa;
import testes.exceptions.ValidacaoCampoException;

public class AlterarNomePessoaAdicionandoApelidoErro implements TransacaoSemRetorno<ExemploModelo, ValidacaoCampoException> {

	private static final long serialVersionUID = 1L;

	private Long idPessoa;
	private String novoNome;
	private String apelido;
	
	public AlterarNomePessoaAdicionandoApelidoErro(Long idPessoa, String novoNome, String apelido) {		
		this.idPessoa = idPessoa;
		this.novoNome = novoNome;
		this.apelido = apelido;
	}

	@Override
	public void validar(ExemploModelo pojoUnico) throws ValidacaoCampoException {
		if (!pojoUnico.getMapaPessoasId().containsKey(idPessoa)) {
			throw new ValidacaoCampoException("id", "Pessoa não encontrada");
		}		
	}

	@Override
	public void executar(ExemploModelo pojoUnico) {
		Pessoa pessoa = pojoUnico.getMapaPessoasId().get(idPessoa);
		pessoa.setNome(novoNome);
		pessoa.getApelidos().add(apelido);//Nullpointer...
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AlterarNomePessoaAdicionandoApelidoErro [idPessoa=");
		builder.append(idPessoa);
		builder.append(", novoNome=");
		builder.append(novoNome);
		builder.append(", apelido=");
		builder.append(apelido);
		builder.append("]");
		return builder.toString();
	}
	
}