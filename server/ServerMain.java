package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import server.aggregation.AggregationService;
import server.auth.ServerManager;
import server.cache.ProductCache;
import server.data.TimeSeriesManager;
import server.persistence.PersistenceManager;

/**
 * Servidor principal para gestão de eventos e agregações.
 * Aceita conexões TCP e cria threads para cada cliente.
 */
public class ServerMain {
    private static final int DEFAULT_PORT = 12345;
    private static final int DEFAULT_D = 30; // Dias históricos
    private static final int DEFAULT_S = 100; // Séries em cache
    
    private final int port;
    private final ServerManager serverManager;
    private TimeSeriesManager tsManager;
    private final ProductCache cache;
    private final AggregationService aggregationService;
    private final ExecutorService threadPool;
    private final AtomicBoolean running;
    private final PersistenceManager persistenceManager;
    private ServerSocket serverSocket;
    private final int maxDays;
    
    public ServerMain(int port, int maxDays, int maxSeries) {
        this.port = port;
        this.maxDays = maxDays;
        this.serverManager = new ServerManager();
        this.persistenceManager = new PersistenceManager();
        
        // Tentar carregar dados persistidos
        try {
            this.tsManager = persistenceManager.loadAll(serverManager, maxDays);
        } catch (IOException e) {
            System.err.println("Erro ao carregar dados: " + e.getMessage());
            System.err.println("Criando novo estado...");
            this.tsManager = new TimeSeriesManager(maxDays);
        }
        
        this.cache = new ProductCache(maxSeries);
        this.aggregationService = new AggregationService(tsManager, cache);
        this.threadPool = Executors.newCachedThreadPool();
        this.running = new AtomicBoolean(false);
    }
    
    /**
     * Inicia o servidor.
     */
    public void start() throws IOException {
        if (running.get()) {
            throw new IllegalStateException("Servidor já está a correr");
        }
        
        serverSocket = new ServerSocket(port);
        running.set(true);
        
        System.out.println("Servidor iniciado na porta " + port);
        System.out.println("Configuração: D=" + tsManager.getMaxDays() + ", S=" + cache.getMaxSeries());
        System.out.println("Comandos: 'newday' para simular novo dia, 'stats' para estatísticas, 'quit' para sair");
        
        // Thread para aceitar conexões
        Thread acceptThread = new Thread(() -> acceptConnections());
        acceptThread.start();
        
        // Thread para comandos do servidor
        Thread commandThread = new Thread(() -> handleCommands());
        commandThread.start();
    }
    
    /**
     * Aceita conexões de clientes.
     */
    private void acceptConnections() {
        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nova conexão de: " + clientSocket.getInetAddress());
                
                // Criar handler para o cliente
                ClientHandler handler = new ClientHandler(
                    clientSocket,
                    serverManager,
                    tsManager,
                    aggregationService
                );
                
                threadPool.execute(handler);
                
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("Erro ao aceitar conexão: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Processa comandos do servidor.
     */
    private void handleCommands() {
        Scanner scanner = new Scanner(System.in);
        
        while (running.get()) {
            try {
                String command = scanner.nextLine().trim().toLowerCase();
                
                switch (command) {
                    case "newday":
                        simulateNewDay();
                        break;
                        
                    case "stats":
                        printStatistics();
                        break;
                        
                    case "save":
                        saveData();
                        break;
                        
                    case "quit":
                    case "exit":
                        shutdown();
                        break;
                        
                    case "help":
                        printHelp();
                        break;
                        
                    default:
                        System.out.println("Comando desconhecido. Digite 'help' para ajuda.");
                }
                
            } catch (Exception e) {
                System.err.println("Erro ao processar comando: " + e.getMessage());
            }
        }
        
        scanner.close();
    }
    
    /**
     * Simula o início de um novo dia.
     */
    private void simulateNewDay() {
        int oldDay = tsManager.getCurrentDayId();
        tsManager.newDay();
        aggregationService.invalidateOnNewDay();
        int newDay = tsManager.getCurrentDayId();
        
        System.out.println("Novo dia simulado: " + oldDay + " -> " + newDay);
        System.out.println("Cache invalidado");
        
        // Guardar automaticamente após mudança de dia
        try {
            persistenceManager.saveAll(serverManager, tsManager);
        } catch (IOException e) {
            System.err.println("Erro ao guardar dados: " + e.getMessage());
        }
    }
    
    /**
     * Guarda os dados no disco.
     */
    private void saveData() {
        try {
            persistenceManager.saveAll(serverManager, tsManager);
        } catch (IOException e) {
            System.err.println("Erro ao guardar dados: " + e.getMessage());
        }
    }
    
    /**
     * Imprime estatísticas do servidor.
     */
    private void printStatistics() {
        System.out.println("\n=== Estatísticas do Servidor ===");
        System.out.println("Utilizadores registados: " + serverManager.getUserCount());
        System.out.println("Dia corrente: " + tsManager.getCurrentDayId());
        System.out.println("Eventos hoje: " + tsManager.getCurrentDay().getEventCount());
        System.out.println("Dias históricos: " + tsManager.getHistoricalDayCount() + "/" + tsManager.getMaxDays());
        System.out.println("Cache: " + cache.size() + "/" + cache.getMaxSeries());
        System.out.println("================================\n");
    }
    
    /**
     * Imprime ajuda dos comandos.
     */
    private void printHelp() {
        System.out.println("\n=== Comandos Disponíveis ===");
        System.out.println("newday  - Simula o início de um novo dia");
        System.out.println("stats   - Mostra estatísticas do servidor");
        System.out.println("help    - Mostra esta ajuda");
        System.out.println("quit    - Encerra o servidor");
        System.out.println("============================\n");
    }
    
    /**
     * Encerra o servidor gracefully.
     */
    public void shutdown() {
        if (!running.get()) {
            return;
        }
        
        System.out.println("Encerrando servidor...");
        
        // Guardar dados antes de encerrar
        try {
            persistenceManager.saveAll(serverManager, tsManager);
        } catch (IOException e) {
            System.err.println("Erro ao guardar dados: " + e.getMessage());
        }
        
        running.set(false);
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar ServerSocket: " + e.getMessage());
        }
        
        threadPool.shutdown();
        System.out.println("Servidor encerrado");
    }
    
    /**
     * Ponto de entrada do servidor.
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        int maxDays = DEFAULT_D;
        int maxSeries = DEFAULT_S;
        
        // Parse argumentos
        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida, usando " + DEFAULT_PORT);
            }
        }
        
        if (args.length >= 2) {
            try {
                maxDays = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("D inválido, usando " + DEFAULT_D);
            }
        }
        
        if (args.length >= 3) {
            try {
                maxSeries = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.err.println("S inválido, usando " + DEFAULT_S);
            }
        }
        
        try {
            ServerMain server = new ServerMain(port, maxDays, maxSeries);
            server.start();
        } catch (IOException e) {
            System.err.println("Erro ao iniciar servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
