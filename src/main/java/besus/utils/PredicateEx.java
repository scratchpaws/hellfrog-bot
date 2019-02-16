package besus.utils;

import besus.utils.func.Func;
import besus.utils.collection.Sequental;
import org.apache.commons.lang3.StringUtils;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.functions.Func1;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import static besus.utils.func.Func.asStr;

/**
 * Created by besus on 27.05.17.
 */
@SuppressWarnings("unchecked")
public interface PredicateEx<THIS extends PredicateEx<THIS, OBJ>, OBJ> extends Predicate<OBJ>, Func.AnyFunc<OBJ, Boolean>, WithDefaultsMixin<Boolean>  {

    // need to create subclass instance from lambda;
    THIS of(Predicate<OBJ> func);

    // need to rx.Func1 compatibility
    @Override
    default Boolean call(OBJ obj) {
        return test(obj);
    }

    // need to Function compatibility
    @Override
    default Boolean apply(OBJ obj) {
        return test(obj);
    }


    @Override
    default THIS and(Predicate<? super OBJ> other) {
        return of(Predicate.super.and(other));
    }

    @Override
    default THIS negate() {
        return of(Predicate.super.negate());
    }

    @Override
    default THIS or(Predicate<? super OBJ> other) {
        return of(Predicate.super.or(other));
    }

    default Boolean testEx(OBJ obj, Action0 onTrue, Action0 onFalse, Action1<Exception> onError) {
        try {
            boolean result = test(obj);
            if (result) {
                onTrue.call();
            } else {
                onFalse.call();
            }
            return result;
        } catch (Exception e) {
            onError.call(e);
            return failValue();
        }
    }

    default Boolean testEx(OBJ obj, Action0 onTrue, Action1<Exception> onError) {
        return testEx(obj, onTrue, Actions.empty(), onError);
    }

    default Boolean testEx(OBJ obj, Action1<Exception> onError) {
        return testEx(obj, Actions.empty(), Actions.empty(), onError);
    }

    default <T> Optional<T> get(OBJ obj, Func1<OBJ, T> ftrue) {
        return select(obj, ftrue, n -> null);
    }

    default <T> Optional<T> orElse(OBJ obj, Func1<OBJ, T> ffalse) {
        return select(obj, n -> null, ffalse);
    }

    default <T> Optional<T> select(OBJ obj, Func1<OBJ, T> ftrue, Func1<OBJ, T> ffalse) {
        return test(obj) ? Optional.ofNullable(ftrue.call(obj)) : Optional.ofNullable(ffalse.call(obj));
    }

    // compiler fuckup
    static <THIS extends PredicateEx<THIS, OBJ>, OBJ> PredicateEx<THIS, OBJ> of(PredicateEx<THIS, OBJ> src) {
        return src;
    }

    @Override
    default Boolean invoke(OBJ obj) {
        return test(obj);
    };

    interface SimplePredicateEx<T> extends PredicateEx<SimplePredicateEx<T>, T> {
        @Override
        default SimplePredicateEx<T> of(Predicate<T> func) {
            return func::test;
        }
    }

    SimplePredicateEx<Number> isZero = number -> number.longValue() == 0;
    SimplePredicateEx<? super Object> isNull = Objects::isNull;
    SimplePredicateEx<CharSequence> ifStrEmpty = StringUtils::isEmpty;
    SimplePredicateEx<Iterable> ifEmpty = iterable -> !iterable.iterator().hasNext();
    static <T> SimplePredicateEx<Iterable> contains(T val) {
        return data -> StreamSupport.stream(data.spliterator(), false).anyMatch(val::equals);
    }

    static <T extends String> SimplePredicateEx<T> startsWith(T prefix) {
        return s -> s.startsWith(prefix);
    }

    static <T> SimplePredicateEx<T> check(PredicateEx what) {
        return what::test;
    }

    static <T> SimplePredicateEx<T> in(T...variants) {
        return v -> Sequental.of(variants).stream().anyMatch(eq(v));
    }

    static <T> SimplePredicateEx<T> eq(T to) {
        return v -> Objects.equals(v, to);
    }

    static <T> SimplePredicateEx<T> not(Func1<T, Boolean> predicate) {
        return t -> !predicate.call(t);
    }

    static SimplePredicateEx<Object> ifInstance(Class<?> clazz) {
        return obj -> obj != null && obj.getClass().isAssignableFrom(clazz);
    }
}
