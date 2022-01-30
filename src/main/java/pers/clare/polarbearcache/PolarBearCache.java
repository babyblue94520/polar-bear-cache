package pers.clare.polarbearcache;

import org.springframework.cache.Cache;

public interface PolarBearCache extends Cache {

    Object getValue(String key);

    void onlyEvict(String key);

    void onlyClear();

    void expire(long now);
}
