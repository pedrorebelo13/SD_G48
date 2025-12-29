package client;

import java.io.IOException;
import java.util.Scanner;

/**
 * Interface de utilizador simples para testar o cliente.
 */
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
    
    private void processCommand(String line) throws IOException {
        String[] parts = line.split("\\s+");
        String command = parts[0].toLowerCase();
        
        switch (command) {
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
                
            case "rev":
                handleAggregateRevenue(parts);
                break;
                
            case "avg":
                handleAggregateAverage(parts);
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
    
    private void handleAggregateRevenue(String[] parts) throws IOException {
        if (parts.length < 3) {
            System.out.println("Uso: rev <produto> <dias>");
            return;
        }
        
        try {
            String product = parts[1];
            int days = Integer.parseInt(parts[2]);
            
            double result = client.aggregateRevenue(product, days);
            if (result >= 0) {
                System.out.printf("Receita total: %.2f\n", result);
            } else {
                System.out.println("Dados insuficientes ou erro");
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
        System.out.println("rev <prod> <days>          - Agregação receita");
        System.out.println("avg <prod> <days>          - Agregação preço médio");
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
