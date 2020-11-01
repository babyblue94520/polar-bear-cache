package pers.clare.core.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cache.Cache;

@Getter
@AllArgsConstructor
public class BusyValueWrapper implements Cache.ValueWrapper {
    private final Object value;
    @Setter
    private long validTime;

    @Override
    public Object get() {
        return value;
    }
}
