package hellfrog.settings.db.h2;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class BooleanToLongConverter implements AttributeConverter<Boolean, Long> {

    @Override
    public Long convertToDatabaseColumn(Boolean attribute) {
        if (attribute == null) {
            return null;
        }
        if (Boolean.TRUE.equals(attribute)) {
            return 1L;
        } else {
            return 0L;
        }
    }

    @Override
    public Boolean convertToEntityAttribute(Long dbData) {
        if (dbData == null) {
            return null;
        }
        return dbData > 0L ? Boolean.TRUE : Boolean.FALSE;
    }
}
