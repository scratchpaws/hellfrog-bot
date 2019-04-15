package besus.utils.collection;

import rx.Observable;
import rx.functions.Func0;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static besus.utils.MiscUtils.coalesce;
import static besus.utils.MiscUtils.coalesceBy;

/**
 * Helps concatenate sequences of single values, arrays, iterables and collections and using them as single iterable sequence, stream or observable.
 * Created by besus on 12.09.17.
 */
@SuppressWarnings("unchecked")
public class Sequental<T> implements Iterable<T> {
    private static final VoidIterator voiḏ = new VoidIterator<>();
    private Iterator<T> its = voiḏ;

    public static <S> Sequental<S> of(S single) {
        return new Sequental<S>().with(single);
    }

    public static <S, I extends Iterator<S>> Sequental<S> of(I iterator) {
        return new Sequental<S>().with(iterator);
    }

    public static <S, I extends Iterable<S>> Sequental<S> of(I iterable) {
        return new Sequental<S>().with(iterable);
    }

    public static <S> Sequental<S> all(S... values) {
        return new Sequental<S>().with(values);
    }

    public static <S> Sequental<S> of(S[] array) {
        return new Sequental<S>().with(array);
    }

    public static <T> Sequental<T> infinite(Func0<T> generator) {
        return of(new InfiniteGeneratorIterator<>(generator));
    }

    public Sequental<T> with(Iterator<T> iterator) {
        its = new JoinIterator<T>(its, iterator);
        return this;
    }

    public Sequental<T> with(T value) {
        return with(new SingleIterator<>(value));
    }

    public Sequental<T> with(Iterable<T> iterable) {
        return with(iterable.iterator());
    }

    public Sequental<T> with(T[] array) {
        return with(Spliterators.iterator(Arrays.spliterator(array)));
    }

    public Stream<T> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(its, Spliterator.ORDERED), false);
    }

    public rx.Observable<T> observable() {
        return Observable.from(this);
    }

    public Sequental<T> repeatable() {
        return new RepeatableSequental<>(stream().collect(Collectors.toList()));
    }

    @Override
    public Iterator<T> iterator() {
        return its;
    }

    public static class VoidIterator<T> implements Iterator<T> {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public T next() {
            return null;
        }
    }

    public static class SingleIterator<T> implements Iterator<T> {
        private T val;

        SingleIterator(T val) {
            this.val = val;
        }

        @Override
        public boolean hasNext() {
            return val != null;
        }

        @Override
        public T next() {
            T ret = val;
            val = null;
            return ret;
        }
    }

    public static class JoinIterator<T> implements Iterator<T> {
        private final Iterator<T> is[];

        JoinIterator(Iterator<T>... iterators) {
            this.is = iterators;
        }

        @Override
        public boolean hasNext() {
            return coalesce(coalesceBy(Iterator::hasNext, is), voiḏ).hasNext();
        }

        @Override
        public T next() {
            return coalesce(coalesceBy(Iterator::hasNext, is), (Iterator<T>) voiḏ).next();
        }
    }

    public static class InfiniteGeneratorIterator<T> implements Iterator<T> {
        private final Func0<T> gen;

        InfiniteGeneratorIterator(Func0<T> generator) {
            this.gen = generator;
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public T next() {
            return gen.call();
        }
    }

    private static class RepeatableSequental<T> extends Sequental<T> {
        private final List<T> computed;

        RepeatableSequental(List<T> computed) {
            this.computed = computed;
        }

        @Override
        public Sequental<T> with(Iterator<T> iterator) {
            return of(computed).with(iterator).repeatable();
        }

        @Override
        public Stream stream() {
            return of(computed).stream();
        }

        @Override
        public Observable observable() {
            return of(computed).observable();
        }

        @Override
        public Iterator iterator() {
            return of(computed).iterator();
        }
    }
}
