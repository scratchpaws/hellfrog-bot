package hellfrog.commands.scenes;

import hellfrog.core.SessionState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

public class ScenarioState {

    private final long stepId;
    private static final Logger log = LogManager.getLogger(SessionState.class.getSimpleName());
    private final ConcurrentHashMap<String, Object> objectsMap = new ConcurrentHashMap<>();

    public ScenarioState(long stepId) {
        this.stepId = stepId;
    }

    public void put(String key, Object value) {
        objectsMap.put(key, value);
    }

    public <T> T get(String key, Class<T> type) {
        Object obj = objectsMap.get(key);
        if (obj == null) return null;
        if (type.isInstance(obj)) {
            try {
                return type.cast(obj);
            } catch (ClassCastException err) {
                log.error("Unable cast object with key " + key + " to " + type.getName()
                        + ", step id: " + stepId, err);
                return null;
            }
        } else {
            return null;
        }
    }
}
