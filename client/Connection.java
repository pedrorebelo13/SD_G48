    package client;

import geral.Protocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;


public class Connection implements AutoCloseable {
    private final String host;
    private final int port;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean connected;
    private final AtomicInteger requestIdCounter;
    
    public Connection(String host, int port) {
        this.host = host;
        this.port = port;
        this.connected = false;
        this.requestIdCounter = new AtomicInteger(1);
    }
    
    public void connect() throws IOException {
        if (connected) {
            throw new IllegalStateException("Já conectado");
        }
        
        socket = new Socket(host, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        connected = true;
    }
    
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
    
    public Protocol.Response sendRequest(Protocol.Request request) throws IOException {
        if (!isConnected()) {
            throw new IllegalStateException("Não conectado");
        }
        
        request.writeTo(out);
        return Protocol.Response.readFrom(in, request.getOperation());
    }
    
    public Protocol.Response register(String username, String password) throws IOException {
        Protocol.Request request = new Protocol.Request(requestIdCounter.getAndIncrement(), Protocol.OP_REGISTER);
        request.setParam("username", username);
        request.setParam("password", password);
        return sendRequest(request);
    }
    
    public Protocol.Response login(String username, String password) throws IOException {
        Protocol.Request request = new Protocol.Request(requestIdCounter.getAndIncrement(), Protocol.OP_LOGIN);
        request.setParam("username", username);
        request.setParam("password", password);
        return sendRequest(request);
    }
    
    public Protocol.Response logout() throws IOException {
        Protocol.Request request = new Protocol.Request(requestIdCounter.getAndIncrement(), Protocol.OP_LOGOUT);
        return sendRequest(request);
    }
    
    public Protocol.Response addEvent(String product, int quantity, double price) throws IOException {
        Protocol.Request request = new Protocol.Request(requestIdCounter.getAndIncrement(), Protocol.OP_ADD_EVENT);
        request.setParam("product", product);
        request.setParam("quantity", quantity);
        request.setParam("price", price);
        return sendRequest(request);
    }
    
    public Protocol.Response aggregateQuantity(String product, int days) throws IOException {
        Protocol.Request request = new Protocol.Request(requestIdCounter.getAndIncrement(), Protocol.OP_QUANTITY_SOLD);
        request.setParam("product", product);
        request.setParam("days", days);
        return sendRequest(request);
    }
    
    public Protocol.Response aggregateVolume(String product, int days) throws IOException {
        Protocol.Request request = new Protocol.Request(requestIdCounter.getAndIncrement(), Protocol.OP_SALES_VOLUME);
        request.setParam("product", product);
        request.setParam("days", days);
        return sendRequest(request);
    }
    
    public Protocol.Response aggregateAverage(String product, int days) throws IOException {
        Protocol.Request request = new Protocol.Request(requestIdCounter.getAndIncrement(), Protocol.OP_AVERAGE_PRICE);
        request.setParam("product", product);
        request.setParam("days", days);
        return sendRequest(request);
    }
    
    public Protocol.Response aggregateMaxPrice(String product, int days) throws IOException {
        Protocol.Request request = new Protocol.Request(requestIdCounter.getAndIncrement(), Protocol.OP_MAX_PRICE);
        request.setParam("product", product);
        request.setParam("days", days);
        return sendRequest(request);
    }
    
    public Protocol.Response filterEvents(java.util.List<String> products, int dayOffset) throws IOException { //envia pedido de filtrar eventos
        Protocol.Request request = new Protocol.Request(requestIdCounter.getAndIncrement(), Protocol.OP_FILTER_EVENTS);
        request.setParam("products", products);
        request.setParam("dayOffset", dayOffset);
        return sendRequest(request);
    }
    
    public Protocol.Response simultaneousSales(String product1, String product2) throws IOException {
        Protocol.Request request = new Protocol.Request(requestIdCounter.getAndIncrement(), Protocol.OP_SIMULTANEOUS_SALES);
        request.setParam("product1", product1);
        request.setParam("product2", product2);
        return sendRequest(request);
    }
    
    public Protocol.Response consecutiveSales(int n) throws IOException {
        Protocol.Request request = new Protocol.Request(requestIdCounter.getAndIncrement(), Protocol.OP_CONSECUTIVE_SALES);
        request.setParam("n", n);
        return sendRequest(request);
    }
    
    @Override
    public void close() {
        if (!connected) {
            return;
        }
        
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Erro ao fechar conexão: " + e.getMessage());
        } finally {
            connected = false;
        }
    }
}
