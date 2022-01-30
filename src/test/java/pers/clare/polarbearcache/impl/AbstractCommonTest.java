package pers.clare.polarbearcache.impl;

import pers.clare.polarbeartest.cache.CacheType;
import pers.clare.polarbeartest.service.AbstractSimpleUserService;
import pers.clare.polarbeartest.service.AbstractUserService;
import pers.clare.polarbeartest.service.ServiceContext;
import pers.clare.polarbeartest.vo.ReloadUser;
import pers.clare.polarbeartest.vo.SimpleUser;
import pers.clare.polarbeartest.vo.User;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
abstract class AbstractCommonTest<UserService extends AbstractUserService, SimpleUserService extends AbstractSimpleUserService> {

    private final Random random = new Random();

    UserService randomUserService() {
        List<UserService> instances = userServices();
        return instances.get(random.nextInt(instances.size()));
    }


    protected List<UserService> userServices() {
        return ServiceContext.userServices(getType());
    }

    protected List<SimpleUserService> simpleUserServices() {
        return ServiceContext.simpleUserServices(getType());
    }

    abstract protected CacheType getType();


    void waitingNotice() {
        // waiting all notice
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    User create() {
        return randomUserService().find(System.currentTimeMillis());
    }

    User create(long id) {
        return randomUserService().find(id);
    }

    Map<UserService, User> getUserMapFromCaching(Long id) {
        User prev = null, current;
        Map<UserService, User> map = new HashMap<>();
        for (UserService instance : userServices()) {
            assertNotSame(prev, (current = instance.findCaching(id)));
            map.put(instance, instance.find(id));
            prev = current;
        }
        return map;
    }

    Map<UserService, User> getUserMap(Long id) {
        User prev = null, current;
        Map<UserService, User> map = new HashMap<>();
        for (UserService instance : userServices()) {
            assertNotSame(prev, (current = instance.find(id)));
            map.put(instance, instance.find(id));
            prev = current;
        }
        return map;
    }

    Map<UserService, List<User>> getUserListMap() {
        List<User> prev = null, current;
        Map<UserService, List<User>> map = new HashMap<>();
        for (UserService instance : userServices()) {
            assertNotSame(prev, (current = instance.findAll()));
            map.put(instance, instance.findAll());
            prev = current;
        }
        return map;
    }

    Map<SimpleUserService, SimpleUser> getSimpleUserMap(Long id) {
        SimpleUser prev = null, current;
        Map<SimpleUserService, SimpleUser> map = new HashMap<>();
        for (SimpleUserService instance : simpleUserServices()) {
            assertNotSame(prev, (current = instance.find(id)));
            map.put(instance, instance.find(id));
            prev = current;
        }
        return map;
    }

    void userMapSame(Map<UserService, User> map) {
        User user;
        for (UserService instance : userServices()) {
            user = map.get(instance);
            assertSame(user, instance.find(user.getId()));
        }
    }

    void userMapNotSame(Map<UserService, User> map) {
        for (UserService instance : userServices()) {
            User user = map.get(instance);
            User actual = instance.find(user.getId());
            assertNotSame(user, actual, () -> "unexpected: <" + user + "> not same but was: <" + actual + ">");
        }
    }

    void userListMapSame(Map<UserService, List<User>> map) {
        List<User> userList;
        for (UserService instance : userServices()) {
            userList = map.get(instance);
            assertSame(userList, instance.findAll());
        }
    }

    void userListMapNotSame(Map<UserService, List<User>> map) {
        List<User> userList;
        for (UserService instance : userServices()) {
            userList = map.get(instance);
            assertNotSame(userList, instance.findAll());
        }
    }

    void simpleUserMapSame(Map<SimpleUserService, SimpleUser> map) {
        SimpleUser simpleUser;
        for (SimpleUserService instance : simpleUserServices()) {
            simpleUser = map.get(instance);
            assertSame(simpleUser, instance.find(simpleUser.getId()));
        }
    }

    void simpleUserMapNotSame(Map<SimpleUserService, SimpleUser> map) {
        SimpleUser simpleUser;
        for (SimpleUserService instance : simpleUserServices()) {
            simpleUser = map.get(instance);
            assertNotSame(map.get(instance), instance.find(simpleUser.getId()));
        }
    }

    @Test
    @Order(1)
    void findNull() {
        assertNull(randomUserService().findNull(null));
        assertNull(randomUserService().findNull(System.currentTimeMillis()));
    }

    @Test
    @Order(2)
    void cacheable() {
        User user = create();
        userMapSame(getUserMap(user.getId()));
    }

    @Test
    @Order(2)
    void cacheable_sync() throws InterruptedException, ExecutionException {
        UserService userService = randomUserService();
        assertNull(userService.findSync(1L));

        ConcurrentMap<User, User> userMap = new ConcurrentHashMap<>();
        int thread = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(thread);
        List<Callable<Void>> tasks = new ArrayList<>();
        long id = System.currentTimeMillis();
        for (int i = 0; i < thread; i++) {
            tasks.add(() -> {
                User user = userService.findSync(id);
                userMap.put(user, user);
                return null;
            });
        }
        for (Future<Void> f : executorService.invokeAll(tasks)) {
            f.get();
        }
        executorService.shutdown();
        assertEquals(1, userMap.size());
    }

    @Test
    @Order(3)
    void evict() {
        User user = create();
        User user1 = create(1);
        User user10 = create(10);
        Map<UserService, User> map = getUserMap(user.getId());
        userMapSame(map);
        Map<UserService, User> map1 = getUserMap(user1.getId());
        userMapSame(map1);
        Map<UserService, User> map10 = getUserMap(user10.getId());
        userMapSame(map10);

        User modifyUser = randomUserService().update(user.getId());
        assertNotSame(user, modifyUser);

        waitingNotice();

        userMapNotSame(map);
        userMapSame(map1);
        userMapNotSame(map10);
        userMapSame(getUserMap(user.getId()));
        userMapSame(getUserMap(user10.getId()));
    }

    @Test
    @Order(4)
    void put() {
        User user = create();
        Map<UserService, User> map = getUserMap(user.getId());
        userMapSame(map);

        UserService userService = randomUserService();
        User modifyUser = userService.updatePut(user.getId());
        assertNotSame(user, modifyUser);

        assertSame(modifyUser, userService.find(user.getId()));

        waitingNotice();

        for (UserService instance : userServices()) {
            if (instance == userService) {
                assertSame(modifyUser, instance.find(user.getId()));
            } else {
                assertNotSame(user, instance.find(user.getId()));
            }
        }

        userMapSame(getUserMap(user.getId()));
    }

    @Test
    @Order(4)
    void putResultKey() {
        User user = create();
        Map<UserService, User> map = getUserMap(user.getId());
        userMapSame(map);

        UserService userService = randomUserService();
        User modifyUser = userService.updatePutResultKey(user.getId());
        assertNotSame(user, modifyUser);

        assertSame(modifyUser, userService.find(user.getId()));

        waitingNotice();

        for (UserService instance : userServices()) {
            if (instance == userService) {
                assertSame(modifyUser, instance.find(user.getId()));
            } else {
                assertNotSame(user, instance.find(user.getId()));
            }
        }

        userMapSame(getUserMap(user.getId()));

        modifyUser = userService.updatePutResultKey(null);
        assertSame(null, modifyUser);
    }

    @Test
    @Order(5)
    void depend_on_insert() {
        User user = create();
        Map<UserService, User> userMap = getUserMap(user.getId());
        userMapSame(userMap);

        Map<UserService, List<User>> userListMap = getUserListMap();
        userListMapSame(userListMap);

        Map<SimpleUserService, SimpleUser> simpleUserMap = getSimpleUserMap(user.getId());
        simpleUserMapSame(simpleUserMap);

        randomUserService().insert();

        waitingNotice();

        userMapSame(userMap);

        userListMapNotSame(userListMap);

        simpleUserMapSame(simpleUserMap);
    }

    @Test
    @Order(6)
    void depend_on_update() {
        User user = create();
        Map<UserService, User> userMap = getUserMap(user.getId());
        userMapSame(userMap);

        Map<UserService, List<User>> userListMap = getUserListMap();
        userListMapSame(userListMap);

        Map<SimpleUserService, SimpleUser> simpleUserMap = getSimpleUserMap(user.getId());
        simpleUserMapSame(simpleUserMap);

        Map<SimpleUserService, SimpleUser> simpleUserMap2 = getSimpleUserMap(user.getId() + 999);
        simpleUserMapSame(simpleUserMap2);

        randomUserService().update(user.getId());

        waitingNotice();

        userMapNotSame(userMap);

        userListMapNotSame(userListMap);

        simpleUserMapNotSame(simpleUserMap);

        simpleUserMapSame(simpleUserMap2);
    }

    @Test
    @Order(7)
    void reload_on_evict() {
        UserService userService = randomUserService();
        User user = userService.find(System.currentTimeMillis());
        ReloadUser reloadUser1 = userService.findReload(1L);

        ReloadUser reloadUser2 = new ReloadUser(reloadUser1.getId(), "reloadUser2", System.currentTimeMillis());
        userService.setReloadUser(reloadUser2);

        userService.update(user.getId());

        ReloadUser reloadUser3 = userService.findReload(reloadUser1.getId());
        assertNotEquals(reloadUser1, reloadUser3);
        assertEquals(reloadUser2, userService.findReload(reloadUser1.getId()));
    }

    @Test
    @Order(8)
    void cachingCacheable() {
        User user = create();
        userMapSame(getUserMapFromCaching(user.getId()));
    }


    @Test
    @Order(9)
    void cachingPut() {
        User user = create();
        Map<UserService, User> map = getUserMapFromCaching(user.getId());
        userMapSame(map);

        UserService userService = randomUserService();
        User modifyUser = userService.updateCachingPut(user.getId());
        assertNotSame(user, modifyUser);

        assertSame(modifyUser, userService.find(user.getId()));

        waitingNotice();

        for (UserService instance : userServices()) {
            if (instance == userService) {
                assertSame(modifyUser, instance.find(user.getId()));
            } else {
                assertNotSame(user, instance.find(user.getId()));
            }
        }

        userMapSame(getUserMap(user.getId()));
    }

}
