package client;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demultiplexer para sincronização de múltiplas threads.
 * Permite que várias threads enviem pedidos concorrentemente sem se bloquearem mutuamente.
 * Uma thread lê do socket e distribui as respostas para as threads corretas.
 */
public class Demultiplexer implements AutoCloseable {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final ReentrantLock sendLock = new ReentrantLock();
    private final ReentrantLock mapLock = new ReentrantLock();
    
    // Mapa para guardar as threads à espera de resposta
    // Chave: ID do pedido (Tag), Valor: Classe auxiliar com a resposta e condição de espera
    private final Map<Integer, Entry> pendingRequests = new HashMap<>();
    
    private int nextTag = 0;
    private IOException exception = null; // Para propagar erros do socket

    public Demultiplexer(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        
        // Thread Reader (O "Multiplexer" de leitura)
        // Esta thread lê tudo do socket e distribui para quem pediu
        Thread readerThread = new Thread(this::listenLoop, "Demux-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    // Classe auxiliar para sincronização (Wait/Notify para cada pedido específico)
    private static class Entry {
        final Condition cond;
        byte[] data;

        Entry(ReentrantLock lock) {
            this.cond = lock.newCondition();
        }
    }

    /**
     * Método usado pelas várias threads do cliente.
     * Envia um pedido e bloqueia até a resposta específica desse pedido chegar.
     */
    public byte[] send(byte[] data) throws IOException {
        int tag;
        Entry entry;

        // 1. Registar o pedido e obter uma Tag única
        mapLock.lock();
        try {
            if (exception != null) throw exception;
            tag = nextTag++;
            entry = new Entry(mapLock);
            pendingRequests.put(tag, entry);
        } finally {
            mapLock.unlock();
        }

        // 2. Enviar dados (Atomicamente para não misturar bytes de threads diferentes)
        sendLock.lock();
        try {
            out.writeInt(tag);
            out.writeInt(data.length);
            out.write(data);
            out.flush();
        } finally {
            sendLock.unlock();
        }

        // 3. Bloquear à espera da resposta específica (Demultiplexing)
        mapLock.lock();
        try {
            // Enquanto não houver dados e não houver erro, espera
            while (entry.data == null && exception == null) {
                entry.cond.await();
            }
            
            if (exception != null) throw exception;
            return entry.data;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Thread interrompida enquanto esperava resposta");
        } finally {
            pendingRequests.remove(tag); // Limpeza
            mapLock.unlock();
        }
    }

    /**
     * Loop da thread de leitura (Background).
     * Lê respostas do servidor e "acorda" a thread correta.
     */
    private void listenLoop() {
        try {
            while (true) {
                // Ler cabeçalho
                int tag = in.readInt();
                int len = in.readInt();
                
                // Ler corpo da mensagem
                byte[] data = new byte[len];
                in.readFully(data);

                // Entregar a mensagem à thread correta
                mapLock.lock();
                try {
                    Entry entry = pendingRequests.get(tag);
                    if (entry != null) {
                        entry.data = data;
                        entry.cond.signal(); // Acorda APENAS a thread que fez este pedido
                    }
                } finally {
                    mapLock.unlock();
                }
            }
        } catch (IOException e) {
            // Se o socket fechar ou der erro, avisar toda a gente
            mapLock.lock();
            try {
                this.exception = e;
                // Acordar todas as threads pendentes para lançarem exceção
                for (Entry entry : pendingRequests.values()) {
                    entry.cond.signalAll();
                }
            } finally {
                mapLock.unlock();
            }
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
