package besus.utils.collection;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * A HashMap which entries expires after the specified life time.
 * The life-time can be defined on a per-key basis, or using a default one, that is passed to the
 * constructor.
 *
 * @param <K> the Key type
 * @param <V> the Value type
 */
public class SelfExpiringHashMap<K, V> implements SelfExpiringMap<K, V> {

    private final Map<K, V> internalMap;

    private final Map<K, ExpiringKey<K>> expiringKeys;

    private final boolean renewOnGet;

    /**
     * Holds the map keys using the given life time for expiration.
     */
    private final DelayQueue<ExpiringKey<? extends K>> delayQueue = new DelayQueue<>();

    /**
     * The default max life time in milliseconds.
     */
    private final long maxLifeTimeMillis;


    public SelfExpiringHashMap(long defaultMaxLifeTimeMillis, Map<K, ExpiringKey<K>> storage, boolean renewOnGet) {
        this.renewOnGet = renewOnGet;
        internalMap = new ConcurrentHashMap<>();
        expiringKeys = storage;
        this.maxLifeTimeMillis = defaultMaxLifeTimeMillis;
    }


    @Override
    public int size() {
        cleanup();
        return internalMap.size();
    }

    @Override
    public boolean isEmpty() {
        cleanup();
        return internalMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        cleanup();
        return internalMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        cleanup();
        return internalMap.containsValue(value);
    }

    @Override
    public V get(Object key) {
        cleanup();
        if (renewOnGet) {
            renewKey((K) key);
        }
        return internalMap.get(key);
    }

    @Override
    public V put(K key, V value) {
        return this.put(key, value, maxLifeTimeMillis);
    }

    @Override
    public V put(K key, V value, long lifeTimeMillis) {
        cleanup();
        ExpiringKey<K> delayedKey = new ExpiringKey<>(key, lifeTimeMillis);
        ExpiringKey<K> oldKey = expiringKeys.put(key, delayedKey);
        if (oldKey != null) {
            expireKey(oldKey);
            expiringKeys.put(key, delayedKey);
        }
        delayQueue.offer(delayedKey);
        return internalMap.put(key, value);
    }

    @Override
    public V remove(Object key) {
        V removedValue = internalMap.remove(key);
        expireKey(expiringKeys.remove(key));
        return removedValue;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public boolean renewKey(K key) {
        ExpiringKey<K> delayedKey = expiringKeys.get(key);
        if (delayedKey != null) {
            delayedKey.renew();
            return true;
        }
        return false;
    }

    private void expireKey(ExpiringKey<K> delayedKey) {
        if (delayedKey != null) {
            delayedKey.expire();
            cleanup();
        }
    }

    @Override
    public void clear() {
        delayQueue.clear();
        expiringKeys.clear();
        internalMap.clear();
    }


    @Override
    public Set<K> keySet() {
        return internalMap.keySet();
    }

    @Override
    public Collection<V> values() {
        return internalMap.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return internalMap.entrySet();
    }

    private void cleanup() {
        ExpiringKey<? extends K> delayedKey = delayQueue.poll();
        while (delayedKey != null) {
            internalMap.remove(delayedKey.getKey());
            expiringKeys.remove(delayedKey.getKey());
            delayedKey = delayQueue.poll();
        }
    }

    public class ExpiringKey<KK> implements Delayed {

        private final long maxLifeTimeMillis;
        private final KK key;
        private long startTime = System.currentTimeMillis();

        public ExpiringKey(KK key, long maxLifeTimeMillis) {
            this.maxLifeTimeMillis = maxLifeTimeMillis;
            this.key = key;
        }

        public KK getKey() {
            return key;
        }


        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ExpiringKey<KK> other = (ExpiringKey<KK>) obj;
            return this.key == other.key || (this.key != null && this.key.equals(other.key));
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 31 * hash + (this.key != null ? this.key.hashCode() : 0);
            return hash;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(getDelayMillis(), TimeUnit.MILLISECONDS);
        }

        private long getDelayMillis() {
            return (startTime + maxLifeTimeMillis) - System.currentTimeMillis();
        }

        public void renew() {
            startTime = System.currentTimeMillis();
        }

        public void expire() {
            startTime = Long.MIN_VALUE;
        }

        @Override
        public int compareTo(Delayed that) {
            return Long.compare(this.getDelayMillis(), that.getDelay(TimeUnit.MILLISECONDS));
        }
    }

}