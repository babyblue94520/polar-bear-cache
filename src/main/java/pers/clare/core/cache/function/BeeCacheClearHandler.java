package pers.clare.core.cache.function;

@FunctionalInterface
public interface BeeCacheClearHandler<String, T> {
    T apply(String key, T oldData);
}
