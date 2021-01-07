package hellfrog.commands.cmdline;

import hellfrog.common.BroadCast;
import hellfrog.common.CodeSourceUtils;
import hellfrog.common.SavedAttachment;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;

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
        commandAction(cmdline, event);
    }

    @Override
    protected void executeCreateMessageEventDirect(CommandLine cmdline, ArrayList<String> cmdlineArgs,
                                                   TextChannel channel, MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {
        commandAction(cmdline, event);
    }

    private void commandAction(CommandLine cmdline,
                               MessageCreateEvent event) {
        if (!canExecuteGlobalCommand(event)) {
            showAccessDeniedGlobalMessage(event);
            return;
        }

        boolean updateMainJarAction = cmdline.hasOption('m');
        boolean showLibrariesAction = cmdline.hasOption('s');
        boolean updateLibraryJarAction = cmdline.hasOption('l');
        boolean deleteLibraryJarAction = cmdline.hasOption('d');

        if (showLibrariesAction && (updateMainJarAction || updateLibraryJarAction || deleteLibraryJarAction)) {
            showErrorMessage("Cannot specify different modes of the command at the same time.", event);
        } else if (!(updateMainJarAction || showLibrariesAction || updateLibraryJarAction || deleteLibraryJarAction)) {
            showErrorMessage("Action required.", event);
        } else if (updateMainJarAction) {
            doUpdateMainJar(event);
        } else if (showLibrariesAction) {
            doShowLibraries(event);
        } else if (updateLibraryJarAction) {
            doUploadLibrary(event);
        } else {
            doDeleteLibrary(cmdline, event);
        }
    }

    private void doUpdateMainJar(@NotNull MessageCreateEvent event) {
        BroadCast.MessagesLogger messagesLogger = BroadCast.getLogger()
                .addUnsafeUsageCE("upgrade main jar file", event);
        try {
            List<MessageAttachment> attaches = event.getMessage().getAttachments();
            if (attaches.size() != 1) {
                showErrorMessage("One main jar file required.", event);
                return;
            }
            MessageAttachment attachment = attaches.get(0);
            if (!attachment.getFileName().toLowerCase().endsWith(".jar")) {
                showErrorMessage("The file is not a jar file.", event);
                return;
            }
            Path mainJar;
            try {
                mainJar = CodeSourceUtils.getCodeSourceJarPath();
            } catch (IOException err) {
                showErrorMessage("Unable to resolve code source: " + err, event);
                return;
            }
            if (mainJar == null) {
                showErrorMessage("Replacing of code source not supported", event);
                return;
            }
            messagesLogger.addInfoMessage("Saving attachment");
            try (SavedAttachment savedAttachment = new SavedAttachment(attachment)) {
                messagesLogger.addInfoMessage("Attachment saved")
                        .addInfoMessage("Replacing the main jar file");
                savedAttachment.moveTo(mainJar);
                messagesLogger.addInfoMessage("Replacing done");

            } catch (IOException err) {
                messagesLogger.addErrorMessage("Unable to upgrade main jar file: " + err.getMessage());
            }
        } finally {
            messagesLogger.send();
        }
    }

    private void doShowLibraries(@NotNull MessageCreateEvent event) {
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
                showErrorMessage("Unable to list library dir: " + err, event);
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
                                            .send(getMessageTargetByRights(event))
                            , new Thread(() -> new MessageBuilder()
                                    .append("Library list is empty", MessageDecoration.BOLD)
                                    .send(getMessageTargetByRights(event))));
        } catch (IOException err) {
            showErrorMessage("Unable to get list: " + err, event);
        }
    }

    private void doUploadLibrary(@NotNull MessageCreateEvent event) {
        BroadCast.getLogger()
                .addUnsafeUsageCE("upgrade library jar file", event)
                .send();
        List<MessageAttachment> attaches = event.getMessage().getAttachments();
        if (attaches.isEmpty()) {
            showErrorMessage("One library jar file required.", event);
            return;
        }
        Path libraryRootPath;
        try {
            libraryRootPath = getRootLibraryPath();
        } catch (IOException err) {
            showErrorMessage("Unable to resolve code source: " + err, event);
            return;
        }

        for (MessageAttachment attach : attaches) {
            if (!attach.getFileName().toLowerCase().endsWith(".jar")) {
                showErrorMessage("The file " + attach.getFileName() +
                        " is not a jar file.", event);
                continue;
            }
            showInfoMessage("Saving attachment " + attach.getFileName(), event);
            try (SavedAttachment savedAttachment = new SavedAttachment(attach)) {
                showInfoMessage("Attachment " + savedAttachment.getFileName() +
                        " saved, moving file into libraries directory.", event);
                Path target = libraryRootPath.resolve(savedAttachment.getFileName());
                try {
                    savedAttachment.moveTo(target);
                    showInfoMessage("Attachment " + target + " saved", event);
                } catch (IOException placeE) {
                    showErrorMessage("Unable to place attachment " +
                            savedAttachment.getFileName() +
                            " to libraries directory: " + placeE, event);
                }
            } catch (IOException saveE) {
                showErrorMessage("Unable to save attachment " +
                        attach.getFileName() + ": " + saveE, event);
            }
        }
    }

    private void doDeleteLibrary(@NotNull CommandLine cmdline, @NotNull MessageCreateEvent event) {
        BroadCast.getLogger()
                .addUnsafeUsageCE("delete library", event)
                .send();

        String[] names = cmdline.getOptionValues('d');
        if (names == null || names.length == 0) {
            showErrorMessage("Library names required.", event);
            return;
        }
        Path rootLibraryPath;
        try {
            rootLibraryPath = getRootLibraryPath();
        } catch (IOException err) {
            showErrorMessage("Unable to resolve code source: " + err, event);
            return;
        }
        for (String libName : names) {
            Path target = rootLibraryPath.resolve(libName);
            if (Files.notExists(target)) {
                showErrorMessage("File " + libName + " not exists", event);
                continue;
            }
            if (!Files.isRegularFile(target)) {
                showErrorMessage("File " + libName + " is not regular file", event);
                continue;
            }
            try {
                if (Files.deleteIfExists(target)) {
                    showInfoMessage("File " + target + " deleted", event);
                } else {
                    showErrorMessage("File " + target + " not deleted", event);
                }
            } catch (IOException err) {
                showErrorMessage("Unable to delete file " + target + ": " + err, event);
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
