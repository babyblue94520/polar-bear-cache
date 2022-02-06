package pers.clare.polarbeartest.service.ttl;

import org.springframework.stereotype.Service;
import pers.clare.polarbeartest.cache.CacheType;
import pers.clare.polarbeartest.service.ServiceContext;

@Service
public class TTLContext extends ServiceContext<TTLUserService, TTLSimpleUserService> {

    @Override
    protected CacheType type() {
        return CacheType.TTL;
    }

}
