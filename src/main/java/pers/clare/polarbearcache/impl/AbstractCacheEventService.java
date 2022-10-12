package pers.clare.polarbearcache.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.clare.polarbearcache.PolarBearCacheEventService;


public abstract class AbstractCacheEventService implements PolarBearCacheEventService {
    private static final Logger log = LogManager.getLogger();

    private Runnable connectedHandler = () -> {
    };

    @Override
    public Runnable onConnected(Runnable runnable) {
        this.connectedHandler = runnable;
        return runnable;
    }
    @SuppressWarnings("unused")
    protected void publishConnectedEvent() {
        try {
            connectedHandler.run();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
