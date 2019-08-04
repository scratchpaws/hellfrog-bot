package hellfrog.settings;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class WtfMap {

    private ConcurrentHashMap<Long, String> nameValues = new ConcurrentHashMap<>();
    private AtomicLong lastName = new AtomicLong(0L);

    public ConcurrentHashMap<Long, String> getNameValues() {
        return nameValues;
    }

    public void setNameValues(ConcurrentHashMap<Long, String> nameValues) {
        this.nameValues = nameValues;
    }

    public AtomicLong getLastName() {
        return lastName;
    }

    public void setLastName(AtomicLong lastName) {
        this.lastName = lastName;
    }
}
