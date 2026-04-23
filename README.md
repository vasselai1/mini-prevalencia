-----

# 🌟 Mini-Prevalência: ACID Java em uma classe 🌟

A **Mini-Prevalência** é uma solução de prevalência de objetos em Java inspirada no famoso padrão e implementação Prevayler (2001-2013). Ele oferece um mecanismo ACID (Atomicidade, Consistência, Isolamento e Durabilidade) onde o modelo de dados reside na memória (RAM) para acesso rápido, e todas as alterações são registradas de forma transacional em arquivos binários no sistema de arquivos.

Este projeto é notável por sua simplicidade, sendo contido em uma única classe, ideal para ser copiado e integrado rapidamente em qualquer projeto Java. 

# 🎯 Foco Total em Orientação a Objetos

Livre-se da complexidade do mapeamento Objeto-Relacional (ORM) e da necessidade constante de cache externo (como Redis) para compensar a lentidão dos bancos de dados tradicionais. Com a Mini-Prevalência, você só precisa dominar a Orientação a Objetos. Seu modelo de dados é 100% puro e serializável, eliminando a camada de impedância relacional e entregando performance nativa.

-----

## ✨ Recursos Principais

  * **Modelo de Prevalência Prevayler-like:** O estado (modelo POJO) é mantido em **memória** para consultas ultra-rápidas. Todas as modificações são transações que são serializadas e gravadas em **disco** como um log de operações.
  * **Transações ACID:** O estado só é alterado dentro de um bloco sincronizado (`synchronized (pojoRegistro)`), garantindo **Atomicidade**, **Consistência** e **Isolamento**. A gravação em disco garante a **Durabilidade**.
  * **Serialização Java Padrão:** Utiliza `java.io.ObjectOutputStream` e `java.io.ObjectInputStream` para gravar o modelo e as transações, resultando em arquivos binários (`.bin`).
  * **Reconstrução de Estado Rápida:** O sistema é capaz de reconstruir o estado da memória a partir do **Arquivo Acelerador** (snapshot) e reexecutar apenas as transações subsequentes.
  * **Modo Primário e Réplica:**
      * **Primário:** Permite consultas e executa transações (gravação).
      * **Réplica:** Somente leitura, com um *listener* que acompanha e executa transações do sistema de arquivos para manter a memória sincronizada.
  * **Proteção de Retorno (Opcional):** Permite copiar objetos retornados de consultas ou transações (`isRetornoProjegidoPorCopia()`) para evitar alterações não transacionais no modelo.

-----

## 🏗️ Requisitos e Configuração

### Pré-requisitos

  * **Java Development Kit (JDK):** O projeto utiliza recursos do Java padrão.
  * Seu **Modelo de Dados (POJO)** e suas **Classes de Transação** devem implementar a interface `java.io.Serializable`.

### Estrutura de Diretório

Ao configurar, a Mini-Prevalência criará a seguinte estrutura de diretórios:

```
[diretorio_gravacao]/
├── [pacote.da.classe.do.modelo.POJO]/
│   ├── ACELERADOR/       <-- Contém o arquivo snapshot (foto) do modelo.
│   ├── REPLICAS/         <-- Arquivo de orientação de réplicas.
│   ├── SEQUENCIAS/       <-- Arquivos de contadores sequenciais.
│   └── TRANSACOES/       <-- Arquivos de log binários das transações (ex: 1.bin, 2.bin).
```

### Configurando o Ambiente

O ambiente deve ser configurado apenas uma vez.

```java
import br.tec.mboi.api.MiniPrevalencia;

// Configuração Recomendada (Primário):
// Grava acelerador automaticamente a cada 10s de inatividade e apaga transações antigas.
MiniPrevalencia.configurar("/caminho/para/seus/dados", true, false, 10, true); 

// Configuração de Réplica (Somente Leitura):
MiniPrevalencia.configurarReplica("/caminho/compartilhado/com/dados");

// Obtendo a instância de Prevalência do seu Modelo:
MiniPrevalencia<SeuModeloPOJO> prevalencia = MiniPrevalencia.prevalecer(SeuModeloPOJO.class);
```
ou
```java
import br.tec.mboi.api.MiniPrevalencia;
import br.tec.mboi.api.MiniPrevalencia.Configurador;

// Diretório onde os arquivos de prevalência serão salvos
private static final String DIRETORIO = System.getProperty("user.home") + "/meu_projeto_dados";

Configurador conf = new Configurador() {
    // É o nó principal, que pode executar transações e gravar em disco.
    public boolean isPrimario() {return true;}
    
    // O diretório raiz para a gravação dos dados.
    public String getDiretorioGravacao() {return DIRETORIO;}
    
    // Protege o modelo interno retornando cópias (clones) dos objetos consultados/modificados.
    public boolean isRetornoProjegidoPorCopia() {return true;}
    
    // Se NULL, o snapshot (acelerador) só será gerado manualmente.
    public Integer getSegundosInatividadeParaIniciarGravacaoAcelerador() {return null;}
    
    // Se TRUE, transações internalizadas pelo snapshot são apagadas (libera espaço).
    public boolean isApagarTransacoesInternalizadasPeloAcelerador() {return false;}
};

MiniPrevalencia.setConfigurador(conf);
```


