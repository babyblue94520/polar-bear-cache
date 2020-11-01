package pers.clare.core.cache;

import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
public class BusyBeeCache extends BeeCache {

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final long effectiveTime;

    protected BusyBeeCache(BeeCacheManager manager, String name, long effectiveTime) {
        super(manager, name);
        // 最低時間 1 分鐘
        long delay = 60 * 1000;
        this.effectiveTime = effectiveTime > delay ? effectiveTime : delay;
        executor.scheduleWithFixedDelay(() -> {
            try {
                expire();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }, delay, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public ValueWrapper get(Object key) {
        BusyValueWrapper value = (BusyValueWrapper) super.get(key);
        if (value == null) return null;
        long now = System.currentTimeMillis();
        if (value.getValidTime() > now) {
            value.setValidTime(now + effectiveTime);
            return value;
        } else {
            store.remove(key);
            return null;
        }
    }

    @Override
    protected ValueWrapper createValueWrapper(Object value) {
        return new BusyValueWrapper(value, System.currentTimeMillis() + effectiveTime);
    }

    /**
     *
     */
    private void expire() {
        long oldSize = store.size();
        if (oldSize == 0) return;
        long now = System.currentTimeMillis();
        long t = System.currentTimeMillis();
        for (Map.Entry<Object, ValueWrapper> entry : store.entrySet()) {
            try {
                if (((BusyValueWrapper) entry.getValue()).getValidTime() < now) {
                    store.remove(entry.getKey());
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        log.debug("{} > {} expire {}ms", oldSize, store.size(), System.currentTimeMillis() - t);
    }

}
