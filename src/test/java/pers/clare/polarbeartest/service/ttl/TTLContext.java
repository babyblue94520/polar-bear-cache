package pers.clare.polarbeartest.service.ttl;

import pers.clare.polarbeartest.cache.CacheType;
import pers.clare.polarbeartest.service.ServiceContext;
import org.springframework.stereotype.Service;

@Service
public class TTLContext extends ServiceContext<TTLUserService, TTLSimpleUserService> {

    @Override
    protected CacheType type() {
        return CacheType.TTL;
    }

}
