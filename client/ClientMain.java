package client;

import java.io.IOException;
import java.util.Scanner;

public class ClientMain {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 12345;
    
    private final Client client;
    private final Scanner scanner;
    private boolean running;
    
    public ClientMain(String host, int port) {
        this.client = new Client(host, port);
        this.scanner = new Scanner(System.in);
        this.running = false;
    }
    
    public void start() {
        System.out.println("=== Cliente de Eventos ===");
        
        try {
            client.connect();
            System.out.println("Conectado ao servidor");
        } catch (IOException e) {
            System.err.println("Erro ao conectar: " + e.getMessage());
            return;
        }
        
        running = true;
        printHelp();
        
        while (running) {
            try {
                System.out.print("> ");
                String line = scanner.nextLine().trim();
                
                if (line.isEmpty()) {
                    continue;
                }
                
                processCommand(line);
                
            } catch (Exception e) {
                System.err.println("Erro: " + e.getMessage());
            }
        }
        
        client.close();
        scanner.close();
    }
    
private void handleSimultaneousSales(String[] parts) throws IOException {
                    if (parts.length < 3) {
                        System.out.println("Uso: simul <produto1> <produto2>");
                        return;
                    }
                    String p1 = parts[1];
                    String p2 = parts[2];
                    System.out.println("Aguardando vendas simultâneas de " + p1 + " e " + p2 + " no dia corrente...");
                    Boolean result = client.simultaneousSales(p1, p2);
                    if (result == null) {
                        String lastError = client.getLastErrorMessage();
                        if (lastError == null || lastError.isEmpty()) lastError = "Erro desconhecido";
                        System.out.println("Erro: " + lastError);
                    } else if (result) {
                        System.out.println("Ambos os produtos foram vendidos no dia corrente!");
                    } else {
                        System.out.println("O dia terminou sem vendas simultâneas destes produtos.");
                    }
                }

    private void processCommand(String line) throws IOException {
        String[] parts = line.split("\\s+");
        String command = parts[0].toLowerCase();
        
        switch (command) {
                        case "simul":
                            handleSimultaneousSales(parts);
                            break;
                
                
            case "register":
                handleRegister(parts);
                break;
            case "login":
                handleLogin(parts);
                break;
            case "logout":
                handleLogout();
                break;
            case "add":
                handleAddEvent(parts);
                break;
            case "qty":
                handleAggregateQuantity(parts);
                break;
            case "vol":
                handleAggregateVolume(parts);
                break;
            case "avg":
                handleAggregateAverage(parts);
                break;
            case "max":
                handleAggregateMax(parts);
                break;
            case "filter":
                handleFilterEvents(parts);
                break;
            case "status":
                handleStatus();
                break;
            case "help":
                printHelp();
                break;
            case "quit":
                running = false;
                System.out.println("A sair...");
                break;
            default:
                System.out.println("Comando desconhecido. Digite 'help' para ajuda.");
        }
    }
    
        

    private void handleRegister(String[] parts) throws IOException {
        if (parts.length < 3) {
            System.out.println("Uso: register <username> <password>");
            return;
        }
        
        boolean success = client.register(parts[1], parts[2]);
        if (success) {
            System.out.println("Utilizador registado com sucesso!");
        } else {
            System.out.println("Erro ao registar. Username já existe?");
        }
    }
    
    private void handleLogin(String[] parts) throws IOException {
        if (parts.length < 3) {
            System.out.println("Uso: login <username> <password>");
            return;
        }
        
        boolean success = client.login(parts[1], parts[2]);
        if (success) {
            System.out.println("Login bem-sucedido! Bem-vindo " + parts[1]);
        } else {
            System.out.println("Login falhou. Credenciais inválidas?");
        }
    }
    
    private void handleLogout() throws IOException {
        boolean success = client.logout();
        if (success) {
            System.out.println("Logout bem-sucedido!");
        } else {
            System.out.println("Erro ao fazer logout");
        }
    }
    
    private void handleAddEvent(String[] parts) throws IOException {
        if (parts.length < 4) {
            System.out.println("Uso: add <produto> <quantidade> <preço>");
            return;
        }
        
        try {
            String product = parts[1];
            int quantity = Integer.parseInt(parts[2]);
            double price = Double.parseDouble(parts[3]);
            
            boolean success = client.addEvent(product, quantity, price);
            System.out.println(success ? "Evento adicionado" : "Erro ao adicionar evento");
            
        } catch (NumberFormatException e) {
            System.out.println("Quantidade e preço devem ser números");
        }
    }
    
