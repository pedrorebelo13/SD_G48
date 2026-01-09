package server;

import geral.Protocol;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import server.persistence.TimeSeriesPersistence;

/*
Gestor de séries temporais.
Gere o dia corrente e os D dias anteriores.
 */

public class TimeSeriesManager {
    private final int maxDays; // D - número máximo de dias históricos (disco)
    private final int maxMemoryDays; // S - número máximo de dias em memória
    private final TimeSeriesPersistence persistence;
    
    private final List<DayData> historicalDays; // Dias completos em memória
    private DayData currentDay; // Dia corrente
    private int currentDayId; // ID do dia corrente
    private final ReentrantReadWriteLock lock;
    private final Condition newEventCondition;
    private AggregationService aggregationService; // Referência para invalidar cache
    
    // Classe interna para representar os dados de um dia
    private static class DayData {
        final int dayId;
        final List<Protocol.Event> events;
        final long startTime;
        boolean completed;
        
        DayData(int dayId) {
            this.dayId = dayId;
            this.events = new ArrayList<>();
            this.startTime = System.currentTimeMillis();
            this.completed = false;
        }
    }
    
    public TimeSeriesManager(int maxDays, int maxMemoryDays, TimeSeriesPersistence persistence) {
        if (maxDays < 1) throw new IllegalArgumentException("maxDays >= 1");
        if (maxMemoryDays > maxDays) throw new IllegalArgumentException("maxMemoryDays <= maxDays");
        
        this.maxDays = maxDays;
        this.maxMemoryDays = maxMemoryDays;
        this.persistence = persistence;
        
        this.historicalDays = new ArrayList<>();
        this.currentDayId = 0;
        this.currentDay = new DayData(currentDayId);
        this.lock = new ReentrantReadWriteLock();
        this.newEventCondition = lock.writeLock().newCondition();
    }
    
    public void setAggregationService(AggregationService as) {
        this.aggregationService = as;
    }
    
    //Adiciona um evento ao dia corrente.
    public void addEvent(String product, int quantity, double price) {
        lock.writeLock().lock();
        try {
            Protocol.Event event = new Protocol.Event(product, quantity, price);
            if (currentDay.completed) {
                throw new IllegalStateException("Dia já está completo");
            }
            currentDay.events.add(event);
            newEventCondition.signalAll();
        } finally {
            lock.writeLock().unlock();
        }
    }

