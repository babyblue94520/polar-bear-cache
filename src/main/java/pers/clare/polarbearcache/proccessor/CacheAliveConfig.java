package pers.clare.polarbearcache.proccessor;

public class CacheAliveConfig {
    private final long effectiveTime;
    private final boolean extension;

    public CacheAliveConfig(long effectiveTime, boolean extension) {
        this.effectiveTime = effectiveTime;
        this.extension = extension;
    }

    public long getEffectiveTime() {
        return effectiveTime;
    }

    public boolean isExtension() {
        return extension;
    }
}
