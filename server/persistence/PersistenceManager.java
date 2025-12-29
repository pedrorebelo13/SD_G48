package server.persistence;

import java.io.IOException;
import java.util.List;
import server.auth.ServerManager;
import server.auth.User;
import server.data.TimeSeriesManager;

/**
 * Gestor centralizado de persistência.
 * Coordena guardar e carregar todos os dados do servidor.
 */
public class PersistenceManager {
    private static final String DEFAULT_DATA_DIR = "data";
    private static final String USERS_FILE = "users.dat";
    private static final String TIMESERIES_FILE = "timeseries.dat";
    
    private final String dataDirectory;
    private final UserPersistence userPersistence;
    private final TimeSeriesPersistence timeSeriesPersistence;
    
    public PersistenceManager() {
        this(DEFAULT_DATA_DIR);
    }
    
    public PersistenceManager(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.userPersistence = new UserPersistence(dataDirectory + "/" + USERS_FILE);
        this.timeSeriesPersistence = new TimeSeriesPersistence(dataDirectory + "/" + TIMESERIES_FILE);
    }
    
    /**
     * Guarda todos os dados do servidor.
     * @param serverManager Gestor de autenticação
     * @param tsManager Gestor de séries temporais
     * @throws IOException se falhar a escrita
     */
    public void saveAll(ServerManager serverManager, TimeSeriesManager tsManager) throws IOException {
        System.out.println("A guardar dados...");
        
        // Guardar utilizadores
        List<User> users = getAllUsers(serverManager);
        userPersistence.save(users);
        System.out.println("  - Utilizadores: " + users.size());
        
        // Guardar séries temporais
        timeSeriesPersistence.save(tsManager);
        System.out.println("  - Dia corrente: " + tsManager.getCurrentDayId());
        System.out.println("  - Dias históricos: " + tsManager.getHistoricalDayCount());
        System.out.println("  - Eventos hoje: " + tsManager.getCurrentDay().getEventCount());
        
        System.out.println("Dados guardados com sucesso!");
    }
    
    /**
     * Carrega todos os dados do servidor.
     * @param serverManager Gestor de autenticação (será preenchido)
     * @param maxDays Valor de D para criar TimeSeriesManager se necessário
     * @return TimeSeriesManager carregado ou novo se não existir
     * @throws IOException se falhar a leitura
     */
    public TimeSeriesManager loadAll(ServerManager serverManager, int maxDays) throws IOException {
        System.out.println("A carregar dados...");
        
        // Carregar utilizadores
        List<User> users = userPersistence.load();
        for (User user : users) {
            serverManager.register(user);
        }
        System.out.println("  - Utilizadores: " + users.size());
        
        // Carregar séries temporais
        TimeSeriesManager tsManager = timeSeriesPersistence.load();
        
        if (tsManager == null) {
            System.out.println("  - Nenhuma série temporal encontrada, criando nova");
            tsManager = new TimeSeriesManager(maxDays);
        } else {
            System.out.println("  - Dia corrente: " + tsManager.getCurrentDayId());
            System.out.println("  - Dias históricos: " + tsManager.getHistoricalDayCount());
            System.out.println("  - Eventos hoje: " + tsManager.getCurrentDay().getEventCount());
        }
        
        System.out.println("Dados carregados com sucesso!");
        return tsManager;
    }
    
    /**
     * Guarda apenas utilizadores.
     * @param serverManager Gestor de autenticação
     * @throws IOException se falhar a escrita
     */
    public void saveUsers(ServerManager serverManager) throws IOException {
        List<User> users = getAllUsers(serverManager);
        userPersistence.save(users);
    }
    
    /**
     * Guarda apenas séries temporais.
     * @param tsManager Gestor de séries temporais
     * @throws IOException se falhar a escrita
     */
    public void saveTimeSeries(TimeSeriesManager tsManager) throws IOException {
        timeSeriesPersistence.save(tsManager);
    }
    
    /**
     * Obtém todos os utilizadores do ServerManager.
     * Como não temos um método público para isso, usamos reflexão ou
     * adicionamos um método no ServerManager.
     */
    private List<User> getAllUsers(ServerManager serverManager) {
        // NOTA: Isto requer adicionar um método getAllUsers() no ServerManager
        // Por agora, retornamos lista vazia se não tivermos acesso
        // Vou assumir que vamos adicionar esse método
        return serverManager.getAllUsers();
    }
    
    /**
     * Obtém o diretório de dados.
     * @return Caminho do diretório
     */
    public String getDataDirectory() {
        return dataDirectory;
    }
}
