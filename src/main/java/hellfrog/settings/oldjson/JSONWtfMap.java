package hellfrog.settings.oldjson;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class JSONWtfMap {

    private Map<Long, String> nameValues = Collections.emptyMap();
    private AtomicLong lastName = new AtomicLong(0L);

    public Map<Long, String> getNameValues() {
        return nameValues;
    }

    public void setNameValues(Map<Long, String> nameValues) {
        this.nameValues = nameValues != null ? Collections.unmodifiableMap(nameValues) : this.nameValues;
    }

    public AtomicLong getLastName() {
        return lastName;
    }

    public void setLastName(AtomicLong lastName) {
        this.lastName = lastName != null ? lastName : this.lastName;
    }
}
