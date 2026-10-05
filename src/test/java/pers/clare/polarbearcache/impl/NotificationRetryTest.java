package pers.clare.polarbearcache.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pers.clare.polarbearcache.PolarBearCacheEventService;
import pers.clare.polarbearcache.PolarBearCacheProperties;
import pers.clare.polarbearcache.proccessor.CacheAnnotationFactory;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationRetryTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void retryBatchStopsAtFirstFailureAndKeepsRemainingNotifications(int successesBeforeFailure) {
        PolarBearCacheEventService events = mock(PolarBearCacheEventService.class);
        BasicCacheManager manager = manager(events, new PolarBearCacheProperties());
        try {
            doThrow(new IllegalStateException("offline")).when(events).send(anyString());
            manager.evictNotify("cache", "one");
            manager.evictNotify("cache", "two");
            manager.evictNotify("cache", "three");
            clearInvocations(events);
            AtomicInteger attempts = new AtomicInteger();
            doAnswer(invocation -> {
                if (attempts.incrementAndGet() > successesBeforeFailure) {
                    throw new IllegalStateException("offline");
                }
                return null;
            }).when(events).send(anyString());

            manager.retryNotifications();
            verify(events, times(successesBeforeFailure + 1)).send(anyString());

            clearInvocations(events);
            doNothing().when(events).send(anyString());
            manager.retryNotifications();
            verify(events, times(3 - successesBeforeFailure)).send(anyString());
            clearInvocations(events);
            manager.retryNotifications();
            verifyNoInteractions(events);
        } finally {
            manager.destroy();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"cache\nkey", "cache", ""})
    void failuresAreRetriedUntilSuccess(String body) {
        PolarBearCacheEventService events = mock(PolarBearCacheEventService.class);
        doThrow(new IllegalStateException("offline"))
                .doThrow(new IllegalStateException("offline"))
                .doNothing().when(events).send(endsWith("\n" + body));
        BasicCacheManager manager = manager(events, new PolarBearCacheProperties());
        try {
            notify(manager, body);
            manager.retryNotifications();
            manager.retryNotifications();
            manager.retryNotifications();
            verify(events, times(3)).send(endsWith("\n" + body));
        } finally {
            manager.destroy();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"cache\nkey", "cache", ""})
    void matchingSuccessCancelsPendingRetry(String body) {
        PolarBearCacheEventService events = mock(PolarBearCacheEventService.class);
        doThrow(new IllegalStateException("offline")).doNothing().when(events).send(endsWith("\n" + body));
        BasicCacheManager manager = manager(events, new PolarBearCacheProperties());
        try {
            notify(manager, body);
            notify(manager, body);
            manager.retryNotifications();
            verify(events, times(2)).send(endsWith("\n" + body));
        } finally {
            manager.destroy();
        }
    }

    @Test
    void failuresAreCoalescedAndUnrelatedSuccessDoesNotCancelThem() {
        PolarBearCacheEventService events = mock(PolarBearCacheEventService.class);
        doThrow(new IllegalStateException("offline")).when(events).send(endsWith("\ncache\nkey"));
        BasicCacheManager manager = manager(events, new PolarBearCacheProperties());
        try {
            manager.evictNotify("cache", "key");
            manager.evictNotify("cache", "key");
            manager.evictNotify("cache", "other");
            doNothing().when(events).send(endsWith("\ncache\nkey"));
            manager.retryNotifications();
            manager.retryNotifications();
            verify(events, times(3)).send(endsWith("\ncache\nkey"));
            verify(events).send(endsWith("\ncache\nother"));
        } finally {
            manager.destroy();
        }
    }

    @Test
    void newerFailureSurvivesConcurrentSuccessfulRetry() throws Exception {
        PolarBearCacheEventService events = mock(PolarBearCacheEventService.class);
        CountDownLatch retryEntered = new CountDownLatch(1);
        CountDownLatch releaseRetry = new CountDownLatch(1);
        CountDownLatch newRequestStarted = new CountDownLatch(1);
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
            int attempt = attempts.incrementAndGet();
            if (attempt == 2) {
                retryEntered.countDown();
                assertTrue(releaseRetry.await(5, TimeUnit.SECONDS));
            }
            if (attempt == 1 || attempt == 3) throw new IllegalStateException("offline");
            return null;
        }).when(events).send(endsWith("\ncache\nkey"));
        BasicCacheManager manager = manager(events, new PolarBearCacheProperties());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            manager.evictNotify("cache", "key");
            Future<?> retry = executor.submit(manager::retryNotifications);
            assertTrue(retryEntered.await(5, TimeUnit.SECONDS));
            Future<?> request = executor.submit(() -> {
                newRequestStarted.countDown();
                manager.evictNotify("cache", "key");
            });
            assertTrue(newRequestStarted.await(5, TimeUnit.SECONDS));
            // The same-key request completes while the retry is still blocked in send().
            request.get(5, TimeUnit.SECONDS);
            releaseRetry.countDown();
            retry.get(5, TimeUnit.SECONDS);
            manager.retryNotifications();
            manager.retryNotifications();
            assertEquals(4, attempts.get());
        } finally {
            releaseRetry.countDown();
            executor.shutdownNow();
            manager.destroy();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void successfulPublishDoesNotRemoveFailureRecordedWhileSending(boolean existingFailure) throws Exception {
        PolarBearCacheEventService events = mock(PolarBearCacheEventService.class);
        BasicCacheManager manager = manager(events, new PolarBearCacheProperties());
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            if (existingFailure) {
                doThrow(new IllegalStateException("initial failure")).when(events).send(anyString());
                manager.clearNotify("cache");
            }
            AtomicInteger attempts = new AtomicInteger();
            doAnswer(invocation -> {
                int attempt = attempts.incrementAndGet();
                if (attempt == 1) {
                    entered.countDown();
                    assertTrue(release.await(5, TimeUnit.SECONDS));
                } else if (attempt == 2) {
                    throw new IllegalStateException("new failure");
                }
                return null;
            }).when(events).send(anyString());
            Future<?> successful = executor.submit(() -> manager.clearNotify("cache"));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            executor.submit(() -> manager.clearNotify("cache")).get(5, TimeUnit.SECONDS);
            release.countDown();
            successful.get(5, TimeUnit.SECONDS);
            manager.retryNotifications();
            manager.retryNotifications();
            assertEquals(3, attempts.get());
        } finally {
            release.countDown();
            executor.shutdownNow();
            manager.destroy();
        }
    }

    @Test
    void failedRetryGetsNewIdentitySoConcurrentSuccessCannotEraseIt() throws Exception {
        PolarBearCacheEventService events = mock(PolarBearCacheEventService.class);
        BasicCacheManager manager = manager(events, new PolarBearCacheProperties());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
            int attempt = attempts.incrementAndGet();
            if (attempt == 2) {
                entered.countDown();
                assertTrue(release.await(5, TimeUnit.SECONDS));
            }
            if (attempt == 1 || attempt == 3) throw new IllegalStateException("offline");
            return null;
        }).when(events).send(anyString());
        try {
            manager.clearNotify("cache");
            Future<?> successful = executor.submit(() -> manager.clearNotify("cache"));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            executor.submit(manager::retryNotifications).get(5, TimeUnit.SECONDS);
            release.countDown();
            successful.get(5, TimeUnit.SECONDS);
            manager.retryNotifications();
            manager.retryNotifications();
            assertEquals(4, attempts.get());
        } finally {
            release.countDown();
            executor.shutdownNow();
            manager.destroy();
        }
    }

    @Test
    void retriesUseConfiguredScheduleAndStopOnShutdown() throws Exception {
        PolarBearCacheEventService events = mock(PolarBearCacheEventService.class);
        CountDownLatch retried = new CountDownLatch(1);
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
            if (attempts.incrementAndGet() == 1) throw new IllegalStateException("offline");
            retried.countDown();
            return null;
        }).when(events).send(endsWith("\ncache"));
        BasicCacheManager manager = manager(events, new PolarBearCacheProperties()
                .setNotificationRetryInterval(Duration.ofMillis(20)));
        try {
            manager.clearNotify("cache");
            manager.run();
            assertTrue(retried.await(5, TimeUnit.SECONDS));
        } finally {
            manager.destroy();
        }
        ScheduledExecutorService scheduler = (ScheduledExecutorService)
                org.springframework.test.util.ReflectionTestUtils.getField(manager, "executor");
        assertNotNull(scheduler);
        assertTrue(scheduler.isShutdown());
        assertTrue(scheduler.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(2, attempts.get());
    }

    @Test
    void retryRetainsSenderIdAndIgnoresOwnEcho() {
        PolarBearCacheEventService events = mock(PolarBearCacheEventService.class);
        when(events.isAvailable()).thenReturn(true);
        BasicCacheManager manager = manager(events, new PolarBearCacheProperties());
        try {
            manager.getCache("cache").put("key", "value");
            doThrow(new IllegalStateException("offline")).when(events).send(endsWith("\ncache\nkey"));
            manager.evictNotify("cache", "key");
            doAnswer(invocation -> {
                manager.receive(invocation.getArgument(0));
                return null;
            }).when(events).send(endsWith("\ncache\nkey"));
            manager.retryNotifications();
            assertNotNull(manager.getCache("cache").get("key"));
            manager.receive("cache\nkey");
            assertNull(manager.getCache("cache").get("key"));
        } finally {
            manager.destroy();
        }
    }

    @Test
    void retryIntervalMustBeAtLeastOneMillisecond() {
        PolarBearCacheProperties properties = new PolarBearCacheProperties();
        assertEquals(Duration.ofSeconds(5), properties.getNotificationRetryInterval());
        assertThrows(IllegalArgumentException.class, () -> properties.setNotificationRetryInterval(null));
        assertThrows(IllegalArgumentException.class, () -> properties.setNotificationRetryInterval(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> properties.setNotificationRetryInterval(Duration.ofMillis(-1)));
        assertThrows(IllegalArgumentException.class, () -> properties.setNotificationRetryInterval(Duration.ofNanos(1)));
    }

    private static BasicCacheManager manager(PolarBearCacheEventService events, PolarBearCacheProperties properties) {
        return new BasicCacheManager(new CacheAnnotationFactory(), properties, null, events);
    }

    private static void notify(BasicCacheManager manager, String body) {
        if (body.isEmpty()) manager.clearAllNotify();
        else if (body.contains("\n")) manager.evictNotify("cache", "key");
        else manager.clearNotify("cache");
    }
}
