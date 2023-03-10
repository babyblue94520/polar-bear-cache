package pers.clare.polarbearcache.impl;

import org.springframework.lang.NonNull;
import pers.clare.polarbearcache.PolarBearCacheDependencies;
import pers.clare.polarbearcache.support.CacheDependency;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class CacheDependenciesImpl implements PolarBearCacheDependencies {
    private final ConcurrentMap<String, ConcurrentMap<String, CacheDependency>> dependencies = new ConcurrentHashMap<>();

    public Collection<CacheDependency> find(String name) {
        ConcurrentMap<String, CacheDependency> map = dependencies.get(name);
        if (map == null) {
            return Collections.emptyList();
        }
        return map.values();
    }

    public String depend(
            @NonNull String name
            , String... dependNames
    ) {
        return depend(name, false, null, dependNames);
    }

    public String depend(
            @NonNull String name
            , Function<String, String> keyConverter
            , String... dependNames
    ) {
        return depend(name, false, keyConverter, dependNames);
    }

    public String depend(
            @NonNull String name
            , boolean allEntries
            , String... dependNames
    ) {
        return depend(name, allEntries, null, dependNames);
    }

    public String depend(
            @NonNull String name
            , boolean allEntries
            , Function<String, String> keyConverter
            , String... dependNames
    ) {
        if (dependNames == null || dependNames.length == 0) {
            return name;
        }
        if (keyConverter == null) {
            keyConverter = (key) -> key;
        }
        for (String depend : dependNames) {
            dependencies.computeIfAbsent(depend, k -> new ConcurrentHashMap<>())
                    .put(name, new CacheDependency(name, allEntries, keyConverter));
        }
        return name;
    }
}
