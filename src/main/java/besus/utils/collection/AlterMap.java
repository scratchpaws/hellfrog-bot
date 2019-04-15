package besus.utils.collection;

import besus.utils.PredicateEx;
import besus.utils.func.Mapper;

import java.util.*;

/**
 * // todo: extract Alternatives interface
 * Map, returning alternative existant values for sequence of [default] keys if requested value is null;
 * Example
 * map : (1, "one"), (2, "two"), (4, "four")
 * get(1): "one"
 * get(3): null
 * get(5, 4): "four"
 * get(5, 6, 7): null
 * get(5, 6, 7, 2, 10): "two"
 * get(5, 6, 7, 2, 1): "two"
 * Created by besus on 12.09.17.
 */
public class AlterMap<K, V> implements Map<K, V>, Mapper<K, V> {
    private final Map<K, V> source;
    private List<K> alterKeys = new LinkedList<K>();
    private Mapper<? super Map<K, V>, V> alterMapper = m -> null;

    public AlterMap(Map<K, V> source, Mapper<? super Map<K, V>, V> alterMapper) {
        this.source = source;
        this.alterKeys = alterKeys;
        this.alterMapper = alterMapper;
    }

    public AlterMap(Map<K, V> source, Mapper<? super Map<K, V>, V> alterMapper, K... alterKeysDefault) {
        this.source = source;
        this.alterKeys.addAll(Arrays.asList(alterKeysDefault));
        this.alterMapper = alterMapper;
    }

    public AlterMap(Mapper<K, V> source, Mapper<? super Map<K, V>, V> alterMapper, K... alterKeysDefault) {
        this(source.asMap(), alterMapper, alterKeysDefault);
    }

    public AlterMap(Map<K, V> source) {
        this.source = source;
    }

    @SafeVarargs
    public AlterMap(Map<K, V> source, K... alterKeysDefault) {
        this.source = source;
        this.alterKeys.addAll(Arrays.asList(alterKeysDefault));
    }

    public static <M extends Map<K, V>, K, V> Mapper<M, V> getAny() {
        return m -> m.values().iterator().next();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        source.putAll(m);
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
    public Set<K> keySet() {
        return source.keySet();
    }

    @Override
    public void clear() {
        source.clear();
    }

    @Override
    public V get(Object key) {
        return this.getAny((K) key);
    }

    public V getOrNew(K key) {
        return source.computeIfAbsent(key, k -> alterMapper.invoke(this));
    }


    @SafeVarargs
    public final V getAny(K key, K... alters) {
        return Sequental.of(key).with(alters).with(alterKeys).stream()
                .map(source::get)
                .filter(PredicateEx.not(PredicateEx.isNull))
                .findFirst()
                .orElseGet(() -> alterMapper.invoke(this));
    }

    @Override
    public V put(K key, V value) {
        return source.put(key, value);
    }


    @Override
    public V remove(Object key) {
        return source.remove(key);
    }

    @Override
    public Collection<V> values() {
        return source.values();
    }

    @Override
    public boolean isEmpty() {
        return source.isEmpty();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return source.entrySet();
    }

    @Override
    public int size() {
        return source.size();
    }
}
