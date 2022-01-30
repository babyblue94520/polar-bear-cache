package pers.clare.polarbearcache.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@DisplayName("VolatileSenderQueueTest")
@TestInstance(PER_CLASS)
class VolatileSenderQueueTest {

    @Test
    void add() {
        long effectiveTime = 10000L;
        VolatileSenderQueue<Integer> queue = new VolatileSenderQueue<>(effectiveTime);
        queue.add(1);
        assertEquals(1, queue.size());
        assertEquals(1, queue.poll());
        assertEquals(0, queue.size());
    }

    @Test
    void invalidate() throws InterruptedException {
        long effectiveTime = 500;
        VolatileSenderQueue<Integer> queue = new VolatileSenderQueue<>(500);
        queue.add(1);
        assertEquals(1, queue.size());
        Thread.sleep(effectiveTime);
        assertNull(queue.poll());
    }

    @Test
    void race_condition() {
        long effectiveTime = 10000L;
        VolatileSenderQueue<Integer> queue = new VolatileSenderQueue<>(effectiveTime);
        int thread = 5;
        int max = 10000;
        int total = thread * max;
        multi(thread, () -> {
            for (int i = 0; i < max; i++) {
                queue.add(i);
            }
            return null;
        });
        assertEquals(total, queue.size());

        AtomicInteger pollCount = new AtomicInteger();
        multi(thread, () -> {
            while (queue.poll() != null) {
                pollCount.incrementAndGet();
            }
            return null;
        });
        assertEquals(total, pollCount.get());
    }

    @Test
    void race_condition_invalidate() throws InterruptedException {
        long effectiveTime = 5000L;
        VolatileSenderQueue<Integer> queue = new VolatileSenderQueue<>(effectiveTime);
        int thread = 5;
        int max = 10000;
        int total = thread * max;
        int breakpoint = total / 2;
        multi(thread, () -> {
            for (int i = 0; i < max; i++) {
                queue.add(i);
            }
            return null;
        });
        assertEquals(total, queue.size());

        AtomicInteger pollCount = new AtomicInteger();
        multi(thread, () -> {
            while (pollCount.incrementAndGet() <= breakpoint) {
                queue.poll();
            }
            return null;
        });
        assertEquals(breakpoint, queue.size());

        assertNotNull(queue.poll());
        Thread.sleep(effectiveTime);
        assertNull(queue.poll());
    }

    static void multi(int thread, Callable<Void> callable) {
        ExecutorService executorService = Executors.newFixedThreadPool(thread);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < thread; i++) {
            tasks.add(callable);
        }
        try {
            executorService.invokeAll(tasks).forEach(f -> {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executorService.shutdown();
    }
}
