package besus.utils.func;

import besus.utils.collection.Sequental;


import java.util.*;

import static besus.utils.MiscUtils.coalesce;
import static besus.utils.func.Func.cast;


/**
 * Alias for using in class's implement clause
 * Any instances of Mapper instances can be used directly in Stream.map(), Observable.map() etc.
 * @param <T> Type of value to map
 * @param <R> Type of mapping result
 */
public interface Mapper<T, R> extends Func.AnyFunc<T, R> {
    @Override
    default R invoke(T t) {
        return get(t);
    }

    R get(T from);

    default R get(T from, R defaulṯ) {
        return coalesce(get(from), defaulṯ);
    }

    default <N> N get(T from, Func.AnyFunc<R, N> andPerform) {
        return andPerform.apply(get(from));
    }

    default <A> A getChained(Object... keys) {
        return getChained(Sequental.of(keys).iterator());
    }

    @SuppressWarnings("unchecked")
    default <A> A getChained(Iterator keyPointer) {
        if (! keyPointer.hasNext()) {
            return (A) this;
        }
        R nextVal = get((T) keyPointer.next());
        if (keyPointer.hasNext() && nextVal instanceof Mapper) {
            return  ((Mapper<?, A>) nextVal).getChained(keyPointer);
        } else {
            return (A) nextVal;
        }
    }


    default Mapper<T, R> with(Mapper<T, R> ifNullFunc) {
        return Func.AnyFunc.super.orElse(ifNullFunc)::apply;
    }

    default Mapper<T, R> with(T k, R v) {
        return with(mapper(k, v));
    }

//    static <T> Mapper<String, T> mapper(JsonObject o) {
//        return key -> (T) o.getValue(key);
//    }

    static <K, V> Mapper<K, V> mapper(Map<K, V> m) {
        return m::get;
    }

    static <K, V> Mapper<K, V> mapper(Mapper<K, V> src) {
        return src;
    }

    static <K, V> Mapper<K, V> mapper(K k, V v) {
        return some -> Objects.equals(some, k) ? v : null;
    }



    /**
     *
     * @return readonly map to able to use mappers as maps in any api
     */
    @SuppressWarnings("unchecked")
    default Map<T, R> asMap() {
        return new Map<T, R>() {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean containsKey(Object key) {
                return get(key) != null;
            }

            @Override
            public boolean containsValue(Object value) {
                return false;
            }

            @Override
            public R get(Object key) {
                return  Mapper.this.get((T) key);
            }

            @Override
            public R put(T key, R value) {
                return null;
            }

            @Override
            public R remove(Object key) {
                return null;
            }

            @Override
            public void putAll(Map<? extends T, ? extends R> m) {
            }

            @Override
            public void clear() {
            }

            @Override
            public Set<T> keySet() {
                return Collections.EMPTY_SET;
            }

            @Override
            public Collection<R> values() {
                return Collections.EMPTY_SET;
            }

            @Override
            public Set<Entry<T, R>> entrySet() {
                return Collections.EMPTY_SET;
            }
        };
    }

    static void main(String[] args) {
        Mapper<Integer, String> nums =
                mapper(1, "one")
                .with(2, "two")
                .with(3, "three");
        System.err.println(nums.get(1));
        System.err.println(nums.get(3));
        System.err.println(nums.get(2));
        System.err.println(nums.get(4));
        System.err.println(nums.get(4, "4 undefined"));
        Map<Integer, String> numsMap = nums.asMap();
        System.err.println(numsMap.get(1));
        System.err.println(numsMap.get(3));
        System.err.println(numsMap.get(2));
        System.err.println(numsMap.get(4));
        System.err.println(numsMap.getOrDefault(4, "4 undefined"));

    }

}

