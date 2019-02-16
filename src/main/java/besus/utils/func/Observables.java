package besus.utils.func;


import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;


/**
 * Created by besus on 17.10.17.
 */
public interface Observables {
    /**
     * adapters for using Observable::flatMap in case "and then", i.e. "execute observable and continue chain after it done, omitting results"
     *
     * @param chainLink Observable to insert into execution chain
     */
    static <T> Func.AnyFunc<T, Observable<T>> chained(Func1<T, Observable<?>> chainLink) {
        return t -> chainLink.call(t).lastOrDefault(null).map(omitted -> t);
    }

    static <T, A1> Func.AnyFunc<T, Observable<T>> chained(Func2<T, A1, Observable<?>> chainLink, A1 a1) {
        return t -> chainLink.call(t, a1).lastOrDefault(null).map(omitted -> t);
    }

    static <T, A1, A2> Func.AnyFunc<T, Observable<T>> chained(Func3<T, A1, A2, Observable<?>> chainLink, A1 a1, A2 a2) {
        return t -> chainLink.call(t, a1, a2).lastOrDefault(null).map(omitted -> t);
    }

    static <T> Func.AnyFunc<T, Observable<T>> chained(Observable<?> chainLink) {
        return t -> chainLink.lastOrDefault(null).map(dummy -> t);
    }

//    static <T> Observable.Transformer<T, T> shuffle(Func.AnyFunc<T, Double> valueFunc, int maxCount) {
//        return  o -> o.toList()
//                .map(l -> new RandomSelector<>(l, valueFunc))
//                .flatMapIterable(rs -> rs.getShuffle(maxCount));
//    }
//
//
//    static <T> Observable.Transformer<T, T> windowCycled(int skip, int maxCount) {
//        return o -> o.toList()
//                .flatMapIterable(list -> () -> new CycledList<>(list).subsequence(skip, maxCount));
//    }
//
//    static <T> Observable.Transformer<T, T> windowCycledShuffle(int skip, int maxCount) {
//        return o -> o.toList()
//                .flatMapIterable(list -> new RandomSelector<>(() -> new CycledList<>(list).subsequence(skip, maxCount), exact(1D)).getShuffle(maxCount));
//    }
//
//    static <T> Observable.Transformer<T, T> ignoreErrors() {
//        return o -> o.flatMap(Observable::just, exact(Observable.empty()), Observable::empty);
//    }

}
