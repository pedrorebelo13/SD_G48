package server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// Gestor de autenticação de utilizadores
public class Authentication {
    private final Map<String, User> users;
    private final ReentrantReadWriteLock lock;
    
    public Authentication() {
        this.users = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }
    
    // Regista um novo utilizador no sistema
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
    
    // Regista um utilizador já existente (para persistência)
    public void register(User user) {
        lock.writeLock().lock();
        try {
            users.put(user.getUsername(), user);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // Autentica um utilizador
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
    
    // Obtém o número total de utilizadores registados.
    public int getUserCount() {
        lock.readLock().lock();
        try {
            return users.size();
        } finally {
            lock.readLock().unlock();
        }
    }


    // Obtém todos os utilizadores (para persistência)
    public List<User> getAllUsers() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(users.values());
        } finally {
            lock.readLock().unlock();
        }
    }
}
