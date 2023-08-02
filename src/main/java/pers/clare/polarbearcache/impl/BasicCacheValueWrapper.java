package pers.clare.polarbearcache.impl;

import org.springframework.cache.Cache;


public class BasicCacheValueWrapper implements Cache.ValueWrapper {
    private final Object value;

    private long validTime;

    public BasicCacheValueWrapper(Object value, long validTime) {
        this.value = value;
        this.validTime = validTime;
    }

    @Override
    public Object get() {
        return value;
    }

    public long getValidTime() {
        return validTime;
    }

    void setValidTime(long validTime) {
        this.validTime = validTime;
    }

}
