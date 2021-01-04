package hellfrog.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Выполняет загрузку текстовых ресурсов из файла, когда в самом коде вставлять их
 * в текстовом виде не имеет смысла (например, многострочный текст, со спецсимволами и т.д.)
 */
public class ResourcesLoader {

    private static final Logger log = LogManager.getLogger("Resources loader");

    public static <T> void initFileResources(@NotNull Object object,
                                             @NotNull Class<? extends T> type) {

        if (type.isInstance(object)) {
            T obj = type.cast(object);
            boolean annotationIs = false;
            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(FromTextFile.class)) {
                    FromTextFile stringResource = method.getAnnotation(FromTextFile.class);
                    String fileName = stringResource.fileName();
                    if (CommonUtils.isTrStringNotEmpty(fileName)) {
                        String loadedResource = fromTextFile(fileName);
                        try {
                            method.invoke(obj, loadedResource);
                        } catch (Exception err) {
                            String errMsg = String.format("Unable to invoke annotated method \"%s::%s\": %s",
                                    type.getName(), method.getName(), err.getMessage());
                            log.fatal(errMsg, err);
                            throw new RuntimeException(errMsg, err);
                        }
                    }
                    if (!annotationIs) {
                        annotationIs = true;
                    }
                }
            }
            for (Field field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(FromTextFile.class)) {
                    FromTextFile stringResource = field.getAnnotation(FromTextFile.class);
                    String fileName = stringResource.fileName();
                    if (CommonUtils.isTrStringNotEmpty(fileName)) {
                        String loadedResource = fromTextFile(fileName);
                        try {
                            try {
                                String fieldName = field.getName();
                                String methodName = "set" + Character.toUpperCase(fieldName.charAt(0))
                                        + fieldName.substring(1);
                                Method mayBeMethod = type.getDeclaredMethod(methodName, String.class);
                                mayBeMethod.invoke(obj, loadedResource);
                            } catch (NoSuchMethodException ignore) {
                                field.setAccessible(true);
                                field.set(obj, loadedResource);
                            }
                        } catch (Exception err) {
                            String errMsg = String.format("Unable to set annotated field value \"%s.%s\": %s",
                                    type.getName(), field.getName(), err.getMessage());
                            log.fatal(errMsg, err);
                            throw new RuntimeException(errMsg, err);
                        }
                    }
                    if (!annotationIs) {
                        annotationIs = true;
                    }
                }
            }
            if (!annotationIs) {
                String errMsg = String.format("No public methods in class \"%s\", annotated by %s",
                        type.getName(), FromTextFile.class.getName());
                log.fatal(errMsg);
                throw new RuntimeException(errMsg);
            }
        } else {
            String errMsg = String.format("Unable to cast object to class \"%s\"", type.getName());
            log.fatal(errMsg);
            throw new RuntimeException(errMsg);
        }
    }

    @NotNull
    public static String fromTextFile(@NotNull String fileName) {
        ClassLoader loader = ResourcesLoader.class.getClassLoader();
        InputStream input = loader.getResourceAsStream(fileName);
        if (input == null && !fileName.startsWith("/")) {
            input = loader.getResourceAsStream("/" + fileName);
        }
        if (input == null) {
            String errMsg = String.format("Unable to load resource from file \"%s\": file not exists", fileName);
            log.fatal(errMsg);
            throw new RuntimeException(errMsg);
        }
        StringBuilder result = new StringBuilder();
        try (BufferedReader bufIO = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = bufIO.readLine()) != null) {
                result.append(line)
                        .append('\n');
            }
        } catch (IOException err) {
            String errMsg = String.format("Unable to load resource file \"%s\": %s",
                    fileName, err.getMessage());
            log.fatal(errMsg, err);
            throw new RuntimeException(errMsg, err);
        }
        if (result.length() == 0) {
            String errMsg = String.format("Resource file \"%s\" is empty", fileName);
            log.fatal(errMsg);
            throw new RuntimeException(errMsg);
        }
        return result.toString();
    }

    public static List<String> getFilenamesInResourceDir(@NotNull final String resourceDirectory) {
        return Arrays.stream(fromTextFile(resourceDirectory).split("\n"))
                .sorted()
                .filter(CommonUtils::isTrStringNotEmpty)
                .collect(Collectors.toUnmodifiableList());
    }
}
