package server.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Gestor de autenticação de utilizadores.
 * Thread-safe para acesso concorrente de múltiplos clientes.
 */
public class ServerManager {
    private final Map<String, User> users;
    private final ReentrantReadWriteLock lock;
    
    public ServerManager() {
        this.users = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }
    
    /**
     * Regista um novo utilizador no sistema.
     * @param username Nome de utilizador
     * @param password Password
     * @return true se o registo foi bem sucedido, false se o utilizador já existe
     */
    public boolean register(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        if (password == null || password.isEmpty()) {
            return false;
        }
        
        lock.writeLock().lock();
        try {
            // Verificar se o utilizador já existe
            if (users.containsKey(username)) {
                return false;
            }
            
            // Criar e guardar novo utilizador
            User user = new User(username, password);
            users.put(username, user);
            return true;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Autentica um utilizador.
     * @param username Nome de utilizador
     * @param password Password
     * @return User se autenticação bem sucedida, null caso contrário
     */
    public User authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        
        lock.readLock().lock();
        try {
            User user = users.get(username);
            
            if (user == null) {
                return null; // Utilizador não existe
            }
            
            if (user.checkPassword(password)) {
                return user;
            }
            
            return null; // Password incorreta
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Verifica se um utilizador existe.
     * @param username Nome de utilizador
     * @return true se o utilizador existe
     */
    public boolean userExists(String username) {
        lock.readLock().lock();
        try {
            return users.containsKey(username);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Obtém o número total de utilizadores registados.
     * @return Número de utilizadores
     */
    public int getUserCount() {
        return users.size();
    }
    
    /**
     * Remove um utilizador (útil para testes).
     * @param username Nome de utilizador a remover
     * @return true se o utilizador foi removido
     */
    public boolean removeUser(String username) {
        lock.writeLock().lock();
        try {
            return users.remove(username) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Limpa todos os utilizadores (útil para testes).
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            users.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Obtém todos os utilizadores (para persistência).
     * @return Lista com todos os utilizadores
     */
    public java.util.List<User> getAllUsers() {
        lock.readLock().lock();
        try {
            return new java.util.ArrayList<>(users.values());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Regista um utilizador já existente (com hash de password).
     * Usado pela camada de persistência.
     * @param user Utilizador a registar
     * @return true se sucesso
     */
    public boolean register(User user) {
        if (user == null) {
            return false;
        }
        
        lock.writeLock().lock();
        try {
            if (users.containsKey(user.getUsername())) {
                return false;
            }
            users.put(user.getUsername(), user);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
