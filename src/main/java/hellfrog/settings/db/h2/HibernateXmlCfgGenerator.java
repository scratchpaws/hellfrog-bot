package hellfrog.settings.db.h2;

import hellfrog.common.CodeSourceUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Software generator of hibernate.cfg.xml at runtime.
 *
 * I have not found a normal way to configure hibernate from runtime
 * (does not find mapped entities despite an explicit call to the addAnnotatedClass method).
 * Therefore, using this class, the hibernate.cfg.xml file is generated at runtime,
 * which is later transferred to the RegisterBuilder for configuration.
 * The configuration file should be automatically deleted when the class resource is closed,
 * but sometimes the file is locked under Windows.
 *
 * You must first set parameters and mapped classes, then call the {@link #create()} method to generate the file.
 * After that, get a link to the file using the {@link #getFile()} method.
 */
public class HibernateXmlCfgGenerator
        implements Closeable, AutoCloseable {

    private static final String HIBERNATE_XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE hibernate-configuration SYSTEM\n" +
            "        \"http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd\">\n" +
            "<hibernate-configuration>\n" +
            "    <session-factory>\n";
    private static final String HIBERNATE_XML_FOOTER = "</session-factory>\n" +
            "</hibernate-configuration>";
    private static final String SETTINGS_DIR_NAME = "settings";

    private final Map<String, String> properties = new HashMap<>();
    private final List<Class<?>> annotatedClasses = new ArrayList<>();
    private final Path tempFileConfig;
    private final Logger sqlLog;

    public HibernateXmlCfgGenerator(@NotNull Logger sqlLog) throws IOException {
        this.sqlLog = sqlLog;
        Path codeSourcePath = CodeSourceUtils.getCodeSourceParent();
        Path settingsPath = codeSourcePath.resolve(SETTINGS_DIR_NAME);
        try {
            if (!Files.exists(settingsPath) || !Files.isDirectory(settingsPath)) {
                Files.createDirectory(settingsPath);
            }
        } catch (IOException err) {
            sqlLog.fatal("Unable to create settings directory: " + err);
            throw err;
        }
        tempFileConfig = Files.createTempFile(settingsPath, "hibernate_", "_.cfg.xml");
    }

    public void setProperty(@NotNull String key, @NotNull String value) {
        this.properties.put(key, value);
    }

    public void addAnnotatedClass(Class<?> annotatedClass) {
        this.annotatedClasses.add(annotatedClass);
    }

    public void create() throws IOException {
        // Hibernate load from config file using FileInputStream with default params. Use same way.
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFileConfig.toFile())))) {
            writer.append(HIBERNATE_XML_HEADER);
            for (Map.Entry<String, String> property : properties.entrySet()) {
                String propString = String.format("<property name=\"%s\">%s</property>\n",
                        StringEscapeUtils.escapeXml11(property.getKey()),
                        StringEscapeUtils.escapeXml11(property.getValue()));
                writer.append(propString);
            }
            for (Class<?> annotatedClass : annotatedClasses) {
                String mappedString = String.format("<mapping class=\"%s\"/>\n",
                        StringEscapeUtils.escapeXml11(annotatedClass.getName()));
                writer.append(mappedString);
            }
            writer.append(HIBERNATE_XML_FOOTER);
            writer.flush();
        }
    }

    public File getFile() {
        return tempFileConfig.toFile();
    }

    @Override
    public void close() {
        try {
            Files.deleteIfExists(tempFileConfig);
        } catch (IOException err) {
            String errMsg1 = String.format("Unable to delete \"%s\" file, adding shutdown hook: %s",
                    tempFileConfig.toString(),
                    err.getMessage());
            sqlLog.warn(errMsg1);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.deleteIfExists(tempFileConfig);
                } catch (IOException hookErr) {
                    String errMsg2 = String.format("Unable to delete \"%s\" file from shutdown hook: %s",
                            tempFileConfig.toString(),
                            hookErr.getMessage());
                    sqlLog.error(errMsg2, hookErr);
                }
            }));
        }
    }
}
