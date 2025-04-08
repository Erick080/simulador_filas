## Instruções
 - Modifique os parâmetros do arquivo input_simulador.yml e execute o código com o comando `javac simulador.java && java simulador`

- Obs: ao especificar no yml uma fila tandem, coloque qualquer numero nos campos referentes ao tempo de chegada, mas nao deixe eles vazios, caso contrario o parser vai falhar.

TODO: Adaptar para simular uma rede de filas com probabilidades
- Fazer uma lista de filas/probabilidades para cada fila (tem que ser ordenada por probabilidade)
- Adaptar o yml para ele parsear as probabilidades entre as filas (se basear no codigo no moodle)
- Adaptar o codigo de parse para detectar quando uma fila recebe clientes de fora ou de uma fila conectada(pra ver quando precisa parsear tempos de chegada ou nao)