import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;

class simulador {
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

    static class Fila {
        int Server;
        int Capacity;
        double MinArrival;
        double MaxArrival;
        double MinService;
        double MaxService;
        int Customers;
        int Loss;
        double[] Times;
        Fila NextQ; // prox fila conectada se tiver

        public Fila(int Server, int Capacity, double MinArrival, double MaxArrival,
                double MinService, double MaxService, Fila NextQ) {
            this.Server = Server;
            this.Capacity = Capacity;
            this.MinArrival = MinArrival;
            this.MaxArrival = MaxArrival;
            this.MinService = MinService;
            this.MaxService = MaxService;
            this.NextQ = NextQ;
            Times = new double[Capacity + 1];
            Customers = 0;
            Loss = 0;
        }

        void Chegada(Evento e) {
            if (Customers < Capacity) {
                Times[Customers] += e.tempo - tempo_global;
                tempo_global = e.tempo;
                Customers++;
                if (Customers <= Server) {
                    if (NextQ != null) // talvez nao funcione
                        eventos.add(new Evento('P', this));
                    else
                        eventos.add(new Evento('S', this));
                }
            } else {
                Loss++;
            }
            eventos.add(new Evento('C', this));
        }

        void Passagem(Evento e){
            // primeiro atualiza o tempo da fila atual
            Times[Customers] += e.tempo - tempo_global;
            tempo_global = e.tempo;
            Customers--;
            // computa a passagem do cliente para a proxima fila
            e.fila = e.fila_passagem;
            e.fila_passagem.Chegada(e);
        }

        void Saida(Evento e) {
            Times[Customers] += e.tempo - tempo_global;
            tempo_global = e.tempo;
            Customers--;
            if (Customers >= Server) {
                if (NextQ != null) {
                    eventos.add(new Evento('P', this));
                } else {
                    eventos.add(new Evento('S', this));
                }
            }
        }

        @Override
        public String toString(){
            StringBuilder sb = new StringBuilder();
            sb.append("Perda de clientes:")
              .append(Loss)
              .append("\n");
            for (int i = 0; i < Times.length; i++) {
                sb.append("Estado ")
                  .append(i)
                  .append(": tempo = ")
                  .append(Times[i])
                  .append("\t probabilidade = ")
                  .append((Times[i] / tempo_global) * 100)
                  .append("%\n");
            }
            return sb.toString();
        }
    }

    static PriorityQueue<Evento> eventos = new PriorityQueue<>(
            (Evento e1, Evento e2) -> Double.compare(e1.tempo, e2.tempo));

    static class Evento {
        char tipo;
        double tempo;
        Fila fila, fila_passagem;

        public Evento(char tipo, Fila fila_param) {
            this.tipo = tipo;
            this.fila = fila_param;
            if (tipo == 'C') {
                this.tempo = tempo_global
                        + (((fila_param.MaxArrival - fila_param.MinArrival) * nextRandom()) + fila_param.MinArrival);
            } else {
                this.tempo = tempo_global
                        + (((fila_param.MaxService - fila_param.MinService) * nextRandom()) + fila_param.MinService);
                if (tipo == 'P')
                    this.fila_passagem = fila_param.NextQ;
            }
        }

        // Overload para o caso de ser o primeiro evento do simulador
        public Evento(char tipo, double tempo, Fila fila) {
            this.tipo = tipo;
            this.tempo = tempo;
            this.fila = fila;
        }
    }

    static double nextRandom() {
        numero_previo = (a * numero_previo + c) % M;
        numeros_aleatorios_usados++;
        return numero_previo / M;
    }

    static void add_fila(Fila fila_param) {
        if (primeira_fila != null) {
            Fila f = primeira_fila;
            while (f.NextQ != null) {
                f = f.NextQ;
            }
            f.NextQ = fila_param;
        }
    }

    static void loadYamlConfig(String nome_arquivo) {
        Scanner scanner = null;
        try (InputStream inputStream = new FileInputStream(nome_arquivo)) {
            scanner = new Scanner(inputStream);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.isEmpty() || line.startsWith("#")) {
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

                if (line.startsWith("tempo_chegada_minimo")) {
                    int tempo_chegada_minimo = Integer.parseInt(value);
                    scanner.nextLine();
                    int tempo_chegada_maximo = Integer.parseInt(value);
                    scanner.nextLine();
                    int tempo_servico_minimo = Integer.parseInt(value);
                    scanner.nextLine();
                    int tempo_servico_maximo = Integer.parseInt(value);
                    scanner.nextLine();
                    int capacidade_fila = Integer.parseInt(value);
                    scanner.nextLine();
                    int num_servidores = Integer.parseInt(value);
                    Fila fila = new Fila(num_servidores, capacidade_fila, tempo_chegada_minimo, tempo_chegada_maximo,
                            tempo_servico_minimo, tempo_servico_maximo, null);
                    if (primeira_fila == null) {
                        primeira_fila = fila;
                    } else {
                        add_fila(fila);
                    }
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

    public static void main(String[] args) {
        loadYamlConfig("input_simulador.yml");
        numero_previo = seed;
       // eventos.add(new Evento('C', 2.000));

        while (numeros_aleatorios_usados < qtd_numeros_aleatorios) {
            Evento e = eventos.poll();
            if (e.tipo == 'C') {
                e.fila.Chegada(e);
            } else if (e.tipo == 'P') {
                e.fila.Passagem(e);
            } else if (e.tipo == 'S')
                e.fila.Saida(e);
        }

        System.out.printf("Tempo global da simulacao: %.4f\n", tempo_global);
        int i = 1;
        while (primeira_fila != null) {
            System.out.println("Fila " + i + ":");
            primeira_fila.toString();
            primeira_fila = primeira_fila.NextQ;
        }
        
    }
}

