package hellfrog.settings.db;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.LongType;

import java.lang.reflect.Field;
import java.time.Instant;

/**
 * SQLite-specific Instant persister for ORMlite, using long type preferred by TimeStamp.
 * Based on https://github.com/graynk/ormlite-core/blob/jsr310-persister-pr/src/main/java/com/j256/ormlite/field/types/InstantType.java
 */
public class InstantPersister extends LongType {

    private static final InstantPersister INSTANCE = new InstantPersister();
    private static final Instant NULL_VALUE = Instant.parse("1970-01-01T00:00:00Z");

    private InstantPersister() {
        super(SqlType.LONG, new Class<?>[] { Instant.class });
    }
    protected InstantPersister(SqlType sqlType, Class<?>[] classes) {
        super(sqlType, classes);
    }

    public static InstantPersister getSingleton() {
        return INSTANCE;
    }

    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
        Long value = (Long) sqlArg;
        if (value == null || value == 0L) {
            return null;
        }
        return Instant.ofEpochMilli(value);
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
        Instant value = (Instant) javaObject;
        if (value == null) {
            return null;
        }
        return value.toEpochMilli();
    }

    @Override
    public Object moveToNextValue(Object currentValue) {
        Instant value = (Instant) currentValue;
        return value.plusMillis(1L);
    }

    @Override
    public boolean isValidForField(Field field) {
        return (field.getType() == Instant.class);
    }
}
