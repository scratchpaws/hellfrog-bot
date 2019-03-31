package besus.utils.func;

import besus.utils.MiscUtils;
import besus.utils.Ref;
import org.apache.commons.lang3.StringUtils;
import rx.functions.*;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static besus.utils.PredicateEx.isNull;


/**
 * Created by besus on 27.05.17.
 */
public interface Func {


    interface AnySupplier<T> extends Supplier<T>, Func0<T>{

        // convert to func that ignores incoming arg
        default <A> AnyFunc<A, T> onAny() {
            return some -> get();
        }

        @Override
        default T call() {
            return get();
        };

        default <R> AnySupplier<R> then(AnyFunc <? super T, R> after) {
            return after.on(this);
        }

        default <R, A1> AnySupplier<R> then(Func2<? super T, A1, ? extends R> after, A1 arg1) {
            return then(some -> after.call(some, arg1));
        }
    }

    static <S, T> AnySupplier<T> statefulSupplier(S state, AnyFunc<Ref<S>, T> next) {
        return new AnySupplier<T>() {
            Ref<S> s = new Ref<>(state);

            @Override
            public T get() {
                return next.apply(s);
            }
        };
    }

    interface AnyAction<T> extends Action1<T>, Consumer<T>/*, Handler<T>*/ {
        @Override
        default void accept(T t) {
            invoke(t);
        };

        @Override
        default void call(T t) {
            invoke(t);
        };

//        @Override
//        default void handle(T t) {
//            invoke(t);
//        }

        void invoke(T t);

        default AnyAction<T> then(AnyAction<T> after) {
            return r -> {
                invoke(r);
                after.invoke(r);
            };
        }

    }

    interface AnyFunc<T, R> extends Func1<T, R>, Function<T, R> {

        @Override
        default R apply(T t) {
            return invoke(t);
        }

        @Override
        default <V> AnyFunc<V, R> compose(Function<? super V, ? extends T> before) {
            return Function.super.compose(before)::apply;
        }

        default <V> AnyFunc<T, V> then(Function<? super R, ? extends V> after) {
            return Function.super.andThen(after)::apply;
        }

        default <V> Action1<T> applyOn(Action1<T> after) {
            return Function.super.andThen(Functions.fromAction(after)::call)::apply;
        }

        default <V, A1> AnyFunc<T, V> then(Func2<? super R, A1, ? extends V> after, A1 arg1) {
            return then(some -> after.call(some, arg1))::apply;
        }

        default <V, A1, A2> AnyFunc<T, V> then(Func3<? super R, A1, A2, ? extends V> after, A1 arg1, A2 arg2) {
            return then(some -> after.call(some, arg1, arg2))::apply;
        }

        default AnyFunc<T, R> orElse(AnyFunc<T, R> ifNullFunc) {
            return orIf(isNull::invoke, ifNullFunc);
        }

        @Override
        default R call(T t){
            return invoke(t);
        }

        default Action1<T> asAction() {
            return this::invoke;
        }

        default AnyFunc<T, R> orIf(Predicate<R> condition, AnyFunc<T, R> onCondition) {
            return (T t) -> {
                R res = invoke(t);
                return condition.test(res) ? onCondition.invoke(t): res;
            };
        }

        // convert to supplier by invoking this with given value
        default AnySupplier<R> on(T value) {
            return () -> invoke(value);
        }

        // convert to supplier by invoking this with supplied value
        default AnySupplier<R> on(AnySupplier<? extends T> valueGen) {
            return () -> invoke(valueGen.get());
        }

        R invoke(T t);
    }

    static <R> AnyAction<R> conditional(AnyFunc<? super R, Boolean> condition, Action1<? super R> action) {
        return arg -> {
            if (condition.apply(arg)) {
                action.call(arg);
            }
        };
    }

    static <R> AnyAction<R> conditional(Boolean condition, Action1<? super R> action) {
        return arg -> {
            if (condition) {
                action.call(arg);
            }
        };
    }

    @SafeVarargs
    static <R> AnyAction<R> compose(AnyAction<R>...actions) {
        return Arrays.stream(actions).reduce(AnyAction::then).orElse(r -> {});
    }

    static <R> AnyFunc<?, R> NULL() {
        return any -> null;
    }

    static <T, R> AnyFunc<T, R> exact(R r) {
        return any -> r;
    }

    static <S> AnyFunc<S, S> self(){
        return t -> t;
    }

    static <T, R> AnyFunc<T, R> do̱(Func1<T, R> ref) {
        return ref::call;
    }

    static <T, R, A1> AnyFunc<T, R> do̱(Func2<T, A1, R> f2, A1 arg1){
        return t -> f2.call(t, arg1);
    }
    static <T, R, A1, A2> AnyFunc<T, R> do̱(Func3<T, A1, A2, R> f3, A1 arg1, A2 arg2){
        return t -> f3.call(t, arg1, arg2);
    }

    AnyFunc<Object, String> asStr = String::valueOf;

    static AnyFunc<String, String> first(int n) {
        return Func.<String>self().then(s -> StringUtils.substring(s, 0, n));
    }
    AnyFunc<Object, Boolean> asBool = asStr.then(Boolean::valueOf);
    AnyFunc<Object, Integer> asInt = self().then(MiscUtils::anyToNum).then(Number::intValue);
    AnyFunc<Object, Long> asLong = self().then(MiscUtils::anyToNum).then(Number::longValue);
    AnyFunc<Object, Double> asDouble = self().then(MiscUtils::anyToNum).then(Number::doubleValue);
//    AnyFunc<Object, JsonExtend> asJson = self().then(JsonExtend::extend);
//    AnyFunc<Object, Integer> asJsCount = self().then(cast(JsonArray.class, JsonArray::size, cast(JsonObject.class, JsonObject::size, notJson -> -1)));

    static <C> AnyFunc<Object, C> cast(Class<C> clazz) {
        return clazz::cast;
    }

    static <C> AnyFunc<Object, C> cast(Class<C> clazz, AnyFunc<Object, C> orAnother) {
        return select(clazz::isInstance, clazz::cast, orAnother);
    }

    static <T, C> AnyFunc<T, C> select(Predicate<T> cond, AnyFunc<T, C> ftrue, AnyFunc<T, C> ffalse) {
        return some -> cond.test(some) ? ftrue.apply(some) : ffalse.apply(some);
    }

    static <T, C> AnyFunc<T, C> mapNull(AnySupplier<C> ifnull, AnyFunc<T, C> ifnot) {
        return some -> some == null ? ifnull.call(): ifnot.apply(some);
    }

    static <T, C> AnyFunc<T, C> mapNull(C ifnull, C ifnot) {
        return some -> some == null ? ifnull : ifnot;
    }

    @SuppressWarnings("unchecked")
    static <T, C, I> AnyFunc<T, C> cast(Class<I> clazz, AnyFunc<? super I, C> forInstance, AnyFunc<? super T, C> orAnother) {
        return some -> clazz.isInstance(some) ? forInstance.apply((I) some) : orAnother.apply(some);
    }

    /**
     * adapter for "map" functions and not builder-style(void) setters or any other side actions(logging?) as
     *  .map(with(o -> o.setId(42))).map(with(o -> o.setSome("some")))
     */
    @SafeVarargs
    static <T> AnyFunc<T, T> with(Action1<T>...performActions) {
        return t ->  {
            Arrays.stream(performActions).forEach(a -> a.call(t));
            return t;
        };
    }

}
