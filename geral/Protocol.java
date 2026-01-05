package geral;

import java.io.*;
import java.util.*;

/*
Protocolo de comunicação partilhado entre cliente e servidor.
Define operações, requests, responses e eventos.
 */
public class Protocol {
    
    // ==================== CÓDIGOS DE OPERAÇÃO ====================
    
    public static final byte OP_REGISTER = 0x01;
    public static final byte OP_LOGIN = 0x02;
    public static final byte OP_LOGOUT = 0x03;
    public static final byte OP_ADD_EVENT = 0x04;
    public static final byte OP_QUANTITY_SOLD = 0x05;
    public static final byte OP_SALES_VOLUME = 0x06;
    public static final byte OP_AVERAGE_PRICE = 0x07;
    public static final byte OP_MAX_PRICE = 0x08;
    public static final byte OP_FILTER_EVENTS = 0x09;
    public static final byte OP_SIMULTANEOUS_SALES = 0x0A;
    public static final byte OP_CONSECUTIVE_SALES = 0x0B;
    public static final byte OP_NEW_DAY = 0x0C;
    
    // ==================== CÓDIGOS DE STATUS ====================
    
    public static final byte STATUS_OK = 0x00;
    public static final byte STATUS_ERROR = 0x01;
    public static final byte STATUS_AUTH_FAILED = 0x02;
    public static final byte STATUS_NOT_AUTHENTICATED = 0x03;
    public static final byte STATUS_USER_EXISTS = 0x04;
    public static final byte STATUS_INVALID_PARAMS = 0x05;
    
    // ==================== CLASSE REQUEST ====================
    
    public static class Request {
        private int requestId;
        private byte operation;
        private Map<String, Object> params;
        
        public Request(int requestId, byte operation) {
            this.requestId = requestId;
            this.operation = operation;
            this.params = new HashMap<>();
        }
        
        public int getRequestId() { return requestId; }
        public byte getOperation() { return operation; }
        
        public Request setParam(String key, Object value) {
            params.put(key, value);
            return this;
        }
        
        public String getString(String key) {
            return (String) params.get(key);
        }
        
        public Integer getInt(String key) {
            return (Integer) params.get(key);
        }
        
        public Double getDouble(String key) {
            return (Double) params.get(key);
        }
        
        public List<String> getStringList(String key) {
            return (List<String>) params.get(key);
        }
        
        // CLIENTE usa isto para enviar
        public void writeTo(DataOutputStream out) throws IOException {
            out.writeInt(requestId);
            out.writeByte(operation);
            
            switch (operation) {
                case OP_REGISTER:
                case OP_LOGIN:
                    Serializer.writeString(out, getString("username"));
                    Serializer.writeString(out, getString("password"));
                    break;
                    
                case OP_ADD_EVENT:
                    Serializer.writeString(out, getString("product"));
                    out.writeInt(getInt("quantity"));
                    out.writeDouble(getDouble("price"));
                    break;
                    
                case OP_QUANTITY_SOLD:
                case OP_SALES_VOLUME:
                case OP_AVERAGE_PRICE:
                case OP_MAX_PRICE:
                    Serializer.writeString(out, getString("product"));
                    out.writeInt(getInt("days"));
                    break;
                    
                case OP_FILTER_EVENTS:
                    Serializer.writeStringList(out, getStringList("products"));
                    out.writeInt(getInt("dayOffset"));
                    break;
                    
                case OP_SIMULTANEOUS_SALES:
                    Serializer.writeString(out, getString("product1"));
                    Serializer.writeString(out, getString("product2"));
                    break;
                    
                case OP_CONSECUTIVE_SALES:
                    out.writeInt(getInt("n"));
                    break;
                    
                case OP_LOGOUT:
                case OP_NEW_DAY:
                    // Sem parâmetros
                    break;
            }
        }
        
