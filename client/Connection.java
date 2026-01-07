package client;

import geral.Protocol;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Conexão que usa o Demultiplexer para permitir múltiplas threads
 * submeterem pedidos concorrentemente.
 */
public class Connection implements AutoCloseable {
    private final String host;
    private final int port;
    private Socket socket;
    private Demultiplexer demux;
    private boolean connected;
    
    public Connection(String host, int port) {
        this.host = host;
        this.port = port;
        this.connected = false;
    }
    
    public void connect() throws IOException {
        if (connected) {
            throw new IllegalStateException("Já conectado");
        }
        
        socket = new Socket(host, port);
        demux = new Demultiplexer(socket);
        connected = true;
    }
    
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
    
    /**
     * Envia um pedido e aguarda a response (thread-safe com Demultiplexer).
     * Múltiplas threads podem chamar isto concorrentemente sem se bloquearem.
     */
    private Protocol.Response sendRequest(Protocol.Request request) throws IOException {
        if (!isConnected()) {
            throw new IllegalStateException("Não conectado");
        }
        
        // Serializar request
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        request.writeTo(dos);
        dos.flush();
        byte[] requestData = baos.toByteArray();
        
        // Enviar via Demultiplexer (thread-safe)
        byte[] responseData = demux.send(requestData);
        
        // Desserializar response
        ByteArrayInputStream bais = new ByteArrayInputStream(responseData);
        DataInputStream dis = new DataInputStream(bais);
        return Protocol.Response.readFrom(dis, request.getOperation());
    }
    
    public Protocol.Response register(String username, String password) throws IOException {
        Protocol.Request request = new Protocol.Request(0, Protocol.OP_REGISTER); // tag será gerado pelo Demultiplexer
        request.setParam("username", username);
        request.setParam("password", password);
        return sendRequest(request);
    }
    
    public Protocol.Response login(String username, String password) throws IOException {
        Protocol.Request request = new Protocol.Request(0, Protocol.OP_LOGIN);
        request.setParam("username", username);
        request.setParam("password", password);
        return sendRequest(request);
    }
    
    public Protocol.Response logout() throws IOException {
        Protocol.Request request = new Protocol.Request(0, Protocol.OP_LOGOUT);
        return sendRequest(request);
    }
    
    public Protocol.Response addEvent(String product, int quantity, double price) throws IOException {
        Protocol.Request request = new Protocol.Request(0, Protocol.OP_ADD_EVENT);
        request.setParam("product", product);
        request.setParam("quantity", quantity);
        request.setParam("price", price);
        return sendRequest(request);
    }
    
    public Protocol.Response aggregateQuantity(String product, int days) throws IOException {
        Protocol.Request request = new Protocol.Request(0, Protocol.OP_QUANTITY_SOLD);
        request.setParam("product", product);
        request.setParam("days", days);
        return sendRequest(request);
    }
    
    public Protocol.Response aggregateVolume(String product, int days) throws IOException {
        Protocol.Request request = new Protocol.Request(0, Protocol.OP_SALES_VOLUME);
        request.setParam("product", product);
        request.setParam("days", days);
        return sendRequest(request);
    }
    
    public Protocol.Response aggregateAverage(String product, int days) throws IOException {
        Protocol.Request request = new Protocol.Request(0, Protocol.OP_AVERAGE_PRICE);
        request.setParam("product", product);
        request.setParam("days", days);
        return sendRequest(request);
    }
    
    public Protocol.Response aggregateMaxPrice(String product, int days) throws IOException {
        Protocol.Request request = new Protocol.Request(0, Protocol.OP_MAX_PRICE);
        request.setParam("product", product);
        request.setParam("days", days);
        return sendRequest(request);
    }
    
    public Protocol.Response filterEvents(java.util.List<String> products, int dayOffset) throws IOException {
        Protocol.Request request = new Protocol.Request(0, Protocol.OP_FILTER_EVENTS);
        request.setParam("products", products);
        request.setParam("dayOffset", dayOffset);
        return sendRequest(request);
    }
    
    public Protocol.Response simultaneousSales(String product1, String product2) throws IOException {
        Protocol.Request request = new Protocol.Request(0, Protocol.OP_SIMULTANEOUS_SALES);
        request.setParam("product1", product1);
        request.setParam("product2", product2);
        return sendRequest(request);
    }
    
    public Protocol.Response consecutiveSales(int n) throws IOException {
        Protocol.Request request = new Protocol.Request(0, Protocol.OP_CONSECUTIVE_SALES);
        request.setParam("n", n);
        return sendRequest(request);
    }
    
    @Override
    public void close() {
        if (!connected) {
            return;
        }
        
        try {
            if (demux != null) demux.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Erro ao fechar conexão: " + e.getMessage());
        } finally {
            connected = false;
        }
    }
}
