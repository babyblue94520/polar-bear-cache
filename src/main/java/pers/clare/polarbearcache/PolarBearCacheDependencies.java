package pers.clare.polarbearcache;

import pers.clare.polarbearcache.support.CacheDependency;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.function.Function;

public interface PolarBearCacheDependencies {

    String depend(
            @NonNull String name
            , String... dependNames
    );

    String depend(
            @NonNull String name
            , Function<String, String> keyConverter
            , String... dependNames
    );

    String depend(
            @NonNull String name
            , boolean allEntries
            , String... dependNames
    );

    String depend(
            @NonNull String name
            , boolean allEntries
            , Function<String, String> keyConverter
            , String... dependNames
    );

    Collection<CacheDependency> find(String name);
}
