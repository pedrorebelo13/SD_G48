package client;

import geral.Protocol;
import java.io.IOException;

public class Client {
    private final Connection connection;
    private boolean authenticated;
    private String currentUser;
    private String lastErrorMessage;
    
    public Client(String host, int port) {
        this.connection = new Connection(host, port);
        this.authenticated = false;
        this.lastErrorMessage = null;
    }
    
    public void connect() throws IOException {
        connection.connect();
    }
    
    public boolean isConnected() {
        return connection.isConnected();
    }
    
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    public String getCurrentUser() {
        return currentUser;
    }
    
    public boolean register(String username, String password) throws IOException {
        Protocol.Response response = connection.register(username, password);
        return response.isSuccess();
    }
    
    public boolean login(String username, String password) throws IOException {
        Protocol.Response response = connection.login(username, password);
        
        if (response.isSuccess()) {
            authenticated = true;
            currentUser = username;
            return true;
        }
        
        return false;
    }
    
    public boolean logout() throws IOException {
        Protocol.Response response = connection.logout();
        
        if (response.isSuccess()) {
            authenticated = false;
            currentUser = null;
            return true;
        }
        
        return false;
    }
    
    public boolean addEvent(String product, int quantity, double price) throws IOException {
        ensureAuthenticated();
        Protocol.Response response = connection.addEvent(product, quantity, price);
        return response.isSuccess();
    }
    
    public int aggregateQuantity(String product, int days) throws IOException {
        ensureAuthenticated();
        Protocol.Response response = connection.aggregateQuantity(product, days);
        
        if (response.isSuccess()) {
            Integer result = response.getInt("quantity");
            return result != null ? result : -1;
        }
        
        return -1;
    }
    
    public double aggregateVolume(String product, int days) throws IOException {
        ensureAuthenticated();
        Protocol.Response response = connection.aggregateVolume(product, days);
        if (response.isSuccess()) {
            lastErrorMessage = null;
            Double result = response.getDouble("volume");
            return result != null ? result : -1;
        } else {
            lastErrorMessage = response.getErrorMessage();
        }
        return -1;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }
    
    public double aggregateAverage(String product, int days) throws IOException {
        ensureAuthenticated();
        Protocol.Response response = connection.aggregateAverage(product, days);
        
        if (response.isSuccess()) {
            Double result = response.getDouble("avgPrice");
            return result != null ? result : -1;
        }
        
        return -1;
    }
    
    public double aggregateMaxPrice(String product, int days) throws IOException {
        ensureAuthenticated();
        Protocol.Response response = connection.aggregateMaxPrice(product, days);
        
        if (response.isSuccess()) {
            Double result = response.getDouble("maxPrice");
            return result != null ? result : -1;
        }
        
        return -1;
    }
    
    public java.util.List<Protocol.Event> filterEvents(java.util.List<String> products, int dayOffset) throws IOException {
        ensureAuthenticated();
        Protocol.Response response = connection.filterEvents(products, dayOffset);
        
        if (response.isSuccess()) {
            return response.getEventList("events");
        }
        
        return new java.util.ArrayList<>();
    }
    
    public Boolean simultaneousSales(String product1, String product2) throws IOException {
        ensureAuthenticated();
        Protocol.Response response = connection.simultaneousSales(product1, product2);
        if (response.isSuccess()) {
            lastErrorMessage = null;
            return response.getBoolean("result");
        } else {
            lastErrorMessage = response.getErrorMessage();
            return null;
        }
    }
    
    public String consecutiveSales(int n) throws IOException {
        ensureAuthenticated();
        Protocol.Response response = connection.consecutiveSales(n);
        if (response.isSuccess()) {
            lastErrorMessage = null;
            return response.getString("product");
        } else {
            lastErrorMessage = response.getErrorMessage();
            return null;
        }
    }
    
    public void close() {
        connection.close();
        authenticated = false;
        currentUser = null;
    }
    
    private void ensureAuthenticated() {
        if (!authenticated) {
            throw new IllegalStateException("Não autenticado. Faça login primeiro.");
        }
    }
}
