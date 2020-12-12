package hellfrog.common;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Function with three parameters that returns the result
 *
 * @param <T1> first parameter type
 * @param <T2> second parameter type
 * @param <T3> third parameter type
 * @param <R> return type
 */
@FunctionalInterface
public interface TriFunction<T1, T2, T3, R> {

    R apply(T1 t1, T2 t2, T3 t3);

    // Not needed yet, but took it by analogy with BiFunction
    default <V> TriFunction<T1, T2, T3, V> andThen(@NotNull final Function<? super R, ? extends V> after) {
        return (T1 t1, T2 t2, T3 t3) -> after.apply(apply(t1, t2, t3));
    }
}
