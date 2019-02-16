package besus.utils.collection;

/**
 * Created by besus on 22.04.17.
 */
public class Chain <T extends Chain<T>> {

    protected T next;

    public Chain<T> append(T what) {
        if (next == null) {
            next = what;
        } else {
            next.append(what);
        }
        return this;
    }
}
