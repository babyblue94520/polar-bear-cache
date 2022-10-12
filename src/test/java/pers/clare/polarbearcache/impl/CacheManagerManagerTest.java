package pers.clare.polarbearcache.impl;

import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import pers.clare.polarbeartest.ApplicationTest2;
import pers.clare.polarbeartest.cache.CacheType;
import pers.clare.polarbeartest.cache.CoreCacheConfig;
import pers.clare.polarbeartest.service.basic.BasicSimpleUserService;
import pers.clare.polarbeartest.service.basic.BasicUserService;
import pers.clare.polarbeartest.service.config.CacheConfigSimpleUserService;
import pers.clare.polarbeartest.service.config.CacheConfigUserService;
import pers.clare.polarbeartest.service.extension.ExtensionSimpleUserService;
import pers.clare.polarbeartest.service.extension.ExtensionUserService;
import pers.clare.polarbeartest.service.ttl.TTLSimpleUserService;
import pers.clare.polarbeartest.service.ttl.TTLUserService;
import pers.clare.polarbeartest.vo.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@DisplayName("PolarBearCacheManagerTest")
@TestInstance(PER_CLASS)
class CacheManagerManagerTest {

    private final List<ApplicationContext> applications = new ArrayList<>();

    @BeforeAll
    void before() {
        String topic = String.format("--polar-bear-cache.topic=test.%s", UUID.randomUUID());
        for (int i = 0; i < 3; i++) {
            applications.add(SpringApplication.run(ApplicationTest2.class, topic));
        }
    }

    @AfterAll
    void after() {
        for (ApplicationContext application : applications) {
            SpringApplication.exit(application);
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    public class basic extends AbstractCommonTest<BasicUserService, BasicSimpleUserService> {

        @Override
        protected CacheType getType() {
            return CacheType.Basic;
        }

        @Test
        @Order(10)
        void notTimeout() throws InterruptedException {
            User user = create();
            Map<BasicUserService, User> map = getUserMap(user.getId());
            userMapSame(map);
            Thread.sleep(CoreCacheConfig.EffectiveTime * 2);
            userMapSame(map);
        }

    }

    @Nested
    @TestInstance(PER_CLASS)
    public class ttl extends AbstractCommonTest<TTLUserService, TTLSimpleUserService> {

        @Override
        protected CacheType getType() {
            return CacheType.TTL;
        }

        @Test
        @Order(10)
        void timeout() throws InterruptedException {
            User user = create();
            Map<TTLUserService, User> map = getUserMap(user.getId());
            userMapSame(map);
            Thread.sleep(CoreCacheConfig.EffectiveTime * 2);
            userMapNotSame(map);
        }

        @Test
        @Order(11)
        void timeout2() throws InterruptedException {
            User user = create();
            Map<TTLUserService, List<User>> map = getUserListMap();
            userListMapSame(map);
            Thread.sleep(CoreCacheConfig.EffectiveTime2 + 1000);
            userListMapNotSame(map);
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    public class extension extends AbstractCommonTest<ExtensionUserService, ExtensionSimpleUserService> {

        @Override
        protected CacheType getType() {
            return CacheType.Extension;
        }

        @Test
        @Order(10)
        void timeout() throws InterruptedException {
            User user = create();
            Map<ExtensionUserService, User> map = getUserMap(user.getId());
            userMapSame(map);
            Thread.sleep(CoreCacheConfig.EffectiveTime + 1000);
            userMapNotSame(map);
        }

        @Test
        @Order(10)
        void notTimeout() throws InterruptedException {
            User user = create();
            Map<ExtensionUserService, User> map = getUserMap(user.getId());
            userMapSame(map);
            for (int i = 0; i < 3; i++) {
                Thread.sleep(CoreCacheConfig.EffectiveTime / 2);
                userMapSame(map);
            }
            Thread.sleep(CoreCacheConfig.EffectiveTime + 1000);
            userMapNotSame(map);
        }

        @Test
        @Order(11)
        void timeout2() throws InterruptedException {
            User user = create();
            Map<ExtensionUserService, List<User>> map = getUserListMap();
            userListMapSame(map);
            Thread.sleep(CoreCacheConfig.EffectiveTime2 + 1000);
            userListMapNotSame(map);
        }

        @Test
        @Order(11)
        void notTimeout2() throws InterruptedException {
            User user = create();
            Map<ExtensionUserService, List<User>> map = getUserListMap();
            userListMapSame(map);
            for (int i = 0; i < 3; i++) {
                Thread.sleep(CoreCacheConfig.EffectiveTime / 2);
                userListMapSame(map);
            }
            Thread.sleep(CoreCacheConfig.EffectiveTime + 1000);
            userListMapNotSame(map);
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    public class cacheConfig extends AbstractCommonTest<CacheConfigUserService, CacheConfigSimpleUserService> {

        @Override
        protected CacheType getType() {
            return CacheType.CacheConfig;
        }

        @Test
        @Order(10)
        void notTimeout() throws InterruptedException {
            User user = create();
            Map<CacheConfigUserService, User> map = getUserMap(user.getId());
            userMapSame(map);
            Thread.sleep(CoreCacheConfig.EffectiveTime * 2);
            userMapSame(map);
        }
    }
}
