package hellfrog.common;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class Tuple<K, V> {

    public final K left;
    public final V right;

    public Tuple(K left, V right) {
        this.left = left;
        this.right = right;
    }

    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    public static<K,V> Tuple<K,V> of(K left, V right) {
        return new Tuple<>(left, right);
    }

    @Override
    public String toString() {
        return "[" + left + " - " + right + "]";
    }
}