-----

## 🛠️ Interfaces Chave para o Desenvolvimento

Seu modelo de domínio interage com a Mini-Prevalência através de interfaces:

### 1\. Transação (Atualiza o Modelo)

Usada para todas as modificações. Implementa a lógica de validação do seu negócio (constraints) e execução (altera o estado do seu modelo), tudo isso será executado em um bloco sincronizado da prevalência.

```java
public interface TransacaoComRetorno <R, T extends Serializable, E extends Throwable> extends Transacao<T, E>, Serializable {
    // 1. Opcional: Lançar Exception se a execução for inválida (sem causar rollback).
    void validar(T pojoRegistro) throws E;
    
    // 2. Obrigatório: Atualizar o estado do pojoRegistro.
    R executar(T pojoRegistro);
}
```
ou
```java
public interface TransacaoSemRetorno <T extends Serializable, E extends Throwable> extends Transacao<T, E>, Serializable {
    // 1. Opcional: Lançar Exception se a execução for inválida (sem causar rollback).
    void validar(T pojoRegistro) throws E;
    
    // 2. Obrigatório: Atualizar o estado do pojoRegistro.
    void executar(T pojoRegistro);
}
```

### 2\. Consulta (Apenas Leitura)

Usada para ler o estado do modelo. **Cuidado:** Leituras aqui são mais rápidas, mas podem ser "sujas" (passar por reversão caso ocorra erro de IO ou bloco executar() da sua transação levante uma exception).

```java
public interface Consulta<R, T extends Serializable> {
    R executar(T pojoUnico);
}
```

### 3\. Transação com Retorno (Para Leitura Limpa e Consistente)

A interface `TransacaoComRetorno` foi criada para alterar o modelo e retornar dados, mas também é ideal para operações de leitura (consultas) que precisam garantir a consistência total dos dados.

Ao ser executada via `prevalencia.executar(transacao)`, o método `executar(T pojoRegistro)` é executado **dentro do bloco sincronizado** do modelo (POJO). Isso garante que os dados acessados estejam no estado consistente e que a leitura seja isolada de transações de escrita concorrentes, caracterizando uma **Leitura Limpa (Clean Read)**.

**Exemplo de Consulta (Leitura) Usando `TransacaoComRetorno`:**

```java
import br.tec.mboi.api.MiniPrevalencia.TransacaoComRetorno;
import testes.entidades.auxbrasil.AuxilioBrasil;
import testes.exceptions.ExemploException;

// Transação de leitura para contar beneficiários (usa o bloco sincronizado para garantir consistência)
public class ContarBeneficiados implements TransacaoComRetorno<Integer, AuxilioBrasil, ExemploException> {
    private static final long serialVersionUID = 1L;
	
    @Override
    public void validar(AuxilioBrasil pojoRegistro) throws ExemploException {
        //nada aqui!
    }

    @Override
    public Integer executar(AuxilioBrasil pojoRegistro) {
        // O código aqui é executado de forma síncrona, garantindo a leitura limpa/consistente.
		// Também poderia alterar dados aqui
        return pojoRegistro.getBeneficios().size();
    }
}
```

## 💡 Exemplo de Uso (Conceitual)