    private void handleAggregateQuantity(String[] parts) throws IOException {
        if (parts.length < 3) {
            System.out.println("Uso: qty <produto> <dias>");
            return;
        }
        
        try {
            String product = parts[1];
            int days = Integer.parseInt(parts[2]);
            
            int result = client.aggregateQuantity(product, days);
            if (result >= 0) {
                System.out.println("Quantidade total: " + result);
            } else {
                System.out.println("Dados insuficientes ou erro");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Dias deve ser um número");
        }
    }
    
    private void handleAggregateVolume(String[] parts) throws IOException {
        if (parts.length < 3) {
            System.out.println("Uso: vol <produto> <dias>");
            return;
        }
        try {
            String product = parts[1];
            int days = Integer.parseInt(parts[2]);
            double result = client.aggregateVolume(product, days);
            if (result >= 0) {
                System.out.printf("Volume total: %.2f\n", result);
            } else {
                String lastError = client.getLastErrorMessage();
                if (lastError == null || lastError.isEmpty()) {
                    lastError = "Dados insuficientes ou erro";
                }
                System.out.println("Erro: " + lastError);
            }
        } catch (NumberFormatException e) {
            System.out.println("Dias deve ser um número");
        }
    }
    
    private void handleAggregateAverage(String[] parts) throws IOException {
        if (parts.length < 3) {
            System.out.println("Uso: avg <produto> <dias>");
            return;
        }
        
        try {
            String product = parts[1];
            int days = Integer.parseInt(parts[2]);
            
            double result = client.aggregateAverage(product, days);
            if (result >= 0) {
                System.out.printf("Preço médio: %.2f\n", result);
            } else {
                System.out.println("Dados insuficientes ou erro");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Dias deve ser um número");
        }
    }
    
    private void handleAggregateMax(String[] parts) throws IOException {
        if (parts.length < 3) {
            System.out.println("Uso: max <produto> <dias>");
            return;
        }
        
        try {
            String product = parts[1];
            int days = Integer.parseInt(parts[2]);
            
            double result = client.aggregateMaxPrice(product, days);
            if (result >= 0) {
                System.out.printf("Preço máximo: %.2f\n", result);
            } else {
                System.out.println("Dados insuficientes ou erro");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Dias deve ser um número");
        }
    }
    
    private void handleFilterEvents(String[] parts) throws IOException {
        if (parts.length < 3) {
            System.out.println("Uso: filter <days> <produto1> [produto2] ...");
            System.out.println("  days: 1=hoje, 2=hoje+ontem, 3=hoje+ontem+anteontem, etc.");
            return;
        }
        try {
            int days = Integer.parseInt(parts[1]);
            // Coletar produtos (do índice 2 em diante)
            java.util.List<String> products = new java.util.ArrayList<>();
            for (int i = 2; i < parts.length; i++) {
                products.add(parts[i]);
            }
            java.util.List<geral.Protocol.Event> events = client.filterEvents(products, days);
            if (events.isEmpty()) {
                System.out.println("Nenhum evento encontrado");
            } else {
                System.out.println("\n=== Eventos Encontrados ===");
                for (geral.Protocol.Event event : events) {
                    System.out.printf("%s: %d unidades a %.2f EUR = %.2f EUR\n",
                        event.getProduct(), 
                        event.getQuantity(), 
                        event.getPrice(),
                        event.getTotalValue());
                }
                System.out.println("Total de eventos: " + events.size());
                System.out.println("==========================\n");
            }
        } catch (NumberFormatException e) {
            System.out.println("days deve ser um número");
        }
    }
    
    private void handleStatus() {
        System.out.println("\n=== Estado do Cliente ===");
        System.out.println("Conectado: " + client.isConnected());
        System.out.println("Autenticado: " + client.isAuthenticated());
        if (client.isAuthenticated()) {
            System.out.println("Utilizador: " + client.getCurrentUser());
        }
        System.out.println("========================\n");
    }
    
    private void printHelp() {
        System.out.println("\n=== Comandos Disponíveis ===");
        System.out.println("register <user> <pass>     - Registar novo utilizador");
        System.out.println("login <user> <pass>        - Autenticar");
        System.out.println("logout                     - Terminar sessão");
        System.out.println("add <prod> <qty> <price>   - Adicionar evento");
        System.out.println("qty <prod> <days>          - Agregação quantidade");
        System.out.println("vol <prod> <days>          - Agregação volume de vendas");
        System.out.println("avg <prod> <days>          - Agregação preço médio");
        System.out.println("max <prod> <days>          - Agregação preço máximo");
        System.out.println("filter <days> <prod>...    - Filtrar eventos por produto(s)");
        System.out.println("simul <p1> <p2>            - Espera vendas simultâneas de dois produtos no dia corrente");
        System.out.println("status                     - Ver estado");
        System.out.println("help                       - Mostrar ajuda");
        System.out.println("quit                       - Sair");
        System.out.println("=============================\n");
    }
    
    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        
        if (args.length >= 1) {
            host = args[0];
        }
        
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida, usando " + DEFAULT_PORT);
            }
        }
        
        ClientMain ui = new ClientMain(host, port);
        ui.start();
    }
}
