package pers.clare.core.cache;

import lombok.extern.log4j.Log4j2;

import java.time.Duration;

@Log4j2
public class BusyBeeCacheManager extends BeeCacheManager {
    private final long effectiveTime;

    public BusyBeeCacheManager(
            BeeCacheMQService beeCacheMQService
            , String duration
    ) {
        super(beeCacheMQService);
        effectiveTime = Duration.parse(duration).toMillis();
    }

    @Override
    public BeeCache createCache(String name){
        return new BusyBeeCache(this, name, effectiveTime);
    }
}
