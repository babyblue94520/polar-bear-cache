package pers.clare.polarbearcache.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pers.clare.polarbeartest.ApplicationTest2;
import pers.clare.polarbeartest.cache.CacheEventServiceImpl;
import pers.clare.polarbeartest.service.basic.BasicUserService;
import pers.clare.polarbeartest.vo.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@DisplayName("AvailableTest")
@TestInstance(PER_CLASS)
@SpringBootTest(classes = ApplicationTest2.class)
class CacheEventAvailableTest {

    @Autowired
    private CacheEventServiceImpl cacheEventService;
    @Autowired
    private BasicUserService basicUserService;

    @Test
    void available() {
        long id = System.currentTimeMillis();
        User user = basicUserService.find(id);
        assertSame(user, basicUserService.find(id));

        cacheEventService.setAvailable(false);
        assertNotSame(user, basicUserService.find(id));

        cacheEventService.setAvailable(true);
        user = basicUserService.find(id);
        assertSame(user, basicUserService.find(id));
    }

    @Test
    void performance() throws ExecutionException, InterruptedException {
        run();
        cacheEventService.setAvailable(false);
        run();
        cacheEventService.setAvailable(true);
        run();
        cacheEventService.setAvailable(false);
        run();
    }

    void run() throws ExecutionException, InterruptedException {
        int thread = 10;
        long max = 1000000;
        AtomicLong count = new AtomicLong();

        long id = System.currentTimeMillis();
        long startTime = System.currentTimeMillis();
        Runnable shutdown = performance(thread, () -> {
            while (count.incrementAndGet() <= max) {
                basicUserService.find(id);
            }
            count.decrementAndGet();
            return null;
        });
        long time = System.currentTimeMillis() - startTime;
        System.out.printf("%d %d %d/s\n", max, time, count.get() * 1000 / time);
        shutdown.run();
    }


    private Runnable performance(int thread, Callable<Void> callable) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(thread);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < thread; i++) {
            tasks.add(callable);
        }
        for (Future<Void> future : executorService.invokeAll(tasks)) {
            future.get();
        }
        return executorService::shutdown;
    }
}
