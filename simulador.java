import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;

class simulador{
    // geracao de numeros aleatorios
    static int a;
    static int c;
    static int M;
    static double numero_previo;
    static double seed;
    static int tempo_chegada_minimo;
    static int tempo_chegada_maximo;
    static int tempo_servico_minimo;            
    static int tempo_servico_maximo;
    
    // configuracao do simulador
    static int qtd_numeros_aleatorios;
    static int capacidade_fila;
    static int num_servidores;

    // controle do simulador
    static double tempo_global = 0.0;
    static double [] tempos_dos_estados;
    static int perda_de_clientes = 0;
    static int tamanho_fila_atual = 0;
    static int numeros_aleatorios_usados = 0;

    static PriorityQueue<Evento> eventos = new PriorityQueue<>(
        (Evento e1, Evento e2) -> Double.compare(e1.tempo, e2.tempo));

    static class Evento{
        char tipo;
        double tempo;
        public Evento(char tipo){
            this.tipo = tipo;
            if (tipo == 'C')
                this.tempo = tempo_global + (((tempo_chegada_maximo - tempo_chegada_minimo) * nextRandom()) + tempo_chegada_minimo);
            else
                this.tempo = tempo_global + (((tempo_servico_maximo - tempo_servico_minimo) * nextRandom()) + tempo_servico_minimo);
        }
        // Overload para o caso de ser o primeiro evento do simulador
        public Evento(char tipo, double tempo){
            this.tipo = tipo;
            this.tempo = tempo;
        }
    }

    static double nextRandom(){
        numero_previo = (a * numero_previo + c) % M;
        numeros_aleatorios_usados++;
        return numero_previo / M;
    }

    static void chegada(Evento e){
        if (tamanho_fila_atual < capacidade_fila){
            tamanho_fila_atual++;
            if (tamanho_fila_atual <= num_servidores){
                eventos.add(new Evento('S'));
            }
        }
        else{
            perda_de_clientes++;
        }
        eventos.add(new Evento('C'));
    }

    static void saida(Evento e){
        tamanho_fila_atual--;
        if (tamanho_fila_atual >= num_servidores){
            eventos.add(new Evento('S'));
        }
    }

    static void loadYamlConfig(String nome_arquivo){
        Map<String, String> config = new HashMap<>();
        Scanner scanner = null;
        try (InputStream inputStream = new FileInputStream(nome_arquivo)) {
            scanner = new Scanner(inputStream);
                
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    config.put(parts[0].trim(), parts[1].trim());
                }
            }
            a = Integer.parseInt(config.get("a"));
            c = Integer.parseInt(config.get("c"));
            M = Integer.parseInt(config.get("M"));
            tempo_chegada_minimo = Integer.parseInt(config.get("tempo_chegada_minimo"));
            tempo_chegada_maximo = Integer.parseInt(config.get("tempo_chegada_maximo"));
            tempo_servico_minimo = Integer.parseInt(config.get("tempo_servico_minimo"));
            tempo_servico_maximo = Integer.parseInt(config.get("tempo_servico_maximo"));
            seed = Double.parseDouble(config.get("seed"));
            qtd_numeros_aleatorios = Integer.parseInt(config.get("qtd_numeros_aleatorios"));
            capacidade_fila = Integer.parseInt(config.get("capacidade_fila"));
            tempos_dos_estados = new double[capacidade_fila+1];
            num_servidores = Integer.parseInt(config.get("num_servidores"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    public static void main(String[] args){
        loadYamlConfig("input_simulador.yml");
        numero_previo = seed;
        eventos.add(new Evento('C', 2.000));

        while(numeros_aleatorios_usados < qtd_numeros_aleatorios){
            Evento e = eventos.poll();
            tempos_dos_estados[tamanho_fila_atual] += e.tempo - tempo_global;
            tempo_global = e.tempo;
            if (e.tipo == 'C'){
                chegada(e);
            }
            else
                saida(e);
        }

        System.out.printf("Tempo global da simulacao: %.4f\n", tempo_global);
        System.out.println("Perda de clientes: " + perda_de_clientes);
        for (int i = 0; i <= capacidade_fila; i++){
            System.out.printf("Estado %d: tempo = %.3f \t probabilidade = %.3f%%\n", i, tempos_dos_estados[i], (tempos_dos_estados[i] / tempo_global) * 100);
        }
    }

}