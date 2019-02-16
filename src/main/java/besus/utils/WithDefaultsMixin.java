package besus.utils;

/**
 * Created by besus on 27.05.17.
 */
public interface WithDefaultsMixin<D> {
    default D defaultValue() {
        return null;
    }

    default D failValue() {
        return null;
    }

}
