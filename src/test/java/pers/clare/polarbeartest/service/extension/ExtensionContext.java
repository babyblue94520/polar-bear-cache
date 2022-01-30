package pers.clare.polarbeartest.service.extension;

import pers.clare.polarbeartest.cache.CacheType;
import pers.clare.polarbeartest.service.ServiceContext;
import org.springframework.stereotype.Service;

@Service
public class ExtensionContext extends ServiceContext<ExtensionUserService, ExtensionSimpleUserService> {

    @Override
    protected CacheType type() {
        return CacheType.Extension;
    }
}
