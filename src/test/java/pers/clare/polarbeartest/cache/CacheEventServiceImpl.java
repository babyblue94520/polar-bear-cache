package pers.clare.polarbeartest.cache;

import pers.clare.polarbearcache.PolarBearCacheEventService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


public class CacheEventServiceImpl implements PolarBearCacheEventService {

    private static final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private static final ExecutorService executor = Executors.newFixedThreadPool(1);
    private boolean available = true;



    @Override
    public void send( String body) {
        executor.submit(() -> listeners.forEach(consumer -> consumer.accept(body)));
    }

    @Override
    public void addListener( Consumer<String> listener) {
        listeners.add(listener);
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available){
        this.available = available;
    }

}
