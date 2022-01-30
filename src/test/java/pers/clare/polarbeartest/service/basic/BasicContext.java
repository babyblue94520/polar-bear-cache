package pers.clare.polarbeartest.service.basic;

import pers.clare.polarbeartest.cache.CacheType;
import pers.clare.polarbeartest.service.ServiceContext;
import org.springframework.stereotype.Service;

@Service
public class BasicContext extends ServiceContext<BasicUserService, BasicSimpleUserService> {

    @Override
    protected CacheType type() {
        return CacheType.Basic;
    }
}
