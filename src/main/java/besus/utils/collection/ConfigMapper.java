package besus.utils.collection;

import besus.utils.func.Mapper;

import java.util.Objects;

import static besus.utils.MiscUtils.coalesce;

/**
 * Created by besus on 17.10.17.
 */
public class ConfigMapper<V> implements Mapper<String, V> {

    private ConfigMapper(Mapper<String, V> base) {
        chain = base::apply;
    }

    Mapper<String, V> chain = k -> null;
    public ConfigMapper append(String k, V v) {
        Mapper<String, V> prev = chain;
        chain = s -> Objects.equals(s, k) ? v : prev.get(k);
        return this;
    }

    public ConfigMapper append(Mapper<String, V> newm) {
        Mapper<String, V> prev = chain;
        chain = k -> coalesce(newm.get(k), prev.get(k));
        return this;
    }

    @Override
    public V get(String key) {
        return chain.getChained((Object[]) key.split("/"));
    }
}
