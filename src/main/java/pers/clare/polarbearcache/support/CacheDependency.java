package pers.clare.polarbearcache.support;

import org.springframework.lang.NonNull;

import java.util.function.Function;

public class CacheDependency {
    private final String name;
    private final Boolean allEntries;
    private final Function<String, String> keyConverter;

    public CacheDependency(@NonNull String name, @NonNull Boolean allEntries, @NonNull Function<String, String> keyConverter) {
        this.name = name;
        this.allEntries = allEntries;
        this.keyConverter = keyConverter;
    }

    public String getName() {
        return name;
    }

    public Boolean getAllEntries() {
        return allEntries;
    }

    public Function<String, String> getKeyConverter() {
        return keyConverter;
    }
}
