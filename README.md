# mini-prevalencia

Inspirado no Prevayler 2001-2013, grava em memória e em sistema de arquivos a construção transacional de seu modelo POJO, possibilitando que ao reiniciar seu sistema, os objetos 
sejam reconstruídos em memória através da execução de transações serializáveis carregadas do sistema de arquivos. O uso de réplicas secundárias é limitado a leitura, sem gravação.
O modelo de dados (POJO) e transacional devem ser serializáveis, pois, o funcionamento depende da serialização Java para gravação de arquivos e retornos que protegem o modelo de alteração 
não transacional. Utilize corretamente serialVersionUID para cada uma das entidades e transações de seu modelo, também utilize backup automatizado por ferramentas especializadas.
A atualização do modelo deve ser implementada somente através de transações serializáveis, o modelo (pojoRegistro) não pode ser atualizado fora das transações! Toda transação deve ser serializada 
na íntegra para o correto funcionamento, por exemplo, todos dados que forem atribuídos na transação devem ser serializáveis e suficientes para que a transação seja reexecutada quando for 
carregada do sistema de arquivos.
Para utilizar: copie a classe MiniPrevalencia, crie o diretório que deseja gravar os arquivos, codifique seu modelo serializável, transações serializáveis e consultas conforme interfaces disponibilizadas.