```java
// 1. Classe de Exemplo (Seu Modelo POJO)
public class SeuModeloPOJO implements Serializable { 
    // ... deve ter construtor padrão ...
}

// 2. Classe de Transação de Exemplo
public class AdicionarUsuario implements MiniPrevalencia.TransacaoSemRetorno<SeuModeloPOJO, RuntimeException> {
	private static final long serialVersionUID = 1L;

	private final String nome; // Deve ser serializável

    public AdicionarUsuario(String nome) {
        this.nome = nome;
    }

    @Override
    public void validar(SeuModeloPOJO pojoRegistro) throws RuntimeException {
        if (nome == null || nome.isEmpty()) {
            throw new RuntimeException("Nome inválido!");
        }
    }

    @Override
    public void executar(SeuModeloPOJO pojoRegistro) {
        // Lógica de modificação do estado:
        pojoRegistro.getListaDeUsuarios().add(this.nome);
    }
}

// 3. Execução
// O tipo E (RuntimeException) é a exception que pode ser lançada na validação.
try {
    prevalencia.executar(new AdicionarUsuario("João"));
} catch (RuntimeException e) {
    // Captura exceção da validação.
} catch (MiniPrevalencia.ExecucaoTransacaoException e) {
    // Captura exceção da execução (rollback já foi feito).
}
```

-----

## 💡 Estrutura e Exemplo de Uso

O fluxo de trabalho envolve três componentes principais: o **Modelo (POJO)**, a **Transação** (escrita) e a **Consulta** (leitura).

### 1\. Modelo (POJO)

Deve implementar `Serializable` e ser o objeto único de estado que será gravado. Este será o ponto de acesso central ao seu modelo, só existirá uma instância dessa classe, por isso do construtor padrão, a pravalência se encarrega de controlar sua construção, demais composições/relacionamentos a instânciação deve ocorrer dentro de transações.

```java
// testes.entidades.auxbrasil.AuxilioBrasil
public class ExemploModelo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Map<String, Estado> estados = new HashMap<>();
    private List<Beneficio> beneficios = new ArrayList<>();
	private String nome;

    // Construtor padrão obrigatório.
    public AuxilioBrasil() { }
    
    // Getters e Setters
    // ...
}
```

### 2\. Transação (Para Escrita)

A classe de transação **deve ser nomeada** (não pode ser classe anônima, pois precisa ser serializada e desserializada) e conter todos os dados necessários para a modificação.

```java
// TransacaoSemRetorno (sem retorno de dados)
public class NomearModeloExemplo implements MiniPrevalencia.TransacaoSemRetorno<ExemploModelo, ValidacaoCampoException> {
    private static final long serialVersionUID = 1L;

    private final String novoNome; 

    public NomearModeloExemplo(String novoNome) {
        this.novoNome = novoNome;
    }

    @Override
    public void validar(ExemploModelo pojoRegistro) throws ValidacaoCampoException {
        if ("Teste Validação".equals(novoNome)) { // Exemplo de regra
            throw new ValidacaoCampoException("Nome proibido!");
        }
    }

    @Override
    public void executar(ExemploModelo pojoRegistro) {
        // Apenas o código aqui dentro modifica o estado.
        pojoRegistro.setNome(novoNome);
    }
}
```

**Execução de Transação:**

```java
try {
    prevalencia.executar(new NomearModeloExemplo("Meu Novo Nome"));
} catch (ValidacaoCampoException e) {
    // Exceção de Validação (antes da execução/gravação).
} catch (MiniPrevalencia.ExecucaoTransacaoException e) {
    // Exceção durante a execução (rollback é realizado).
}
```

### 3\. Consulta (Para Leitura)

Usada para ler dados do modelo.

```java
// testes.consultas.auxbrasil.TotalizarValoresBeneficiosPorCidade
public class TotalizarValoresBeneficiosPorCidade implements Consulta<Map<Cidade, Double>, ExemploModelo> {

    @Override
    public Map<Cidade, Double> executar(AuxilioBrasil pojoUnico) {
        // Lógica de consulta (ex: uso de streams para processamento de dados)
        return pojoUnico.getBeneficios()
                   .parallelStream()
                   .collect(Collectors.groupingBy(Beneficio::getCidade, Collectors.summingDouble(Beneficio::getValor)));
    }
}
```

**Execução de Consulta:**

```java
Map<Cidade, Double> totalPorCidade = prevalencia.executar(new TotalizarValoresBeneficiosPorCidade());
```


-----

