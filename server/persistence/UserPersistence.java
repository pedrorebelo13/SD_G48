package server.persistence;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import server.auth.User;

/**
 * Persistência de utilizadores.
 * Guarda e carrega utilizadores em formato binário.
 */
public class UserPersistence {
    private final String filePath;
    
    public UserPersistence(String filePath) {
        this.filePath = filePath;
    }
    
    /**
     * Guarda lista de utilizadores no disco.
     * @param users Lista de utilizadores
     * @throws IOException se falhar a escrita
     */
    public void save(List<User> users) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            
            // Header
            out.writeInt(0x55534552); // "USER" magic number
            out.writeInt(1); // Versão
            
            // Número de utilizadores
            out.writeInt(users.size());
            
            // Cada utilizador
            for (User user : users) {
                writeUser(out, user);
            }
        }
    }
    
    /**
     * Carrega lista de utilizadores do disco.
     * @return Lista de utilizadores ou lista vazia se não existir
     * @throws IOException se falhar a leitura
     */
    public List<User> load() throws IOException {
        File file = new File(filePath);
        
        if (!file.exists()) {
            return new ArrayList<>();
        }
        
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            
            // Verificar header
            int magic = in.readInt();
            if (magic != 0x55534552) {
                throw new IOException("Ficheiro inválido (magic number incorreto)");
            }
            
            int version = in.readInt();
            if (version != 1) {
                throw new IOException("Versão não suportada: " + version);
            }
            
            // Número de utilizadores
            int count = in.readInt();
            List<User> users = new ArrayList<>(count);
            
            // Ler cada utilizador
            for (int i = 0; i < count; i++) {
                users.add(readUser(in));
            }
            
            return users;
        }
    }
    
    /**
     * Escreve um utilizador.
     */
    private void writeUser(DataOutputStream out, User user) throws IOException {
        // Username
        byte[] usernameBytes = user.getUsername().getBytes("UTF-8");
        out.writeInt(usernameBytes.length);
        out.write(usernameBytes);
        
        // Password hash
        byte[] passwordHash = user.getPasswordHash();
        out.writeInt(passwordHash.length);
        out.write(passwordHash);
    }
    
    /**
     * Lê um utilizador.
     */
    private User readUser(DataInputStream in) throws IOException {
        // Username
        int usernameLen = in.readInt();
        byte[] usernameBytes = new byte[usernameLen];
        in.readFully(usernameBytes);
        String username = new String(usernameBytes, "UTF-8");
        
        // Password hash
        int hashLen = in.readInt();
        byte[] passwordHash = new byte[hashLen];
        in.readFully(passwordHash);
        
        return new User(username, passwordHash);
    }
}
