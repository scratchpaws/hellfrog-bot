package besus.utils.collection.cache;


import besus.utils.collection.SelfExpiringHashMap;
import besus.utils.func.Func;
import rx.functions.Func0;

import java.util.*;

/**
 * Simple cache for objects with costly initialization
 */
public abstract class Cache<K, V> {
//    private static Logger logger = LogFactory.getLogger(Cache.class);

    private final Map<K, V> container;

    protected Cache(Map<K, V> container) {
        this.container = container;
    }

    public V get(K key, Func.AnyFunc<K, V> initializer) {
        return container.computeIfAbsent(key, initializer);
    }

    public V get(K key) {
//        logger.trace(toString() + " get key " + key);
        return container.get(key);
    }

    public V get(K key, Func0<V> initializer) {
        return container.computeIfAbsent(key, k -> initializer.call());
    }

    public V put(K key, V value) {
//        logger.trace(toString() + " put key " + key);
        container.put(key, value);
        return value;
    }

    public void clear() {
        container.clear();
    }

    public int size() {
        return container.size();
    }

    public abstract String type();

    public V any() {
        if (container.size() == 0) {
            return null;
        }
        return container.values().iterator().next();
    }

    /**
     * use this class carefully: may cause OOM
     *
     * @param <K>
     * @param <V>
     */
    public static class Permanent<K, V> extends Cache<K, V> {
        public Permanent() {
            super(new HashMap<>());
        }

        @Override
        public String type() {
            return "Permanent cache";
        }
    }

    /**
     * use this class carefully: may cause OOM
     *
     * @param <K>
     * @param <V>
     */
    public static class Weak<K, V> extends Cache<K, V> {
        public Weak() {
            super(new WeakHashMap<>());
        }

        @Override
        public String type() {
            return "Weak cache";
        }
    }

    public static class ZeroCache<K, V> extends Cache<K, V> {
        public ZeroCache() {
            super(new Map<K, V>() {
                @Override
                public int size() {
                    return 0;
                }

                @Override
                public boolean isEmpty() {
                    return true;
                }

                @Override
                public boolean containsKey(Object key) {
                    return false;
                }

                @Override
                public boolean containsValue(Object value) {
                    return false;
                }

                @Override
                public V get(Object key) {
                    return null;
                }

                @Override
                public V put(K key, V value) {
                    return null;
                }

                @Override
                public V remove(Object key) {
                    return null;
                }

                @Override
                public void putAll(Map<? extends K, ? extends V> m) {
                }

                @Override
                public void clear() {
                }

                @Override
                public Set<K> keySet() {
                    return Collections.emptySet();
                }

                @Override
                public Collection<V> values() {
                    return Collections.emptySet();
                }

                @Override
                public Set<Entry<K, V>> entrySet() {
                    return Collections.emptySet();
                }
            });
        }

        @Override
        public String type() {
            return "Zero cache";
        }
    }

    public static class TimeoutCache<K, V> extends Cache<K, V> {
        private final long timeout;

        public TimeoutCache(long timeout, Map<K, SelfExpiringHashMap<K, V>.ExpiringKey<K>> container) {
            super(new SelfExpiringHashMap<K, V>(timeout, container, false));
            this.timeout = timeout;
        }

        @Override
        public String type() {
            return "Timeout cache(" + timeout + ")";
        }
    }
}
