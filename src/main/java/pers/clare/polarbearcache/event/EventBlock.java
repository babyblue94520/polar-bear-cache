package pers.clare.polarbearcache.event;

import pers.clare.polarbearcache.support.VolatileBlock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

@SuppressWarnings("unused")
public class EventBlock {

    private final VolatileBlock volatileBlock = new VolatileBlock(5000);

    private final ConcurrentMap<String, VolatileBlock> clearBlocks = new ConcurrentHashMap<>(16);

    private final ConcurrentMap<String, ConcurrentMap<String, VolatileBlock>> evictBlocks = new ConcurrentHashMap<>(16);

    public boolean isBlock() {
        return volatileBlock.isBlock();
    }

    public boolean isBlock(String name) {
        return getClearBlock(name).isBlock();
    }

    public boolean isBlock(String name, String key) {
        return getEvictBlock(name, key).isBlock();
    }

    public void block() {
        volatileBlock.block();
    }

    public void block(String name) {
        getClearBlock(name).block();
    }

    public void block(String name, String key) {
        getEvictBlock(name, key).block();
    }

    private final Function<? super String, ? extends ConcurrentMap<String, VolatileBlock>> createConcurrentHashMap = (k) -> new ConcurrentHashMap<>();

    private final Function<? super String, ? extends VolatileBlock> createVolatileBlock = (k) -> new VolatileBlock(5000);

    private VolatileBlock getClearBlock(String name) {
        return clearBlocks.computeIfAbsent(name, createVolatileBlock);
    }

    private VolatileBlock getEvictBlock(String name, String key) {
        return evictBlocks
                .computeIfAbsent(name, createConcurrentHashMap)
                .computeIfAbsent(key, createVolatileBlock);
    }
}
