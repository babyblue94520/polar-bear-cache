package pers.clare.polarbearcache.impl;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Defers mutations and notifications until commit when transaction synchronization
 * is active. Like Spring's transaction-aware cache, putIfAbsent and callable loads
 * remain immediate.
 */
public class TransactionCache extends BasicCache {

    protected TransactionCache(BasicCacheManager manager, String name,
                               long effectiveTime, boolean extension) {
        super(manager, name, effectiveTime, extension);
    }


    @Override
    public void put(Object key, Object value) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    TransactionCache.super.put(key, value);
                }
            });
        } else {
            super.put(key, value);
        }
    }

    @Override
    public void evict(Object key) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    TransactionCache.super.evict(key);
                }
            });
        } else {
            super.evict(key);
        }
    }

    @Override
    public void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    TransactionCache.super.clear();
                }
            });
        } else {
            super.clear();
        }
    }

    @Override
    public void putNotify(String key) {
        if (key == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    TransactionCache.super.putNotify(key);
                }
            });
        } else {
            super.putNotify(key);
        }
    }
}
