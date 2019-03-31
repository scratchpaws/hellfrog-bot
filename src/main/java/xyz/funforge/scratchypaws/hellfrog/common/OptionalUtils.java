package xyz.funforge.scratchypaws.hellfrog.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Бекпорт некоторых методов из JDK9+ для Optional
 */
public class OptionalUtils {

    public static <T> void ifPresentOrElse(@NotNull Optional<T> optional,
                                           @NotNull Consumer<? super T> ifPresent,
                                           @NotNull Runnable ifNotPresent) {

        if (optional.isPresent()) {
            T value = optional.get();
            ifPresent.accept(value);
        } else {
            ifNotPresent.run();
        }
    }
}
