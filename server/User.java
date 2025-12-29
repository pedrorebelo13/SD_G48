package server.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Representa um utilizador do sistema.
 */
public class User {
    private final String username;
    private final byte[] passwordHash;
    
    /**
     * Cria um novo utilizador com password em hash.
     * @param username Nome de utilizador
     * @param password Password em texto limpo (será guardada em hash)
     */
    public User(String username, String password) {
        this.username = username;
        this.passwordHash = hashPassword(password);
    }
    
    /**
     * Construtor público para criar utilizador com hash já calculado (persistência).
     */
    public User(String username, byte[] passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }
    
    public String getUsername() {
        return username;
    }
    
    public byte[] getPasswordHash() {
        return passwordHash.clone();
    }
    
    /**
     * Verifica se a password fornecida corresponde à password do utilizador.
     * @param password Password a verificar
     * @return true se a password está correta
     */
    public boolean checkPassword(String password) {
        byte[] hash = hashPassword(password);
        return Arrays.equals(passwordHash, hash);
    }
    
    /**
     * Calcula o hash SHA-256 de uma password.
     * @param password Password em texto limpo
     * @return Hash da password
     */
    private static byte[] hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(password.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            // SHA-256 está sempre disponível
            throw new RuntimeException("Erro ao calcular hash da password", e);
        }
    }
    
    @Override
    public String toString() {
        return "User[" + username + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof User)) return false;
        User other = (User) obj;
        return username.equals(other.username);
    }
    
    @Override
    public int hashCode() {
        return username.hashCode();
    }
}