## ⚠️ Observações Importantes

  * **ACID:** Nunca modifique o estado do seu modelo fora das transações! É tentador fazer um simples set em um atributo, mas isso quebra a prevalência, se precisar apenas para exibição ative RetornoProjegidoPorCopia.
  * **Serialização:** O uso de `SerialVersionUID` e a compatibilidade das classes (POJOs e Transações) entre diferentes versões da aplicação são **críticos** para a correta reconstrução do estado.
  * **Transações:** Uma transação, após ser executada, **não deve ser alterada**. Para uma nova funcionalidade, crie uma nova classe de transação.
  * **Transações Anônimas:** Classes de transação anônimas não podem ser usadas, pois a serialização requer uma classe nomeada e estática para reconstrução.
  * **Memória RAM:** Lembre-se que a base inteira reside na memória RAM. Para bases com milhões de registros, certifique-se de que a Java Virtual Machine (JVM) tenha memória heap suficiente (ex: java -Xms4000m -Xmx6000m).
  * **Bloco Transacional:** Deve ser leve e rápido sem depências externas, caso seja lento, resultará em um gargalo generalizado nas gravações, bloqueando todas as transações subsequentes.
  * **Acesso aos Dados:** Somente pela aplicação, não desenvolvi um utilitário de consultas externas. Como trabalho futuro penso em um utilitário com reflection e JoSQL estilo SquirelSQL. (Contribuições são bem vindas).
  * **Autoscaling Restrito:** Só pode haver uma instância primária (gravação), no entanto réplicas de leitura são irrestritas.
  * **Dead Lock:** Não utilizar transações dentro de transações, cada transação deve ser auto-contida.
  * **Portabilidade:** A Mini-Prevalência foi desenhada com um *backend* de persistência desacoplado. Isso significa que, em vez de se limitar ao disco local, a arquitetura permite o **fácil porte** para qualquer meio que grave *bytes*. Já existe uma versão particular que utiliza o **Amazon S3** (ou qualquer *Object Storage*) como mecanismo de persistência, facilitando o uso em ambientes de **nuvem** e com alta disponibilidade.
-----


## 🚀 Estudo de Caso de Desempenho: Base Auxílio Brasil

O **Mini-Prevalência** foi testado para lidar com bases de dados massivas, demonstrando sua eficiência na reconstrução de estado e na velocidade de consultas em memória, mesmo em um hardware modesto.

### Cenário do Teste de Carga

| Característica | Detalhe |
| :--- | :--- |
| **Base de Dados** | Base de um mês do Auxílio Brasil (`202301_AuxilioBrasil.csv`). |
| **Volume de Dados** | Mais de **21,6 milhões de registros** (Benefícios). |
| **Modelo Final** | Mais de **21,6 milhões de objetos** (Benefícios + Cidades + Estados) mantidos em memória. |
| **Tamanho do Snapshot** | Arquivo Acelerador (Snapshot) de aproximadamente **1.4 GB**. |
| **Hardware** | **Notebook i5 mobile de primeira geração com SSD** (Hardware modesto/antigo). |
| **Configuração Java** | Uso de memória heap alocada (ex: `java -Xms4000m -Xmx6000m`). |

### Resultados de Desempenho

O tempo de execução foi medido no hardware modesto especificado:

| Operação | Detalhe | Tempo no i5 de 1ª Geração |
| :--- | :--- | :--- |
| **Reconstrução de Estado** | Carregamento da base de **21.626.680** objetos. | **~ 42 segundos** |
| **Taxa de Ingestão** | Adição de transações em lote. | **~ 43.000 objetos/segundo** |
| **Consulta Simples (Soma)** | Somar o valor total dos **21.6M** de benefícios. | **~ 660 ms** |
| **Pesquisa Fonética** | Pesquisa complexa de nome/sobrenome usando código fonético, em **21.6M** de registros. | **~ 538 ms** |

### Conclusões do Desempenho

1.  **Escala em RAM:** O teste confirma a capacidade da Mini-Prevalência de lidar com mais de **21 milhões de objetos** (1.4 GB de snapshot) em memória, desde que a JVM seja configurada com heap adequado.
2.  **Acesso em Milissegundos:** Uma vez carregado, o sistema oferece consultas complexas e de larga escala em **sub-segundos**, aproveitando a velocidade da memória RAM, que é a principal vantagem do padrão Prevayler.
3.  **Gravação Rápida:** O mecanismo de serialização binária padrão do Java e a gravação transacional permitem uma alta taxa de ingestão de dados, atingindo **43.000 objetos/segundo** em transações em lote.

---
