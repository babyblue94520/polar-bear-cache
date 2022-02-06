package pers.clare.polarbeartest.service.extension;

import org.springframework.stereotype.Service;
import pers.clare.polarbeartest.cache.CacheType;
import pers.clare.polarbeartest.service.ServiceContext;

@Service
public class ExtensionContext extends ServiceContext<ExtensionUserService, ExtensionSimpleUserService> {

    @Override
    protected CacheType type() {
        return CacheType.Extension;
    }
}