        // SERVIDOR usa isto para receber
        public static Request readFrom(DataInputStream in) throws IOException {
            int requestId = in.readInt();
            byte operation = in.readByte();
            Request req = new Request(requestId, operation);
            
            switch (operation) {
                case OP_REGISTER:
                case OP_LOGIN:
                    req.setParam("username", Serializer.readString(in));
                    req.setParam("password", Serializer.readString(in));
                    break;
                    
                case OP_ADD_EVENT:
                    req.setParam("product", Serializer.readString(in));
                    req.setParam("quantity", in.readInt());
                    req.setParam("price", in.readDouble());
                    break;
                    
                case OP_QUANTITY_SOLD:
                case OP_SALES_VOLUME:
                case OP_AVERAGE_PRICE:
                case OP_MAX_PRICE:
                    req.setParam("product", Serializer.readString(in));
                    req.setParam("days", in.readInt());
                    break;
                    
                case OP_FILTER_EVENTS:
                    req.setParam("products", Serializer.readStringList(in));
                    req.setParam("dayOffset", in.readInt());
                    break;
                    
                case OP_SIMULTANEOUS_SALES:
                    req.setParam("product1", Serializer.readString(in));
                    req.setParam("product2", Serializer.readString(in));
                    break;
                    
                case OP_CONSECUTIVE_SALES:
                    req.setParam("n", in.readInt());
                    break;
                    
                case OP_LOGOUT:
                case OP_NEW_DAY:
                    // Sem parâmetros
                    break;
            }
            
            return req;
        }
    }
    
    // ==================== CLASSE RESPONSE ====================
    
    public static class Response {
        private int requestId;
        private byte status;
        private String errorMessage;
        private Map<String, Object> data;
        
        public Response(int requestId, byte status) {
            this.requestId = requestId;
            this.status = status;
            this.data = new HashMap<>();
        }
        
        public static Response success(int requestId) {
            return new Response(requestId, STATUS_OK);
        }
        
        public static Response error(int requestId, byte status, String message) {
            Response res = new Response(requestId, status);
            res.errorMessage = message;
            return res;
        }
        
        public int getRequestId() { return requestId; }
        public byte getStatus() { return status; }
        public boolean isSuccess() { return status == STATUS_OK; }
        public String getErrorMessage() { return errorMessage; }
        
        public Response setData(String key, Object value) {
            data.put(key, value);
            return this;
        }
        
        public Integer getInt(String key) {
            return (Integer) data.get(key);
        }
        
        public Double getDouble(String key) {
            return (Double) data.get(key);
        }
        
        public Boolean getBoolean(String key) {
            return (Boolean) data.get(key);
        }
        
        public String getString(String key) {
            return (String) data.get(key);
        }
        
        public List<Event> getEventList(String key) {
            return (List<Event>) data.get(key);
        }
        
        // SERVIDOR usa isto para enviar
        public void writeTo(DataOutputStream out, byte operation) throws IOException {
            out.writeInt(requestId);
            out.writeByte(status);
            
            if (status != STATUS_OK) {
                Serializer.writeString(out, errorMessage);
                return;
            }
            
            switch (operation) {
                case OP_REGISTER:
                case OP_LOGIN:
                case OP_LOGOUT:
                case OP_ADD_EVENT:
                case OP_NEW_DAY:
                    // Sem dados adicionais
                    break;
                    
                case OP_QUANTITY_SOLD:
                    out.writeInt(getInt("quantity"));
                    break;
                    
                case OP_SALES_VOLUME:
                    out.writeDouble(getDouble("revenue"));
                    break;
                    
                case OP_AVERAGE_PRICE:
                    out.writeDouble(getDouble("avgPrice"));
                    break;
                    
                case OP_MAX_PRICE:
                    out.writeDouble(getDouble("maxPrice"));
                    break;
                    
                case OP_SIMULTANEOUS_SALES:
                    Serializer.writeBoolean(out, getBoolean("result"));
                    break;
                    
                case OP_CONSECUTIVE_SALES:
                    Serializer.writeString(out, getString("product"));
                    break;
                    
                case OP_FILTER_EVENTS:
                    writeEventList(out, getEventList("events"));
                    break;
            }
        }
        
        // CLIENTE usa isto para receber
        public static Response readFrom(DataInputStream in, byte operation) throws IOException {
            int requestId = in.readInt();
            byte status = in.readByte();
            
            Response res = new Response(requestId, status);
            
            if (status != STATUS_OK) {
                res.errorMessage = Serializer.readString(in);
                return res;
            }
            
            switch (operation) {
                case OP_REGISTER:
                case OP_LOGIN:
                case OP_LOGOUT:
                case OP_ADD_EVENT:
                case OP_NEW_DAY:
                    // Sem dados adicionais
                    break;
                    
                case OP_QUANTITY_SOLD:
                    res.data.put("quantity", in.readInt());
                    break;
                    
                case OP_SALES_VOLUME:
                    res.data.put("revenue", in.readDouble());
                    break;
                    
                case OP_AVERAGE_PRICE:
                    res.data.put("avgPrice", in.readDouble());
                    break;
                    
                case OP_MAX_PRICE:
                    res.data.put("maxPrice", in.readDouble());
                    break;
                    
                case OP_SIMULTANEOUS_SALES:
                    res.data.put("result", Serializer.readBoolean(in));
                    break;
                    
                case OP_CONSECUTIVE_SALES:
                    res.data.put("product", Serializer.readString(in));
                    break;
                    
                case OP_FILTER_EVENTS:
                    res.data.put("events", readEventList(in));
                    break;
            }
            
            return res;
        }
        
