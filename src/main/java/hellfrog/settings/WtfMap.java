package hellfrog.settings;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Deprecated
public class WtfMap {

    private ConcurrentHashMap<Long, String> nameValues = new ConcurrentHashMap<>();
    private AtomicLong lastName = new AtomicLong(0L);

    @Deprecated
    public ConcurrentHashMap<Long, String> getNameValues() {
        return nameValues;
    }

    @Deprecated
    public void setNameValues(ConcurrentHashMap<Long, String> nameValues) {
        this.nameValues = nameValues;
    }

    @Deprecated
    public AtomicLong getLastName() {
        return lastName;
    }

    @Deprecated
    public void setLastName(AtomicLong lastName) {
        this.lastName = lastName;
    }
}
