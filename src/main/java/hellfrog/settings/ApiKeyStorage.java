package hellfrog.settings;

import hellfrog.common.CodeSourceUtils;
import hellfrog.common.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ApiKeyStorage {

    private static final String API_KEY_FILE = "api_key.txt";
    private static final String SETTINGS_DIR_NAME = "settings";

    private ApiKeyStorage() {
        throw new RuntimeException("Cannot create instance of " + ApiKeyStorage.class.getName() + ": not allowed");
    }
    public static void writeApiKey(@NotNull final String apiKey) throws IOException {
        if (CommonUtils.isTrStringEmpty(apiKey)) {
            throw new IOException("Unable to write api key to file: api key is empty");
        }
        Path apiKeyFile = resolveSettingsPath().resolve(API_KEY_FILE);
        try {
            Files.writeString(apiKeyFile, apiKey.strip(), StandardCharsets.UTF_8);
        } catch (IOException err) {
            throw new IOException("Unable save api key to file \"" + apiKeyFile + "\": " + err.getMessage(), err);
        }
    }

    @NotNull
    public static String readApiKey() throws IOException {
        Path apiKeyFile = resolveSettingsPath().resolve(API_KEY_FILE);
        if (Files.notExists(apiKeyFile)) {
            throw new IOException("Unable read api key to file \"" + apiKeyFile + "\": file not exists");
        }
        try {
            String apiKey = Files.readString(apiKeyFile, StandardCharsets.UTF_8);
            if (CommonUtils.isTrStringEmpty(apiKey)) {
                throw new IOException("API key string in file is empty");
            }
            return apiKey.strip();
        } catch (IOException err) {
            throw new IOException("Unable read api key to file \"" + apiKeyFile + "\": " + err.getMessage(), err);
        }
    }

    private static Path resolveSettingsPath() throws IOException {
        try {
            Path settingsPath = CodeSourceUtils.getCodeSourceParent()
                    .resolve(SETTINGS_DIR_NAME);
            if (Files.notExists(settingsPath)) {
                Files.createDirectories(settingsPath);
            }
            if (!Files.isDirectory(settingsPath)) {
                throw new IOException('\"' + settingsPath.toString() + "\" is not a directory");
            }
            return settingsPath;
        } catch (IOException err) {
            throw new IOException("Unable to create settings directory: " + err.getMessage(), err);
        }
    }
}
