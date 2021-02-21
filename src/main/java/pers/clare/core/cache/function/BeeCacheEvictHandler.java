package pers.clare.core.cache.function;

@FunctionalInterface
public interface BeeCacheEvictHandler<String, T> {
    T apply(String key, T oldData);
}
