package besus.utils.collection;

import besus.utils.func.Func;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class LazyMap<K, V> implements Map<K, V> {
   private Map<K, Func.AnySupplier<V>> delegate = new HashMap<>();

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return values().contains(value);
    }

    @Override
    public V get(Object key) {
        Func.AnySupplier<V> supplier = delegate.get(key);
        return supplier != null ? supplier.call() : null;
    }


    public void putLazy(K key, Func.AnySupplier<V> value) {
        delegate.put(key, value);
    }

    @Override
    public V put(K key, V value) {
        delegate.put(key, () -> value);
        return null;
    }

    @Override
    public V remove(Object key) {
        delegate.remove(key);
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<V> values() {
        return delegate.values().stream().map(Func.AnySupplier::call).collect(Collectors.toList());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return delegate.entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue().call())).collect(Collectors.toSet());
    }


}