    //persistencia
    public void addEvent(Protocol.Event event) {
        lock.writeLock().lock();
        try {
            if (currentDay.completed) {
                throw new IllegalStateException("Dia já está completo");
            }
            currentDay.events.add(event);
            newEventCondition.signalAll();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    //Inicia um novo dia, movendo o dia corrente para histórico.
    public void newDay() {
        lock.writeLock().lock();
        try {
            // Completar o dia atual
            currentDay.completed = true;
            newEventCondition.signalAll();
            
            // Persistir dia atual
            try {
                persistence.saveDay(currentDayId, currentDay.events);
                persistence.saveState(this); 
            } catch (IOException e) {
                System.err.println("Erro ao persistir dia " + currentDayId + ": " + e.getMessage());
            }
            
            // Adicionar ao histórico
            historicalDays.add(0, currentDay);
            
            // Remover dias antigos da memória se exceder o limite S
            while (historicalDays.size() > maxMemoryDays) {
                historicalDays.remove(historicalDays.size() - 1);
            }
            
            // Remover dias antigos do disco se exceder o limite D
            int dayToDelete = currentDayId - maxDays;
            if (dayToDelete >= 0) {
                persistence.deleteDay(dayToDelete);
            }
            
             // Invalida cache de agregação (se existir)
             if (aggregationService != null) {
                aggregationService.invalidateCache();
            }
            
            // Criar novo dia
            currentDayId++;
            currentDay = new DayData(currentDayId);
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    //Obtém eventos do dia corrente.
    public List<Protocol.Event> getCurrentDayEvents() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(currentDay.events);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    //Obtém o número de eventos no dia corrente.
    public int getCurrentDayEventCount() {
        lock.readLock().lock();
        try {
            return currentDay.events.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    
    //Obtém o ID do dia corrente.
    public int getCurrentDayId() {
        lock.readLock().lock();
        try {
            return currentDayId;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setCurrentDayId(int id) {
        lock.writeLock().lock();
        try {
            this.currentDayId = id;
            this.currentDay = new DayData(id);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    //Obtém o número máximo de dias históricos armazenados.
    public int getMaxDays() {
        return maxDays;
    }
    
    //Obtém o número de dias históricos armazenados.
    public int getHistoricalDayCount() {
        lock.readLock().lock();
        try {
            return Math.min(currentDayId, maxDays);
        } finally {
            lock.readLock().unlock();
        }
    }

    // Obtém os eventos de UM dia histórico específico.
    public List<Protocol.Event> getHistoricalDayEvents(int daysAgo) {
        lock.readLock().lock();
        try {
            int availableHistory = Math.min(currentDayId, maxDays);
            
            if (daysAgo < 0 || daysAgo >= availableHistory) {
                return new ArrayList<>(); 
            }

            if (daysAgo < historicalDays.size()) {
                // Em memória
                return new ArrayList<>(historicalDays.get(daysAgo).events);
            } else {
                 // Em disco
                 int targetId = currentDayId - 1 - daysAgo;
                 try {
                     return persistence.loadDay(targetId);
                 } catch (IOException e) {
                     System.err.println("Erro ao carregar dia " + targetId + ": " + e.getMessage());
                     return new ArrayList<>();
                 }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    //Obtém eventos filtrados por produtos e de dia.
    public List<Protocol.Event> getFilteredEvents(List<String> products, Integer dayOffset) {
        lock.readLock().lock();
        try {
            List<Protocol.Event> sourceEvents = null;
            
            // dayOffset null = dia corrente
            if (dayOffset == null || dayOffset == 0) {
                sourceEvents = currentDay.events;
            } else {
                // Dia histórico (Reutilizando a lógica do getHistoricalDayEvents mas otimizada se quisermos)
                sourceEvents = getHistoricalDayEvents(dayOffset - 1);
            }

            List<Protocol.Event> result = new ArrayList<>();
            if (sourceEvents != null) {
                for (Protocol.Event event : sourceEvents) {
                    if (products == null || products.isEmpty() || products.contains(event.getProduct())) {
                        result.add(event);
                    }
                }
            }
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    //Aguarda até que ambos os produtos sejam vendidos simultaneamente no dia corrente.
    //Retorna true se a condição foi satisfeita, false se o dia terminou antes.
    public boolean waitForSimultaneousSales(String product1, String product2) {
        lock.writeLock().lock();
        try {
            while (true) {
                // Verificar se o dia acabou
                if (currentDay.completed) {
                    return false;
                }
                
                // Verificar se ambos os produtos foram vendidos
                boolean hasProduct1 = false;
                boolean hasProduct2 = false;
                
                for (Protocol.Event event : currentDay.events) {
                    if (event.getProduct().equals(product1)) {
                        hasProduct1 = true;
                    }
                    if (event.getProduct().equals(product2)) {
                        hasProduct2 = true;
                    }
                    if (hasProduct1 && hasProduct2) {
                        return true;
                    }
                }
                
                try {
                    newEventCondition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    //Aguarda até que N vendas consecutivas ocorram no dia corrente.
    //Retorna o produto com N vendas consecutivas, ou null se o dia terminou.
    public String waitForConsecutiveSales(Integer n) {
        lock.writeLock().lock();
        try {
            while (true) {
                // Verificar se o dia acabou
                if (currentDay.completed) {
                    return null;
                }
                
                // Verificar vendas consecutivas
                if (currentDay.events.size() >= n) {
                    // Verificar os últimos N eventos
                    String product = null;
                    boolean allSame = true;
                    
                    for (int i = currentDay.events.size() - n; i < currentDay.events.size(); i++) {
                        String eventProduct = currentDay.events.get(i).getProduct();
                        if (product == null) {
                            product = eventProduct;
                        } else if (!product.equals(eventProduct)) {
                            allSame = false;
                            break;
                        }
                    }
                    
                    if (allSame && product != null) {
                        return product;
                    }
                }
                
                try {
                    newEventCondition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}

