import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
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
    static int qtd_numeros_aleatorios;

    // controle do simulador
    static double tempo_global = 0.0;
    static int numeros_aleatorios_usados = 0;
    static Fila primeira_fila; // linked list

    class Fila{
        int Server;
        int Capacity;
        double MinArrival;
        double MaxArrival;
        double MinService;
        double MaxService;
        int Customers;
        int Loss;
        double[] Times;
        Fila NextQ;    // prox fila conectada se tiver

        public Fila(int Server, int Capacity, double MinArrival, double MaxArrival,
                    double MinService, double MaxService, Fila NextQ){
            this.Server = Server;
            this.Capacity = Capacity;
            this.MinArrival = MinArrival;
            this.MaxArrival = MaxArrival;
            this.MinService = MinService;
            this.MaxService = MaxService;
            this.NextQ = NextQ;
            Times = new double[Capacity+1];
            Customers = 0;
            Loss = 0;
        }

        void Chegada(){
            if (Customers < Capacity){
                Customers++;
                if (Customers <= Capacity){
                    if (NextQ != null) // talvez nao funcione
                        eventos.add(new Evento('P', this));
                    else
                        eventos.add(new Evento('S', this));
                }
            }
            else{
                Loss++;
            }
            eventos.add(new Evento('C', this));
        }

        void Saida(){
            Customers--;
            if (Customers >= Capacity){
                eventos.add(new Evento('S',this));
            }
        }
    }

    static PriorityQueue<Evento> eventos = new PriorityQueue<>(
        (Evento e1, Evento e2) -> Double.compare(e1.tempo, e2.tempo));

    static class Evento{
        char tipo;
        double tempo;
        Fila fila;
        public Evento(char tipo, Fila fila_param){
            this.tipo = tipo;
            if (tipo == 'C'){
                this.tempo = tempo_global + (((fila_param.MaxArrival - fila_param.MinArrival) * nextRandom()) + fila_param.MinArrival);
                this.fila = fila_param;
            }
            else{
                this.tempo = tempo_global + (((fila_param.MaxService - fila_param.MinService) * nextRandom()) + fila_param.MinService);
                if (tipo == 'P')
                    this.fila = fila_param.NextQ;      
                else
                    this.fila = fila_param;
                }
        }
        // Overload para o caso de ser o primeiro evento do simulador
        public Evento(char tipo, double tempo, Fila fila){
            this.tipo = tipo;
            this.tempo = tempo;
            this.fila = fila;
        }
    }

    static double nextRandom(){
        numero_previo = (a * numero_previo + c) % M;
        numeros_aleatorios_usados++;
        return numero_previo / M;
    }

    static void loadYamlConfig(String nome_arquivo){
        Map<String, String> config = new HashMap<>();
        Scanner scanner = null;
        try (InputStream inputStream = new FileInputStream(nome_arquivo)) {
            scanner = new Scanner(inputStream);
                
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.isEmpty() || line.startsWith("#")){
                    continue;
                }
                String[] parts = line.split(":");
                String key = parts[0].trim();
                String value = parts[0].trim();

                if (key.equals("a"))
                    a = Integer.parseInt(value);
                else if (key.equals("c"))
                    c = Integer.parseInt(value);
                else if (key.equals("M"))
                    M = Integer.parseInt(value);
                else if (key.equals("seed"))
                    seed = Integer.parseInt(value);
                else if (key.equals("qtd_numeros_aleatorios"))
                    qtd_numeros_aleatorios = Integer.parseInt(value);
                
                if (line.startsWith("tempo_chegada_minimo")){
                    int tempo_chegada_minimo = Integer.parseInt(value);
                }
            }
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