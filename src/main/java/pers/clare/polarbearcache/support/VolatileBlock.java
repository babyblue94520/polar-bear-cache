package pers.clare.polarbearcache.support;

import java.util.concurrent.atomic.AtomicInteger;

public class VolatileBlock {
    private AtomicInteger count = new AtomicInteger();
    private long validTime;
    private final long effectiveTime;

    public VolatileBlock(long effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public void block() {
        count.incrementAndGet();
        validTime = System.currentTimeMillis() + effectiveTime;
    }

    public boolean isBlock() {
        if (count.get() == 0) return false;
        if (System.currentTimeMillis() > validTime) {
            count.set(0);
            return false;
        }
        if (count.decrementAndGet() < 0) {
            count.incrementAndGet();
            return false;
        }
        return true;
    }
}
