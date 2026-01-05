package server;

import geral.Protocol;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*
Gestor de séries temporais.
Gere o dia corrente e os D dias anteriores.
Thread-safe para operações concorrentes.
 */
public class TimeSeriesManager {
    private final int maxDays; // D - número máximo de dias históricos
    private final List<DayData> historicalDays; // Dias completos
    private DayData currentDay; // Dia corrente
    private int currentDayId; // ID do dia corrente
    private final ReentrantReadWriteLock lock;
    
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
    
    public TimeSeriesManager(int maxDays) {
        if (maxDays < 1) {
            throw new IllegalArgumentException("maxDays deve ser >= 1");
        }
        this.maxDays = maxDays;
        this.historicalDays = new ArrayList<>();
        this.currentDayId = 0;
        this.currentDay = new DayData(currentDayId);
        this.lock = new ReentrantReadWriteLock();
    }
    
    //Adiciona um evento ao dia corrente.
    public void addEvent(String product, int quantity, double price) {
        lock.readLock().lock();
        try {
            Protocol.Event event = new Protocol.Event(product, quantity, price);
            if (currentDay.completed) {
                throw new IllegalStateException("Dia já está completo");
            }
            currentDay.events.add(event);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addEvent(Protocol.Event event) {
        lock.readLock().lock();
        try {
            if (currentDay.completed) {
                throw new IllegalStateException("Dia já está completo");
            }
            currentDay.events.add(event);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    //Inicia um novo dia, movendo o dia corrente para histórico.
    public void newDay() {
        lock.writeLock().lock();
        try {
            // Completar o dia atual
            currentDay.completed = true;
            
            // Adicionar ao histórico
            historicalDays.add(0, currentDay);
            
            // Remover dias antigos se exceder o limite
            while (historicalDays.size() > maxDays) {
                historicalDays.remove(historicalDays.size() - 1);
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
    
    //Obtém eventos de um produto específico nos últimos N dias.
    public List<Protocol.Event> getEventsByProduct(String product, int days) {
        lock.readLock().lock();
        try {
            List<Protocol.Event> result = new ArrayList<>();
            int count = Math.min(days, historicalDays.size());
            
            for (int i = 0; i < count; i++) {
                for (Protocol.Event event : historicalDays.get(i).events) {
                    if (event.getProduct().equals(product)) {
                        result.add(event);
                    }
                }
            }
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    //Obtém todos os eventos dos últimos N dias.
    public List<List<Protocol.Event>> getAllEvents(int days) {
        lock.readLock().lock();
        try {
            List<List<Protocol.Event>> result = new ArrayList<>();
            int count = Math.min(days, historicalDays.size());
            
            for (int i = 0; i < count; i++) {
                result.add(new ArrayList<>(historicalDays.get(i).events));
            }
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    //Filtra eventos de produtos específicos num dia com offset.
    //dayOffset: 0 = dia corrente, 1 = ontem, 2 = anteontem, etc.
    public List<Protocol.Event> getFilteredEvents(List<String> products, int dayOffset) { //filtra eventos por produtos num dia especifico
        lock.readLock().lock();
        try {
            List<Protocol.Event> result = new ArrayList<>();
            
            if (dayOffset < 0) {
                return result; // Offset inválido
            }
            
            List<Protocol.Event> dayEvents;
            
            if (dayOffset == 0) {
                // Dia corrente
                dayEvents = currentDay.events;
            } else if (dayOffset <= historicalDays.size()) {
                // Dia histórico (offset-1 porque lista começa em 0)
                dayEvents = historicalDays.get(dayOffset - 1).events;
            } else {
                // Offset fora do range
                return result;
            }
            
            // Filtrar eventos pelos produtos especificados
            for (Protocol.Event event : dayEvents) {
                if (products.contains(event.getProduct())) {
                    result.add(event);
                }
            }
            
            return result;
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
    
    //Obtém o número máximo de dias históricos armazenados.
    public int getMaxDays() {
        return maxDays;
    }
    
    //Obtém o número de dias históricos armazenados.
    public int getHistoricalDayCount() {
        lock.readLock().lock();
        try {
            return historicalDays.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    //Limpa todos os dados de séries temporais.
    public void clear() {
        lock.writeLock().lock();
        try {
            historicalDays.clear();
            currentDayId = 0;
            currentDay = new DayData(currentDayId);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    //Verifica se um produto existe nos últimos N dias.
    public boolean hasProduct(String product, int days) {
        lock.readLock().lock();
        try {
            int count = Math.min(days, historicalDays.size());
            for (int i = 0; i < count; i++) {
                for (Protocol.Event event : historicalDays.get(i).events) {
                    if (event.getProduct().equals(product)) {
                        return true;
                    }
                }
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    //Obtém o tempo de início do dia corrente.
    public long getCurrentDayStartTime() {
        lock.readLock().lock();
        try {
            return currentDay.startTime;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    //Verifica se o dia corrente está completo.
    public boolean isCurrentDayCompleted() {
        lock.readLock().lock();
        try {
            return currentDay.completed;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    //Obtém o ID do dia histórico com base no offset.
    public int getHistoricalDayId(int offset) {
        lock.readLock().lock();
        try {
            if (offset < 1 || offset > historicalDays.size()) {
                return -1;
            }
            return historicalDays.get(offset - 1).dayId;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            return String.format("TimeSeriesManager[currentDay=%d, historical=%d/%d, currentEvents=%d]",
                currentDayId, historicalDays.size(), maxDays, currentDay.events.size());
        } finally {
            lock.readLock().unlock();
        }
    }
}
