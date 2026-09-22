package pers.clare.polarbearcache.event;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventSenderQueueTest {

    @Test
    void removesEmptyClearBlockAfterPolling() throws Exception {
        EventSenderQueue queue = new EventSenderQueue();
        queue.add("cache");

        assertTrue(queue.poll("cache"));
        assertEquals(0, mapSize(queue, "clearSenderQueue"));
    }

    @Test
    void removesEmptyEvictBlockAfterPolling() throws Exception {
        EventSenderQueue queue = new EventSenderQueue();
        queue.add("cache", "key");

        assertTrue(queue.poll("cache", "key"));
        assertEquals(0, mapSize(queue, "evictBlocks"));
    }

    @SuppressWarnings("unchecked")
    private int mapSize(EventSenderQueue queue, String fieldName) throws Exception {
        Field field = EventSenderQueue.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return ((Map<String, ?>) field.get(queue)).size();
    }
}
