package besus.utils.collection;

import besus.utils.func.Func;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by besus on 14.08.17.
 * Readonly wrapper for accesing stationary "view"(similar to same sql term) for any map.
 */
public class MapWrapper<K, I, V> implements Map<K, V> {
    private final Map<K, I> source;
    private Func.AnyFunc<I, V> convertor;

    public MapWrapper(Map<K, I> source, Func.AnyFunc<I, V> convertor) {
        this.source = source;
        this.convertor = convertor;
    }


    @Override
    public int size() {
        return source.size();
    }

    @Override
    public boolean isEmpty() {
        return source.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return source.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return source.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return convertor.apply(source.get(key));
    }

    @Override
    public V put(K key, V value) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public V remove(Object key) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void clear() {
        source.clear();
    }

    @Override
    public Set<K> keySet() {
        return source.keySet();
    }

    @Override
    public Collection<V> values() {
        return source.values().stream().map(convertor).collect(Collectors.toList());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return source.entrySet().stream().map(e -> new Entry<K, V>() {
            @Override
            public K getKey() {
                return e.getKey();
            }
            @Override
            public V getValue() {
                return convertor.apply(e.getValue());
            }
            @Override
            public V setValue(Object value) {
                return null;
            }
        }).collect(Collectors.toSet());
    }

    @Override
    public boolean equals(Object o) {
        return source.equals(o);
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }
}
