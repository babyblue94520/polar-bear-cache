package pers.clare.core.cache;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.CacheManager;
import pers.clare.core.lock.IdLock;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Log4j2
public class BeeCacheManager implements CacheManager, CommandLineRunner, InitializingBean {
    protected static String id;
    protected static char[] idChars;
    protected static final char initSplit = ':';
    protected static final char idSplit = '@';
    protected static final char eventSplit = ',';
    protected static final int idLength = 13;
    protected static final int eventIndex = 14;

    protected final ConcurrentMap<String, BeeCache> cacheMap = new ConcurrentHashMap<>(16);

    protected final IdLock<Object> locks = new IdLock<>() {
    };

    private final BeeCacheMQService beeCacheMQService;

    {
        resetId();
    }

    public BeeCacheManager(BeeCacheMQService beeCacheMQService) {
        this.beeCacheMQService = beeCacheMQService;
    }

    public void afterPropertiesSet() {
        if (beeCacheMQService == null) return;
        beeCacheMQService.addListener((origin, data) -> {
            try {
                parse(data);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        });
    }

    @Override
    public void run(String... args) {
        if (beeCacheMQService == null) return;
        ask();
    }

    private void resetId() {
        id = String.valueOf(System.currentTimeMillis());
        idChars = id.toCharArray();
    }

    /**
     * 解析收到信息
     *
     * @param data
     */
    private void parse(String data) {
        log.debug(data);
        char[] cs = data.toCharArray();
        if (cs.length > eventIndex) {
            switch (cs[idLength]) {
                case initSplit:
                    if (isMe(cs)) return;
                    switch (cs[eventIndex]) {
                        // 處理發問
                        case BeeAction.ASK:
                            replay();
                            return;
                        // 處理回答
                        case BeeAction.REPLY:
                            if (isSame(cs, 15)) {
                                resetId();
                                ask();
                            }
                            return;
                    }
                    break;
                case idSplit:
                    if (isMe(cs)) return;
                    parseEvictEvent(cs, eventIndex);
                    return;
            }
        }
        // 向下相容
        parseEvictEvent(cs, 0);
    }

    /**
     * 檢查ID是否相同
     *
     * @param cs
     * @param offset
     * @return
     */
    private boolean isSame(char[] cs, int offset) {
        for (int i = 0; i < idLength; i++) {
            if (idChars[i] != cs[i + offset]) return false;
        }
        return true;
    }

    /**
     * 檢查是否是自己發送
     *
     * @param cs
     * @return
     */
    private boolean isMe(char[] cs) {
        for (int i = 0; i < idLength; i++) {
            if (idChars[i] != cs[i]) return false;
        }
        return true;
    }

    /**
     * 詢問所有服務ID，來解決ID衝突
     */
    private void ask() {
        beeCacheMQService.send(id + initSplit + BeeAction.ASK);
    }

    /**
     * 回復ID
     */
    private void replay() {
        beeCacheMQService.send(id + initSplit + BeeAction.REPLY + id);
    }

    /**
     * @param cs
     * @param offset
     */
    private void parseEvictEvent(char[] cs, int offset) {
        int l = cs.length;
        if (l == offset) {
            onlyClearAll();
        } else {
            String name = null;
            char c;
            int i = offset;
            for (; i < l; i++) {
                c = cs[i];
                if (c == eventSplit) {
                    name = new String(cs, offset, i - offset);
                    offset = i + 1;
                    break;
                }
            }
            if (name == null) {
                name = new String(cs, offset, l - offset);
                offset++;
            }
            if (i == l) {
                onlyClear(name);
            } else {
                onlyEvict(name, new String(cs, offset, l - offset));
            }
        }
    }


    /**
     * 清除並發布通知
     *
     * @param name
     * @param key
     */
    void clearNotify(
            String name
            , String key
    ) {
        if (beeCacheMQService == null) return;
        beeCacheMQService.send(id + idSplit + name + eventSplit + key);

    }

    /**
     * 清除並發布通知
     *
     * @param name
     */
    void clearNotify(
            String name
    ) {
        if (beeCacheMQService == null) return;
        beeCacheMQService.send(id + idSplit + name);

    }

    /**
     * 清除全部並發布通知
     */
    void clearNotify() {
        if (beeCacheMQService == null) return;
        beeCacheMQService.send(id + idSplit);

    }

    @Override
    public BeeCache getCache(String name) {
        BeeCache cache = cacheMap.get(name);
        if (cache != null) return cache;
        String duration = findDuration(name);
        synchronized (locks.getLock(name)) {
            cache = cacheMap.get(name);
            if (cache != null) return cache;
            if (duration == null) {
                cache = new BeeCache(this, name);
            } else {
                cache = new BusyBeeCache(this, name, Duration.parse(duration).toMillis());
            }
            cacheMap.put(name, cache);
        }
        return cache;
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(this.cacheMap.keySet());
    }


    /**
     * 純粹清除
     *
     * @param name
     * @param key
     */
    public void evict(
            String name
            , Object key
    ) {
        if (name == null || key == null) return;
        getCache(name).evict(key);
    }

    /**
     * @param name
     * @param key
     */
    public void onlyEvict(
            String name
            , String key
    ) {
        if (name == null || key == null) return;
        getCache(name).onlyEvict(key);
        log.debug("clear {} {}", name, key);
    }

    /**
     * 清除資料並發出通知
     *
     * @param name
     */
    public void clear(
            String name
    ) {
        if (name == null) return;
        getCache(name).clear();
    }

    /**
     * 只清除資料
     *
     * @param name
     */
    public void onlyClear(
            String name
    ) {
        if (name == null) return;
        getCache(name).onlyClear();
        log.debug("clear {}", name);
    }

    /**
     * 純粹清除全部
     */
    public void clearAll() {
        onlyClearAll();
        clearNotify();
    }

    /**
     * 純粹清除全部
     */
    public void onlyClearAll() {
        Collection<String> names = getCacheNames();
        for (String name : names) {
            getCache(name).onlyClear();
        }
    }

    private String findDuration(String name) {
        char[] cs = name.toCharArray();
        Integer index = null;
        for (int i = 0, l = cs.length - 1; i < l; i++) {
            if (cs[i] == ' ') {
                index = i + 1;
                break;
            }
        }
        if (index == null) return null;
        return new String(cs, index, cs.length - index);
    }
}
