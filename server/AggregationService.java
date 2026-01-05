package server;

import geral.Protocol.Event;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import server.cache.ProductCache;
import server.cache.ProductCache.CachedAggregation;

/*
Serviço de agregações lazy com caching.
Calcula agregações sob demanda e guarda resultados no cache.
Thread-safe para acesso concorrente.
 */
public class AggregationService {
    private final TimeSeriesManager tsManager;
    private final ProductCache cache;
    
    public AggregationService(TimeSeriesManager tsManager, ProductCache cache) {
        this.tsManager = tsManager;
        this.cache = cache;
    }
    
    //Agrega quantidade total vendida de um produto nos últimos N dias.
    public int aggregateQuantity(String product, int days) {
        if (days < 1 || days > tsManager.getMaxDays()) {
            return -1;
        }
        
        String cacheKey = String.format("qty:%s:%d", product, days);
        int currentDayId = tsManager.getCurrentDayId();
        
        // Verificar cache
        CachedAggregation cached = cache.get(cacheKey);
        if (cached != null && cached.isValid(currentDayId)) {
            return (Integer) cached.getValue();
        }
        
        // Calcular agregação
        List<List<Event>> allDaysEvents = tsManager.getAllEvents(days);
        if (allDaysEvents.size() < days) {
            return -1; // Não há dados suficientes
        }
        
        int total = 0;
        for (List<Event> dayEvents : allDaysEvents) {
            for (Event event : dayEvents) {
                if (event.getProduct().equals(product)) {
                    total += event.getQuantity();
                }
            }
        }
        
        // Guardar no cache
        cache.put(cacheKey, new CachedAggregation(total, currentDayId));
        
        return total;
    }
    
    //Agrega receita total de um produto nos últimos N dias.
    public double aggregateRevenue(String product, int days) {
        if (days < 1 || days > tsManager.getMaxDays()) {
            return -1;
        }
        
        String cacheKey = String.format("rev:%s:%d", product, days);
        int currentDayId = tsManager.getCurrentDayId();
        
        // Verificar cache
        CachedAggregation cached = cache.get(cacheKey);
        if (cached != null && cached.isValid(currentDayId)) {
            return (Double) cached.getValue();
        }
        
        // Calcular agregação
        List<List<Event>> allDaysEvents = tsManager.getAllEvents(days);
        if (allDaysEvents.size() < days) {
            return -1;
        }
        
        double total = 0;
        for (List<Event> dayEvents : allDaysEvents) {
            for (Event event : dayEvents) {
                if (event.getProduct().equals(product)) {
                    total += event.getQuantity() * event.getPrice();
                }
            }
        }
        
        // Guardar no cache
        cache.put(cacheKey, new CachedAggregation(total, currentDayId));
        
        return total;
    }
    
    //Agrega preço médio de um produto nos últimos N dias.
    public double aggregateAveragePrice(String product, int days) {
        if (days < 1 || days > tsManager.getMaxDays()) {
            return -1;
        }
        
        String cacheKey = String.format("avg:%s:%d", product, days);
        int currentDayId = tsManager.getCurrentDayId();
        
        // Verificar cache
        CachedAggregation cached = cache.get(cacheKey);
        if (cached != null && cached.isValid(currentDayId)) {
            return (Double) cached.getValue();
        }
        
        // Calcular agregação
        List<List<Event>> allDaysEvents = tsManager.getAllEvents(days);
        if (allDaysEvents.size() < days) {
            return -1;
        }
        
        double totalRevenue = 0;
        int totalQuantity = 0;
        
        for (List<Event> dayEvents : allDaysEvents) {
            for (Event event : dayEvents) {
                if (event.getProduct().equals(product)) {
                    totalRevenue += event.getQuantity() * event.getPrice();
                    totalQuantity += event.getQuantity();
                }
            }
        }
        
        if (totalQuantity == 0) {
            return 0;
        }
        
        double avgPrice = totalRevenue / totalQuantity;
        
        // Guardar no cache
        cache.put(cacheKey, new CachedAggregation(avgPrice, currentDayId));
        
        return avgPrice;
    }

    //Agrega preço máximo de um produto nos últimos N dias.
    public double aggregateMaxPrice(String product, int days) {
        if (days < 1 || days > tsManager.getMaxDays()) {
            return -1;
        }
        
        String cacheKey = String.format("max:%s:%d", product, days);
        int currentDayId = tsManager.getCurrentDayId();
        
        // Verificar cache
        CachedAggregation cached = cache.get(cacheKey);
        if (cached != null && cached.isValid(currentDayId)) {
            return (Double) cached.getValue();
        }
        
        // Calcular agregação
        List<List<Event>> allDaysEvents = tsManager.getAllEvents(days);
        if (allDaysEvents.size() < days) {
            return -1;
        }
        
        double maxPrice = Double.NEGATIVE_INFINITY;
        boolean foundProduct = false;
        
        for (List<Event> dayEvents : allDaysEvents) {
            for (Event event : dayEvents) {
                if (event.getProduct().equals(product)) {
                    maxPrice = Math.max(maxPrice, event.getPrice());
                    foundProduct = true;
                }
            }
        }
        
        if (!foundProduct) {
            return 0;
        }
        
        // Guardar no cache
        cache.put(cacheKey, new CachedAggregation(maxPrice, currentDayId));
        
        return maxPrice;
    }
    
