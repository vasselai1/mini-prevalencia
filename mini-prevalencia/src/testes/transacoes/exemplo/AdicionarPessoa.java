package testes.transacoes.exemplo;

import br.tec.mboi.api.MiniPrevalencia.TransacaoComRetorno;
import testes.entidades.exemplo.ExemploModelo;
import testes.entidades.exemplo.Pessoa;
import testes.exceptions.ValidacaoCampoException;

public class AdicionarPessoa implements TransacaoComRetorno<Integer, ExemploModelo, ValidacaoCampoException> {

	private static final long serialVersionUID = 1L;

	private PessoaVO pessoaVO;
	
	public AdicionarPessoa(PessoaVO pessoaVO) {		
		this.pessoaVO = pessoaVO;
	}

	@Override
	public void validar(ExemploModelo pojoUnico) throws ValidacaoCampoException {
		if (pessoaVO.getDataNascimento() == null) {
			throw new ValidacaoCampoException("dataNascimento", "Data de nascimento é null!");
		}
		if (pojoUnico.getMapaPessoasEmail().containsKey(pessoaVO.getEmail())) {
			throw new ValidacaoCampoException("email", "Já existe outra pessoa cadastrada com mesmo email!");
		}
		if (pojoUnico.getMapaPessoasId().containsKey(pessoaVO.getId())) {
			throw new ValidacaoCampoException("id", "Este identificador já foi informado!");
		}
	}

	@Override
	public Integer executar(ExemploModelo pojoUnico) {
		Pessoa novaPessoa = new Pessoa();
		novaPessoa.setDataNascimento(pessoaVO.getDataNascimento());
		novaPessoa.setEmail(pessoaVO.getEmail());
		novaPessoa.setId(pessoaVO.getId());
		novaPessoa.setNome(pessoaVO.getNome());
		
		pojoUnico.getMapaPessoasEmail().put(novaPessoa.getEmail(), novaPessoa);
		pojoUnico.getMapaPessoasId().put(novaPessoa.getId(), novaPessoa);
		
		return pojoUnico.getMapaPessoasId().size();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AdicionarPessoa [pessoaVO=");
		builder.append(pessoaVO);
		builder.append("]");
		return builder.toString();
	}

	
	
}
