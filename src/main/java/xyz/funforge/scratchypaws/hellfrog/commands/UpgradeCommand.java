package xyz.funforge.scratchypaws.hellfrog.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;
import xyz.funforge.scratchypaws.hellfrog.common.BroadCast;
import xyz.funforge.scratchypaws.hellfrog.common.CodeSourceUtils;
import xyz.funforge.scratchypaws.hellfrog.common.SavedAttachment;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class UpgradeCommand
        extends BotCommand {

    private static final String PREFIX = "upgrade";
    private static final String DESCRIPTION = "Service command for bot upgrade";
    private static final String LIBRARY_PATH_NAME = "lib";

    public UpgradeCommand() {
        super(PREFIX, DESCRIPTION);

        Option mainJar = Option.builder("m")
                .longOpt("main")
                .desc("Update main bot jar file")
                .build();

        Option showLibraries = Option.builder("s")
                .longOpt("show")
                .desc("Show libraries jar files")
                .build();

        Option updateLibrary = Option.builder("l")
                .longOpt("lib")
                .desc("Upload library jar file")
                .build();

        Option deleteLibrary = Option.builder("d")
                .longOpt("del")
                .hasArgs()
                .argName("library jar file name")
                .desc("Delete jar file library for name")
                .build();

        disableUpdateLastCommandUsage();
        addCmdlineOption(mainJar, showLibraries, updateLibrary, deleteLibrary);
    }

    @Override
    protected void executeCreateMessageEventServer(Server server,
                                                   CommandLine cmdline, ArrayList<String> cmdlineArgs,
                                                   TextChannel channel, MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {
        commandAction(cmdline, channel, event);
    }

    @Override
    protected void executeCreateMessageEventDirect(CommandLine cmdline, ArrayList<String> cmdlineArgs,
                                                   TextChannel channel, MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {
        commandAction(cmdline, channel, event);
    }

    private void commandAction(CommandLine cmdline,
                               TextChannel channel, MessageCreateEvent event) {
        if (!canExecuteGlobalCommand(event)) {
            showAccessDeniedGlobalMessage(channel);
            return;
        }

        boolean updateMainJarAction = cmdline.hasOption('m');
        boolean showLibrariesAction = cmdline.hasOption('s');
        boolean updateLibraryJarAction = cmdline.hasOption('l');
        boolean deleteLibraryJarAction = cmdline.hasOption('d');

        if (showLibrariesAction && (updateMainJarAction || updateLibraryJarAction || deleteLibraryJarAction)) {
            showErrorMessage("Cannot specify different modes of the command at the same time.", channel);
        } else if (!(updateMainJarAction || showLibrariesAction || updateLibraryJarAction || deleteLibraryJarAction)) {
            showErrorMessage("Action required.", channel);
        } else if (updateMainJarAction) {
            doUpdateMainJar(event);
        } else if (showLibrariesAction) {
            doShowLibraries(channel);
        } else if (updateLibraryJarAction) {
            doUploadLibrary(event);
        } else {
            doDeleteLibrary(cmdline, event);
        }
    }

    private void doUpdateMainJar(@NotNull MessageCreateEvent event) {
        TextChannel channel = event.getChannel();
        BroadCast.sendBroadcastUnsafeUsageCE("upgrade main jar file", event);
        List<MessageAttachment> attaches = event.getMessage().getAttachments();
        if (attaches.size() != 1) {
            showErrorMessage("One main jar file required.", channel);
            return;
        }
        MessageAttachment attachment = attaches.get(0);
        if (!attachment.getFileName().toLowerCase().endsWith(".jar")) {
            showErrorMessage("The file is not a jar file.", channel);
            return;
        }
        Path mainJar;
        try {
            mainJar = CodeSourceUtils.getCodeSourceJarPath();
        } catch (IOException err) {
            showErrorMessage("Unable to resolve code source: " + err, channel);
            return;
        }
        if (mainJar == null) {
            showErrorMessage("Replacing of code source not supported", channel);
            return;
        }
        showInfoMessage("Saving attachment", channel);
        try (SavedAttachment savedAttachment = new SavedAttachment(attachment)) {
            showInfoMessage("Attachment saved", channel);


            showInfoMessage("Replacing the main jar file", channel);
            savedAttachment.moveTo(mainJar);
            showInfoMessage("Replacing done", channel);

        } catch (IOException err) {
            showErrorMessage("Unable to upgrade main jar file: " + err, channel);
        }
    }

    private void doShowLibraries(@NotNull TextChannel channel) {
        Module unnamed = getClass().getModule();
        Set<String> activePackages = unnamed != null ? unnamed.getPackages() : new TreeSet<>();
        try {
            Path libraryRootPath = getRootLibraryPath();
            List<Path> found = new ArrayList<>();
            try (DirectoryStream<Path> libStream = Files.newDirectoryStream(libraryRootPath, Files::isRegularFile)) {
                for (Path entry : libStream) {
                    found.add(entry);
                }
            } catch (IOException err) {
                showErrorMessage("Unable to list library dir: " + err, channel);
                return;
            }
            found.sort(Comparator.comparing(Path::getFileName));
            found.stream()
                    .map(p -> scanLibraryFile(p, activePackages))
                    .reduce((p1, p2) -> p1 + '\n' + p2)
                    .ifPresentOrElse(list ->
                                    new MessageBuilder()
                                            .append("Library list:", MessageDecoration.BOLD)
                                            .appendNewLine()
                                            .append(list, MessageDecoration.CODE_LONG)
                                            .send(channel)
                            , new Thread(() -> new MessageBuilder()
                                    .append("Library list is empty", MessageDecoration.BOLD)
                                    .send(channel)));
        } catch (IOException err) {
            showErrorMessage("Unable to get list: " + err, channel);
        }
    }

    private void doUploadLibrary(@NotNull MessageCreateEvent event) {
        TextChannel channel = event.getChannel();
        BroadCast.sendBroadcastUnsafeUsageCE("upgrade library jar file", event);
        List<MessageAttachment> attaches = event.getMessage().getAttachments();
        if (attaches.size() == 0) {
            showErrorMessage("One library jar file required.", channel);
            return;
        }
        Path libraryRootPath;
        try {
            libraryRootPath = getRootLibraryPath();
        } catch (IOException err) {
            showErrorMessage("Unable to resolve code source: " + err, channel);
            return;
        }

        for (MessageAttachment attach : attaches) {
            if (!attach.getFileName().toLowerCase().endsWith(".jar")) {
                showErrorMessage("The file " + attach.getFileName() +
                        " is not a jar file.", channel);
                continue;
            }
            showInfoMessage("Saving attachment " + attach.getFileName(), channel);
            try (SavedAttachment savedAttachment = new SavedAttachment(attach)) {
                showInfoMessage("Attachment " + savedAttachment.getFileName() +
                        " saved, moving file into libraries directory.", channel);
                Path target = libraryRootPath.resolve(savedAttachment.getFileName());
                try {
                    savedAttachment.moveTo(target);
                    showInfoMessage("Attachment " + target + " saved", channel);
                } catch (IOException placeE) {
                    showErrorMessage("Unable to place attachment " +
                            savedAttachment.getFileName() +
                            " to libraries directory: " + placeE, channel);
                }
            } catch (IOException saveE) {
                showErrorMessage("Unable to save attachment " +
                        attach.getFileName() + ": " + saveE, channel);
            }
        }
    }

    private void doDeleteLibrary(@NotNull CommandLine cmdline, @NotNull MessageCreateEvent event) {
        TextChannel channel = event.getChannel();
        BroadCast.sendBroadcastUnsafeUsageCE("delete library", event);
        String[] names = cmdline.getOptionValues('d');
        if (names == null || names.length == 0) {
            showErrorMessage("Library names required.", channel);
            return;
        }
        Path rootLibraryPath;
        try {
            rootLibraryPath = getRootLibraryPath();
        } catch (IOException err) {
            showErrorMessage("Unable to resolve code source: " + err, channel);
            return;
        }
        for (String libName : names) {
            Path target = rootLibraryPath.resolve(libName);
            if (Files.notExists(target)) {
                showErrorMessage("File " + libName + " not exists", channel);
                continue;
            }
            if (!Files.isRegularFile(target)) {
                showErrorMessage("File " + libName + " is not regular file", channel);
                continue;
            }
            try {
                if (Files.deleteIfExists(target)) {
                    showInfoMessage("File " + target + " deleted", channel);
                } else {
                    showErrorMessage("File " + target + " not deleted", channel);
                }
            } catch (IOException err) {
                showErrorMessage("Unable to delete file " + target + ": " + err, channel);
            }
        }
    }

    private Path getRootLibraryPath() throws IOException {
        Path libraryRootPath = CodeSourceUtils.resolve(LIBRARY_PATH_NAME);
        if (Files.notExists(libraryRootPath)) {
            try {
                Files.createDirectories(libraryRootPath);
            } catch (IOException clrE) {
                throw new IOException("unable to create library root path: " + clrE, clrE);
            }
        }
        return libraryRootPath;
    }

    private String scanLibraryFile(@NotNull Path inputLib, @NotNull Set<String> activePackages) {
        String libName = inputLib.getFileName().toString();
        if (libName.toLowerCase().endsWith(".jar")) {
            try (JarInputStream jis = new JarInputStream(
                    new BufferedInputStream(Files.newInputStream(inputLib)))) {

                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    if (entry.isDirectory()) continue;
                    String entryName = entry.getName()
                            .replace("\\", ".")
                            .replace("/", ".");
                    for (String pkg : activePackages) {
                        if (entryName.startsWith(pkg)) {
                            return libName + " (active)";
                        }
                    }
                }
            } catch (IOException scanErr) {
                return libName + " (unable to scan: " + scanErr + ")";
            }
        } else {
            return libName + " (not a jar file)";
        }
        return libName;
    }
}
