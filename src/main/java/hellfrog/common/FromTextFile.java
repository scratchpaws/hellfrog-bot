package hellfrog.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Указывает на ресурс в виде текстового файла, упакованного в jar,
 * который необходимо загрузить и вставить в переменную.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.METHOD})
public @interface FromTextFile {

    String fileName();
}
