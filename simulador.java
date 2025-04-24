import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
    static ArrayList<Fila> filas;

    static class FilaEProbabilidade{
        Fila fila;
        double probabilidade;
        boolean saida;

        public FilaEProbabilidade(Fila fila, double probabilidade) {
            this.fila = fila;
            this.probabilidade = probabilidade;
            this.saida = false;
        }

        // pra indicar probabilidade de saida do sistema
        public FilaEProbabilidade(boolean saida, double probabilidade) {
            this.saida = saida;
            this.probabilidade = probabilidade;
        }
    }

    static class Fila {
        int Server;
        int Capacity;
        double MinArrival;
        double MaxArrival;
        double MinService;
        double MaxService;
        int Customers;
        int Loss;
        //double[] Times;
        Map<Integer, Double> Times;
        //PriorityQueue<FilaEProbabilidade> filas_conectadas;
        List<FilaEProbabilidade> filas_conectadas;
        double Timestamp;

        public Fila(int Server, int Capacity, double MinArrival, double MaxArrival,
                double MinService, double MaxService) {
            this.Server = Server;
            this.Capacity = Capacity;
            this.MinArrival = MinArrival;
            this.MaxArrival = MaxArrival;
            this.MinService = MinService;
            this.MaxService = MaxService;
            //this.Times = new double[Capacity + 1]; // nao vai funcionar para capacidade infinita
            this.Times = new HashMap<>();
            this.Customers = 0;
            this.Loss = 0;
            this.Timestamp = 0.0;
            filas_conectadas = new ArrayList<>();
        }

        void Chegada(Evento e) {
            //Times[Customers] += e.tempo - Timestamp;
            Times.put(Customers, Times.getOrDefault(Customers, 0.0) + (e.tempo - Timestamp));
            Timestamp = e.tempo;
            tempo_global = e.tempo;
            if (Customers < Capacity) {
                Customers++;
                if (Customers <= Server) {
                    if (filas_conectadas.size() > 0) 
                        eventos.add(new Evento('P', this));
                    else
                        eventos.add(new Evento('S', this)); // se nao tiver nenhuma fila conectada, agenda saida diretamente
                }
            } else {
                Loss++;
            }
            if (e.tipo == 'C') // se for passagem nao deve criar evento de chegada
                eventos.add(new Evento('C', this));
        }

        void Passagem(Evento e){
            // computa a saida da fila atual
            Saida(e);
            // computa a passagem do cliente para a proxima fila ou saida do sistema com base nas probabilidades
            double probabilidade_passagem = nextRandom();
            double acumulada = 0;
            for(FilaEProbabilidade fila : filas_conectadas) {
                acumulada += fila.probabilidade;
                if (probabilidade_passagem < acumulada) {
                    if (!fila.saida){
                        e.fila = fila.fila;
                        e.fila.Chegada(e);                 
                    }
                    return;
                }
            }
            // Garantia: se nenhuma transição foi feita (por p >= 1.0), força a última
            FilaEProbabilidade ultima = filas_conectadas.get(filas_conectadas.size() - 1);
            if (!ultima.saida) {
                e.fila = ultima.fila;
                e.fila.Chegada(e);
            }
        }

        void Saida(Evento e) {
            //Times[Customers] += e.tempo - Timestamp;
            Times.put(Customers, Times.getOrDefault(Customers, 0.0) + (e.tempo - Timestamp));
            Timestamp = e.tempo;
            tempo_global = e.tempo;
            Customers--;
            if (Customers >= Server) {
                if (filas_conectadas.size() != 0) {
                    eventos.add(new Evento('P', this));
                } else {
                    eventos.add(new Evento('S', this));
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Perda de clientes:")
              .append(Loss)
              .append("\n");

            // for (int i = 0; i < Times.length; i++) {
            // sb.append("Estado ")
            //   .append(i)
            //   .append(": tempo = ")
            //   .append(Times[i])
            //   .append("\t probabilidade = ")
            //   .append((Times[i] / tempo_global) * 100)
            //   .append("%\n");
            // }
            for (Map.Entry<Integer, Double> entry : Times.entrySet()) {
                int estado = entry.getKey();
                double tempo = entry.getValue();
                sb.append("Estado ")
                  .append(estado)
                  .append(": tempo = ")
                  .append(tempo)
                  .append("\t probabilidade = ")
                  .append((tempo / tempo_global) * 100)
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
        Fila fila;

        public Evento(char tipo, Fila fila_param) {
            this.tipo = tipo;
            this.fila = fila_param;
            if (tipo == 'C') {
                this.tempo = tempo_global
                        + (((fila_param.MaxArrival - fila_param.MinArrival) * nextRandom()) + fila_param.MinArrival);
            } else {
                this.tempo = tempo_global
                        + (((fila_param.MaxService - fila_param.MinService) * nextRandom()) + fila_param.MinService);
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

    static String getValueYml(Scanner s){
        String line = s.nextLine();
        String [] parts = line.split(":");
        String value = parts[1].trim();
        return value;
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
                String value = "";
                if (parts.length == 2) {
                    value = parts[1].trim();
                }
                
                if (key.equals("a"))
                    a = Integer.parseInt(value);
                else if (key.equals("c"))
                    c = Integer.parseInt(value);
                else if (key.equals("M"))
                    M = Integer.parseInt(value);
                else if (key.equals("seed"))
                    seed = Double.parseDouble(value);
                else if (key.equals("qtd_numeros_aleatorios"))
                    qtd_numeros_aleatorios = Integer.parseInt(value);
                

                if (line.startsWith("tempo_chegada_minimo")) {
                    double tempo_chegada_minimo = Double.parseDouble(value);
                    value = getValueYml(scanner);
                    double tempo_chegada_maximo = Double.parseDouble(value);
                    value = getValueYml(scanner);
                    double tempo_servico_minimo = Double.parseDouble(value);
                    value = getValueYml(scanner);
                    double tempo_servico_maximo = Double.parseDouble(value);
                    value = getValueYml(scanner);
                    int num_servidores = Integer.parseInt(value);
                    value = getValueYml(scanner);
                    int capacidade_fila;
                    if (value.equalsIgnoreCase("infinito") || value.equals("-1")) {
                        capacidade_fila = Integer.MAX_VALUE;
                    } else {
                        capacidade_fila = Integer.parseInt(value);
                    }
                    Fila fila = new Fila(num_servidores, capacidade_fila, tempo_chegada_minimo, tempo_chegada_maximo,
                            tempo_servico_minimo, tempo_servico_maximo);
                    
                    filas.add(fila);
                }

                if (line.startsWith("transicoes")){
                    while (scanner.hasNextLine()){
                        line = scanner.nextLine().trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;

                        if (line.startsWith("- origem:")) {
                            int origem = Integer.parseInt(line.split(":")[1].trim()) - 1;

                            line = scanner.nextLine().trim();
                            String destinoStr = line.split(":")[1].trim();

                            line = scanner.nextLine().trim();
                            double prob = Double.parseDouble(line.split(":")[1].trim());

                            if (destinoStr.equals("saida")) {
                                filas.get(origem).filas_conectadas.add(new FilaEProbabilidade(true, prob));
                            } else {
                                int destino = Integer.parseInt(destinoStr) - 1;
                                filas.get(origem).filas_conectadas.add(new FilaEProbabilidade(filas.get(destino), prob));
                            }
                        }
                    }
                }
                for (Fila fila : filas) {
                    fila.filas_conectadas.sort(Comparator.comparingDouble(f -> f.probabilidade)); 
                }              
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    public static void main(String[] args) {
        filas = new ArrayList<>();
        loadYamlConfig("input_simulador.yml");
        numero_previo = seed;
        eventos.add(new Evento('C', 2, filas.get(0))); // se a primeira fila do sistema n for declarada primeiro no yml isso n funciona

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
        for (Fila fila : filas) {
            //fila.Times[fila.Customers] += tempo_global - fila.Timestamp;
            fila.Times.put(fila.Customers, fila.Times.getOrDefault(fila.Customers, 0.0) + (tempo_global - fila.Timestamp));
            System.out.println("Fila " + i + ":");
            System.out.println(fila.toString());
            i++;
        }
    }
}

