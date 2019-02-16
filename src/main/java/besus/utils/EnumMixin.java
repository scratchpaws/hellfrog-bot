package besus.utils;

import java.util.Arrays;

public interface EnumMixin<T extends EnumMixin<T>> {

    T[] vals();

    int id();

    @SuppressWarnings("unchecked")
    default T defaultOrFromId(int id) {
        return Arrays.stream(vals()).filter(state -> state.id() == id).findFirst().orElse((T) this);
    }

    @SuppressWarnings("unchecked")
    default T defaultOrFrom(String value) {
        return Arrays.stream(vals()).filter(state -> state.toString().equals(value)).findFirst().orElse((T) this);
    }
}