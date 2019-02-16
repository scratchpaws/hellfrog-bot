package pub.funforge.scratchypaws.rilcobot.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class CodeSourceUtils {

    private volatile static Path codeSourceParentPath = null;
    private volatile static Path codeSourceJarPath = null;

    private CodeSourceUtils() {
        throw new RuntimeException("Instance of " + CodeSourceUtils.class.getName() + " not allowed.");
    }

    public static Path getCodeSourceParent() throws IOException {
        if (codeSourceParentPath != null) return codeSourceParentPath;
        try {
            URI mainCodeSource = SavedAttachment.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI();
            Path _codeSourcePath = Paths.get(mainCodeSource).toRealPath();
            if (Files.isRegularFile(_codeSourcePath)) {
                codeSourceJarPath = _codeSourcePath;
                _codeSourcePath = _codeSourcePath.getParent();
            }
            codeSourceParentPath = _codeSourcePath;
            return codeSourceParentPath;
        } catch (URISyntaxException err) {
            throw new IOException("unable to parse main jar/code source location: " + err, err);
        }
    }

    public static Path resolve(@NotNull String fileName) throws IOException {
        return getCodeSourceParent().resolve(fileName);
    }

    public static Path resolve(@NotNull Path path) throws IOException {
        return getCodeSourceParent().resolve(path);
    }

    public static Path createTempFile() throws IOException {
        return Files.createTempFile(getCodeSourceParent(), "file_", "_.tmp");
    }

    @Nullable
    public static Path getCodeSourceJarPath() throws IOException {
        if (codeSourceJarPath == null) getCodeSourceParent();
        return codeSourceJarPath;
    }
}
