package pers.clare.polarbeartest.service.basic;

import org.springframework.stereotype.Service;
import pers.clare.polarbeartest.cache.CacheType;
import pers.clare.polarbeartest.service.ServiceContext;

@Service
public class BasicContext extends ServiceContext<BasicUserService, BasicSimpleUserService> {

    @Override
    protected CacheType type() {
        return CacheType.Basic;
    }
}
