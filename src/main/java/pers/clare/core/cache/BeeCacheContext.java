package pers.clare.core.cache;

public abstract class BeeCacheContext {
    private static BeeCacheManager manager;

    static void setManager(BeeCacheManager manager) {
        if (BeeCacheContext.manager != null) return;
        BeeCacheContext.manager = manager;
    }

    public static <T> T get(String name, Object key, Class<T> clazz) {
        return (T) manager.getCacheOrTemp(name).get(key);
    }

    public static void clear(String name, Object key) {
        manager.clear(name, String.valueOf(key));
    }

    public static void onlyClearNotify(String name, Object key) {
        manager.clearNotify(name, String.valueOf(key));
    }

    public static void clear(String name) {
        manager.clear(name);
    }

    public static void onlyClearNotify(String name) {
        manager.clearNotify(name);
    }

    public static void clear() {
        manager.clear();
    }

    public static void onlyClearNotify() {
        manager.clearNotify();
    }
}
