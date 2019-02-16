package besus.utils;

import besus.utils.func.Func;
//import io.vertx.core.json.JsonObject;
import rx.functions.Func1;
import rx.functions.Func2;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by besus on 21.03.17.
 */
public class RandomSelector<T> {
    private final List<Pair<T, Number>> items = new ArrayList<>();
    private final Random rand = new Random();
    private double totalSum = 0;

    @Override
    public String toString() {
        return "Random of: " + items.stream()
                .map(i -> "[" + i.item + ", " + ((int)(i.value.doubleValue() *100 / totalSum)) + "%]")
                .collect(Collectors.joining("; "));
    }

    private class Pair<I extends T, V extends Number> {
        final I item;
        final V value;

        Pair(I item, V value) {
            this.item = item;
            this.value = value;
        }
    }

//    public static class JsonSelector extends RandomSelector<JsonObject> {
//        public JsonSelector(List<JsonObject> items, String valueParam) {
//            super(items, obj -> obj.getDouble(valueParam));
//        }
//        public JsonSelector(List<JsonObject> items) {
//            this(items, "value");
//        }
//        public JsonSelector(){
//            super();
//        }
//    }

    public <A> RandomSelector<T> addAll(A source, Func.AnyFunc<A, Iterable<T>> itemExtractor, Func2<A, T, Double> valueExtractor) {
        for(T item : itemExtractor.apply(source)) {
            add(item, valueExtractor.call(source, item));
        }
        return this;
    }

    public RandomSelector<T> add(T item, Func1<T, Double> valueFunc) {
        return this.add(item, valueFunc.call(item));
    }

    public RandomSelector(Iterable<T> items, Func1<T, Double> valueFunc) {
        items.forEach(item -> add(item, valueFunc));
    }

    public RandomSelector() {}

    public RandomSelector<T> add(T item, Number value) {
        Pair<T, Number> pair = new Pair<>(item, value);
        items.add(pair);
        totalSum += pair.value.doubleValue();
        return this;
    }

    public Stream<T> getItems() {
        return items.stream().map(item -> item.item);
    }

    public Integer count() {
        return items.size();
    }

    public T getRandom() {
        if (totalSum == 0) {
            if (items.isEmpty()) {
                return null;
            }
            return items.get(0).item;
        }
        double index = totalSum * rand.nextDouble();
        double sum = 0;
        int i = 0;
        while(sum <= index) {
            sum += items.get(i++).value.doubleValue();
        }
        return items.get(Math.max(0, i-1)).item;
    }

    public T getRandom(T defaultOnEmpty) {
        if (count() == 0) {
            return defaultOnEmpty;
        }
        return getRandom();
    }

    public Collection<T> getShuffle(int maxCount) {
        Set<T> sret = new HashSet<>();
        Set<T> all = items.stream().map(v -> v.item).collect(Collectors.toSet());
        while(sret.size() < all.size() && sret.size() < maxCount) {
            sret.add(getRandom());
        }
        List<T> ret = new ArrayList<>(sret);
        Collections.shuffle(ret);
        return ret;
    }
}