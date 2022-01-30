package pers.clare.polarbearcache.processor;

public class CachePutConfig {
    private final String[] cacheNames;
    private final String key;

    public CachePutConfig(String[] cacheNames, String key) {
        this.cacheNames = cacheNames;
        this.key = key;
    }

    public String[] getCacheNames() {
        return cacheNames;
    }

    public String getKey() {
        return key;
    }
}
