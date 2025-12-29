package server;

import geral.Protocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import server.aggregation.AggregationService;
import server.auth.ServerManager;
import server.auth.User;
import server.data.TimeSeriesManager;

/**
 * Handler para uma conexão de cliente.
 * Processa pedidos e envia respostas usando o protocolo binário.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ServerManager serverManager;
    private final TimeSeriesManager tsManager;
    private final AggregationService aggregationService;
    private DataInputStream in;
    private DataOutputStream out;
    private User authenticatedUser;
    
    public ClientHandler(Socket socket, ServerManager serverManager, 
                        TimeSeriesManager tsManager, AggregationService aggregationService) {
        this.socket = socket;
        this.serverManager = serverManager;
        this.tsManager = tsManager;
        this.aggregationService = aggregationService;
        this.authenticatedUser = null;
    }
    
    @Override
    public void run() {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            
            System.out.println("Cliente conectado: " + socket.getInetAddress());
            
            // Loop de processamento
            while (!socket.isClosed()) {
                Protocol.Request request = Protocol.Request.readFrom(in);
                Protocol.Response response = processRequest(request);
                response.writeTo(out, request.getOperation());
            }
            
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.err.println("Erro na comunicação com cliente: " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }
    
    private Protocol.Response processRequest(Protocol.Request request) {
        try {
            switch (request.getOperation()) {
                case Protocol.OP_REGISTER:
                    return handleRegister(request);
                case Protocol.OP_LOGIN:
                    return handleLogin(request);
                case Protocol.OP_LOGOUT:
                    return handleLogout(request);
                case Protocol.OP_ADD_EVENT:
                    return handleAddEvent(request);
                case Protocol.OP_QUANTITY_SOLD:
                    return handleQuantitySold(request);
                case Protocol.OP_SALES_VOLUME:
                    return handleSalesVolume(request);
                case Protocol.OP_AVERAGE_PRICE:
                    return handleAveragePrice(request);
                default:
                    return Protocol.Response.error(request.getRequestId(), 
                        Protocol.STATUS_INVALID_PARAMS, "Operação desconhecida");
            }
        } catch (Exception e) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_ERROR, "Erro: " + e.getMessage());
        }
    }
    
    private Protocol.Response handleRegister(Protocol.Request request) {
        String username = request.getString("username");
        String password = request.getString("password");
        
        if (username == null || password == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_INVALID_PARAMS, "Username/password em falta");
        }
        
        boolean success = serverManager.register(username, password);
        
        if (success) {
            return Protocol.Response.success(request.getRequestId());
        } else {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_USER_EXISTS, "Username já existe");
        }
    }
    
    private Protocol.Response handleLogin(Protocol.Request request) {
        String username = request.getString("username");
        String password = request.getString("password");
        
        if (username == null || password == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_INVALID_PARAMS, "Username/password em falta");
        }
        
        User user = serverManager.authenticate(username, password);
        
        if (user != null) {
            authenticatedUser = user;
            return Protocol.Response.success(request.getRequestId());
        } else {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_AUTH_FAILED, "Credenciais inválidas");
        }
    }
    
    private Protocol.Response handleLogout(Protocol.Request request) {
        if (authenticatedUser == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_NOT_AUTHENTICATED, "Não autenticado");
        }
        
        authenticatedUser = null;
        return Protocol.Response.success(request.getRequestId());
    }
    
    private Protocol.Response handleAddEvent(Protocol.Request request) {
        if (authenticatedUser == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_NOT_AUTHENTICATED, "Não autenticado");
        }
        
        String product = request.getString("product");
        Integer quantity = request.getInt("quantity");
        Double price = request.getDouble("price");
        
        if (product == null || quantity == null || price == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_INVALID_PARAMS, "Parâmetros inválidos");
        }
        
        try {
            tsManager.addEvent(product, quantity, price);
            aggregationService.invalidateOnNewEvent(product);
            return Protocol.Response.success(request.getRequestId());
        } catch (Exception e) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_ERROR, "Erro ao adicionar evento");
        }
    }
    
    private Protocol.Response handleQuantitySold(Protocol.Request request) {
        if (authenticatedUser == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_NOT_AUTHENTICATED, "Não autenticado");
        }
        
        String product = request.getString("product");
        Integer days = request.getInt("days");
        
        if (product == null || days == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_INVALID_PARAMS, "Parâmetros inválidos");
        }
        
        int result = aggregationService.aggregateQuantity(product, days);
        
        if (result == -1) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_ERROR, "Dados insuficientes");
        }
        
        return Protocol.Response.success(request.getRequestId())
            .setData("quantity", result);
    }
    
    private Protocol.Response handleSalesVolume(Protocol.Request request) {
        if (authenticatedUser == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_NOT_AUTHENTICATED, "Não autenticado");
        }
        
        String product = request.getString("product");
        Integer days = request.getInt("days");
        
        if (product == null || days == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_INVALID_PARAMS, "Parâmetros inválidos");
        }
        
        double result = aggregationService.aggregateRevenue(product, days);
        
        if (result == -1) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_ERROR, "Dados insuficientes");
        }
        
        return Protocol.Response.success(request.getRequestId())
            .setData("revenue", result);
    }
    
    private Protocol.Response handleAveragePrice(Protocol.Request request) {
        if (authenticatedUser == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_NOT_AUTHENTICATED, "Não autenticado");
        }
        
        String product = request.getString("product");
        Integer days = request.getInt("days");
        
        if (product == null || days == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_INVALID_PARAMS, "Parâmetros inválidos");
        }
        
        double result = aggregationService.aggregateAveragePrice(product, days);
        
        if (result == -1) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_ERROR, "Dados insuficientes");
        }
        
        return Protocol.Response.success(request.getRequestId())
            .setData("avgPrice", result);
    }
    
    private void cleanup() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (!socket.isClosed()) socket.close();
            System.out.println("Cliente desconectado: " + socket.getInetAddress());
        } catch (IOException e) {
            System.err.println("Erro ao limpar recursos: " + e.getMessage());
        }
    }
}
