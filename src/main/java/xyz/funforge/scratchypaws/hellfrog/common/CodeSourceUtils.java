package xyz.funforge.scratchypaws.hellfrog.common;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    static Path createTempFile() throws IOException {
        return Files.createTempFile(getCodeSourceParent(), "file_", "_.tmp");
    }

    @Nullable
    public static Path getCodeSourceJarPath() throws IOException {
        if (codeSourceJarPath == null) getCodeSourceParent();
        return codeSourceJarPath;
    }

    /**
     * Сканирует весь classpath на дочерних классов от указанного суперкласса.
     * Для найденных классов создаёт экземпляры и помещает в список.
     *
     * @return список экземпляров дочерних классов данного родительского класса
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> List<T> childClassInstancesCollector(@NotNull final Class<T> superClassFQDN) {
        List<T> collectedCommandsList = new ArrayList<>();
        List<String> successList = new ArrayList<>();
        List<String> failList = new ArrayList<>();
        try (ScanResult scanResult = new ClassGraph()
                .enableAllInfo()
                .scan()) {
            scanResult.getAllClasses().stream()
                    .filter(ci -> ci.extendsSuperclass(superClassFQDN.getName()))
                    .filter(ci -> !ci.isAbstract())
                    .filter(ClassInfo::isPublic)
                    .map(ClassInfo::getName)
                    .forEachOrdered(name -> {
                        try {
                            Class childClass = Class.forName(name);
                            Object instance = childClass.getDeclaredConstructor()
                                    .newInstance();
                            collectedCommandsList.add((T) instance);
                            successList.add(childClass.getName());
                        } catch (Exception err) {
                            failList.add(name + ": " + err);
                        }
                    });
        }
        successList.stream()
                .reduce((s1, s2) -> s1 + ", " + s2)
                .ifPresent(s -> System.err.println("Created instances of: " + s));
        failList.stream()
                .reduce((s1, s2) -> s1 + '\n' + s2)
                .ifPresent(s -> System.err.println("Unable to create instances of:\n" + s));
        return Collections.unmodifiableList(collectedCommandsList);
    }
}