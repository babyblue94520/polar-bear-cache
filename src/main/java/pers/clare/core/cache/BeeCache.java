package pers.clare.core.cache;

import org.springframework.cache.Cache;

public interface BeeCache extends Cache {
    public Object getValue(String key);
    public void onlyEvict(String key);
    public void onlyClear();
}