        // Serialização eficiente de eventos com dicionário
        private static void writeEventList(DataOutputStream out, List<Event> events) throws IOException {
            if (events == null) {
                out.writeInt(-1);
                return;
            }
            
            Map<String, Short> productDict = new HashMap<>();
            List<String> uniqueProducts = new ArrayList<>();
            
            for (Event event : events) {
                if (!productDict.containsKey(event.getProduct())) {
                    productDict.put(event.getProduct(), (short) uniqueProducts.size());
                    uniqueProducts.add(event.getProduct());
                }
            }
            
            out.writeInt(uniqueProducts.size());
            for (String product : uniqueProducts) {
                Serializer.writeString(out, product);
            }
            
            out.writeInt(events.size());
            for (Event event : events) {
                out.writeShort(productDict.get(event.getProduct()));
                out.writeInt(event.getQuantity());
                out.writeDouble(event.getPrice());
                out.writeLong(event.getTimestamp());
            }
        }
        
        private static List<Event> readEventList(DataInputStream in) throws IOException {
            int dictSize = in.readInt();
            if (dictSize == -1) return null;
            
            String[] productDict = new String[dictSize];
            for (int i = 0; i < dictSize; i++) {
                productDict[i] = Serializer.readString(in);
            }
            
            int count = in.readInt();
            List<Event> events = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                short productIndex = in.readShort();
                String product = productDict[productIndex];
                int quantity = in.readInt();
                double price = in.readDouble();
                long timestamp = in.readLong();
                events.add(new Event(product, quantity, price, timestamp));
            }
            
            return events;
        }
    }
    
    // ==================== CLASSE EVENT ====================
    
    public static class Event {
        private final String product;
        private final int quantity;
        private final double price;
        private final long timestamp;
        
        public Event(String product, int quantity, double price) {
            this(product, quantity, price, System.currentTimeMillis());
        }
        
        public Event(String product, int quantity, double price, long timestamp) {
            this.product = product;
            this.quantity = quantity;
            this.price = price;
            this.timestamp = timestamp;
        }
        
        public String getProduct() { return product; }
        public int getQuantity() { return quantity; }
        public double getPrice() { return price; }
        public long getTimestamp() { return timestamp; }
        public double getTotalValue() { return quantity * price; }
        
        @Override
        public String toString() {
            return String.format("Event[%s: %dx%.2f=%.2f @ %d]",
                product, quantity, price, getTotalValue(), timestamp);
        }
    }
    
    // ==================== UTILITÁRIOS ====================
    
    public static String getOperationName(byte operation) {
        switch (operation) {
            case OP_REGISTER: return "REGISTER";
            case OP_LOGIN: return "LOGIN";
            case OP_LOGOUT: return "LOGOUT";
            case OP_ADD_EVENT: return "ADD_EVENT";
            case OP_QUANTITY_SOLD: return "QUANTITY_SOLD";
            case OP_SALES_VOLUME: return "SALES_VOLUME";
            case OP_AVERAGE_PRICE: return "AVERAGE_PRICE";
            case OP_MAX_PRICE: return "MAX_PRICE";
            case OP_FILTER_EVENTS: return "FILTER_EVENTS";
            case OP_SIMULTANEOUS_SALES: return "SIMULTANEOUS_SALES";
            case OP_CONSECUTIVE_SALES: return "CONSECUTIVE_SALES";
            case OP_NEW_DAY: return "NEW_DAY";
            default: return "UNKNOWN";
        }
    }
    
    public static String getStatusName(byte status) {
        switch (status) {
            case STATUS_OK: return "OK";
            case STATUS_ERROR: return "ERROR";
            case STATUS_AUTH_FAILED: return "AUTH_FAILED";
            case STATUS_NOT_AUTHENTICATED: return "NOT_AUTHENTICATED";
            case STATUS_USER_EXISTS: return "USER_EXISTS";
            case STATUS_INVALID_PARAMS: return "INVALID_PARAMS";
            default: return "UNKNOWN";
        }
    }
}