    //Conta dias em que ambos os produtos foram vendidos (interseção).
    public int countCommonDays(String product1, String product2, int days) {
        if (days < 1 || days > tsManager.getMaxDays()) {
            return -1;
        }
        
        String cacheKey = String.format("common:%s:%s:%d", product1, product2, days);
        int currentDayId = tsManager.getCurrentDayId();
        
        // Verificar cache
        CachedAggregation cached = cache.get(cacheKey);
        if (cached != null && cached.isValid(currentDayId)) {
            return (Integer) cached.getValue();
        }
        
        // Calcular agregação
        List<List<Event>> allDaysEvents = tsManager.getAllEvents(days);
        if (allDaysEvents.size() < days) {
            return -1;
        }
        
        int commonDays = 0;
        for (List<Event> dayEvents : allDaysEvents) {
            boolean hasProduct1 = false;
            boolean hasProduct2 = false;
            
            for (Event event : dayEvents) {
                if (event.getProduct().equals(product1)) hasProduct1 = true;
                if (event.getProduct().equals(product2)) hasProduct2 = true;
            }
            
            if (hasProduct1 && hasProduct2) {
                commonDays++;
            }
        }
        
        // Guardar no cache
        cache.put(cacheKey, new CachedAggregation(commonDays, currentDayId));
        
        return commonDays;
    }
    
    //Encontra a maior sequência consecutiva de vendas de um produto
    public int findMaxConsecutive(String product, int days) {
        if (days < 1 || days > tsManager.getMaxDays()) {
            return -1;
        }
        
        String cacheKey = String.format("maxseq:%s:%d", product, days);
        int currentDayId = tsManager.getCurrentDayId();
        
        // Verificar cache
        CachedAggregation cached = cache.get(cacheKey);
        if (cached != null && cached.isValid(currentDayId)) {
            return (Integer) cached.getValue();
        }
        
        // Calcular agregação
        List<List<Event>> allDaysEvents = tsManager.getAllEvents(days);
        if (allDaysEvents.size() < days) {
            return -1;
        }
        
        int maxConsecutive = 0;
        for (List<Event> dayEvents : allDaysEvents) {
            int currentConsecutive = 0;
            for (Event event : dayEvents) {
                if (event.getProduct().equals(product)) {
                    currentConsecutive++;
                    maxConsecutive = Math.max(maxConsecutive, currentConsecutive);
                } else {
                    currentConsecutive = 0;
                }
            }
        }
        
        // Guardar no cache
        cache.put(cacheKey, new CachedAggregation(maxConsecutive, currentDayId));
        
        return maxConsecutive;
    }
    
    //Agrega múltiplos produtos nos últimos N dias.
    public Map<String, ProductAggregation> aggregateMultipleProducts(List<String> products, int days) {
        if (days < 1 || days > tsManager.getMaxDays()) {
            return null;
        }
        
        List<List<Event>> allDaysEvents = tsManager.getAllEvents(days);
        if (allDaysEvents.size() < days) {
            return null;
        }
        
        Map<String, ProductAggregation> result = new HashMap<>();
        
        for (String product : products) {
            int quantity = 0;
            double revenue = 0;
            int eventCount = 0;
            
            for (List<Event> dayEvents : allDaysEvents) {
                for (Event event : dayEvents) {
                    if (event.getProduct().equals(product)) {
                        quantity += event.getQuantity();
                        revenue += event.getQuantity() * event.getPrice();
                        eventCount++;
                    }
                }
            }
            
            double avgPrice = eventCount > 0 ? revenue / quantity : 0;
            result.put(product, new ProductAggregation(quantity, revenue, avgPrice, eventCount));
        }
        
        return result;
    }
    
    //Invalida cache quando um novo evento é adicionado.
    public void invalidateOnNewEvent(String product) {
        cache.invalidateProduct(product);
    }
    
    //Invalida cache quando um novo dia começa.
    public void invalidateOnNewDay() {
        cache.clear(); // Novo dia invalida todas as agregações
    }
    
    //Classe auxiliar para agregações de produtos.
    public static class ProductAggregation {
        private final int quantity;
        private final double revenue;
        private final double avgPrice;
        private final int eventCount;
        
        public ProductAggregation(int quantity, double revenue, double avgPrice, int eventCount) {
            this.quantity = quantity;
            this.revenue = revenue;
            this.avgPrice = avgPrice;
            this.eventCount = eventCount;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public double getRevenue() {
            return revenue;
        }
        
        public double getAveragePrice() {
            return avgPrice;
        }
        
        public int getEventCount() {
            return eventCount;
        }
        
        @Override
        public String toString() {
            return String.format("ProductAggregation[qty=%d, rev=%.2f, avg=%.2f, events=%d]",
                quantity, revenue, avgPrice, eventCount);
        }
    }
}
