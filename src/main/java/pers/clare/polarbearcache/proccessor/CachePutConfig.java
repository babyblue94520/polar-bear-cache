package pers.clare.polarbearcache.proccessor;

import org.springframework.cache.interceptor.CachePutOperation;

public class CachePutConfig {
    private final CachePutOperation operation;

    public CachePutConfig(CachePutOperation operation) {
        this.operation = operation;
    }

    public CachePutOperation getOperation() {
        return operation;
    }
}
