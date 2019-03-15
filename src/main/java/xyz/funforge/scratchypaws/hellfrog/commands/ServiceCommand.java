package xyz.funforge.scratchypaws.hellfrog.commands;

import bsh.Interpreter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.core.entity.permission.PermissionsImpl;
import xyz.funforge.scratchypaws.hellfrog.common.BroadCast;
import xyz.funforge.scratchypaws.hellfrog.common.CommonUtils;
import xyz.funforge.scratchypaws.hellfrog.reactions.DiceReaction;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Управление и диагностика бота
 */
public class ServiceCommand
        extends BotCommand {

    private static final String PREF = "srv";
    private static final String DESCRIPTIONS = "Common bot service commands";

    public ServiceCommand() {
        super(PREF, DESCRIPTIONS);

        Option stopBot = Option.builder("s")
                .longOpt("stop")
                .desc("Disconnect and stop bot")
                .build();

        Option memInfo = Option.builder("m")
                .longOpt("mem")
                .desc("Memory info")
                .build();

        Option botDate = Option.builder("d")
                .longOpt("date")
                .desc("Get bot current local date and time")
                .build();

        Option inviteUrl = Option.builder("i")
                .longOpt("invite")
                .desc("Get bot invite url")
                .build();

        Option runGc = Option.builder("g")
                .longOpt("gc")
                .desc("Run garbage collector")
                .build();

        Option runtimeShell = Option.builder("r")
                .longOpt("runtime")
                .desc("Execute runtime debug module")
                .build();

        Option lastUsage = Option.builder("l")
                .longOpt("last")
                .desc("Show last bot usage")
                .build();

        Option uploadFailRofl = Option.builder("f")
                .longOpt("failrofl")
                .desc("Upload low dice level rofl file")
                .build();

        Option uploadWinRofl = Option.builder("w")
                .longOpt("winforl")
                .desc("Upload high dice level rofl file")
                .build();

        super.addCmdlineOption(stopBot, memInfo, botDate, inviteUrl, runGc, runtimeShell, lastUsage,
                uploadFailRofl, uploadWinRofl);

        super.disableUpdateLastCommandUsage();
    }

    /**
     * Обработка команды, поступившей из текстового канала сервера.
     *
     * @param server      Сервер текстового канала, откуда поступило сообщение
     * @param cmdline     обработанные аргументы командной строки
     * @param cmdlineArgs оставшиеся значения командной строки
     * @param channel     текстовый канал, откуда поступило сообщение
     * @param event       событие сообщения
     */
    @Override
    protected void executeCreateMessageEventServer(Server server,
                                                   CommandLine cmdline, ArrayList<String> cmdlineArgs,
                                                   TextChannel channel, MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {
        executeCommand(cmdline, channel, event, anotherLines);
    }

    /**
     * Обработка команды, поступившей из привата.
     *
     * @param cmdline     обработанные аргументы командной строки
     * @param cmdlineArgs оставшиеся значения командной строки
     * @param channel     текстовый канал, откуда поступило сообщение
     * @param event       событие сообщения
     */
    @Override
    protected void executeCreateMessageEventDirect(CommandLine cmdline, ArrayList<String> cmdlineArgs,
                                                   TextChannel channel, MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {
        executeCommand(cmdline, channel, event, anotherLines);
    }

    private void executeCommand(CommandLine cmdline,
                                TextChannel channel, MessageCreateEvent event,
                                ArrayList<String> anotherLines) {
        if (!canExecuteGlobalCommand(event)) {
            showAccessDeniedGlobalMessage(channel);
            return;
        }

        SettingsController settingsController = SettingsController.getInstance();

        boolean stopAction = cmdline.hasOption('s');
        boolean memInfo = cmdline.hasOption('m');
        boolean getDate = cmdline.hasOption('d');
        boolean getInvite = cmdline.hasOption('i');
        boolean runGc = cmdline.hasOption('g');
        boolean runtimeShell = cmdline.hasOption('r');
        boolean lastUsageAction = cmdline.hasOption('l');
        boolean uploadFailRofl = cmdline.hasOption('f');
        boolean uploadWinRofl = cmdline.hasOption('w');

        if (stopAction ^ memInfo ^ getDate ^ getInvite ^ runGc ^ runtimeShell ^ lastUsageAction ^
                uploadWinRofl ^ uploadFailRofl) {

            if (stopAction) {
                doStopAction(event);
            }

            if (runGc) {
                Runtime.getRuntime()
                        .gc();
            }

            if (memInfo || runGc) {
                Runtime runtime = Runtime.getRuntime();
                long allocatedMemory =
                        (runtime.totalMemory() - runtime.freeMemory());
                new MessageBuilder()
                        .append("Free memory: ", MessageDecoration.BOLD)
                        .append(String.valueOf(runtime.freeMemory()))
                        .appendNewLine()
                        .append("Max memory: ", MessageDecoration.BOLD)
                        .append(String.valueOf(runtime.maxMemory()))
                        .appendNewLine()
                        .append("Total memory: ", MessageDecoration.BOLD)
                        .append(String.valueOf(runtime.totalMemory()))
                        .appendNewLine()
                        .append("Allocated memory: ", MessageDecoration.BOLD)
                        .append(String.valueOf(allocatedMemory))
                        .send(channel);
            }

            if (getDate) {
                new MessageBuilder()
                        .append("Current date and time: ", MessageDecoration.BOLD)
                        .append(CommonUtils.getCurrentGmtTimeAsString())
                        .send(channel);
            }

            if (getInvite) {
                if (settingsController.getDiscordApi() != null) {
                    new MessageBuilder()
                            .append("Bot invite URL: ", MessageDecoration.BOLD)
                            .append(settingsController.getDiscordApi()
                                    .createBotInvite(new PermissionsImpl(470149318)))
                            .send(channel);
                }
            }

            if (runtimeShell) {
                doRunShellAction(anotherLines, event);
            }

            if (lastUsageAction) {
                Instant lastCommandUsage = settingsController.getLastCommandUsage();
                if (lastCommandUsage == null) {
                    new MessageBuilder()
                            .append("No last usage found")
                            .send(channel);
                } else {
                    Instant current = Instant.now();
                    long lastUsageMinutes = ChronoUnit.MINUTES.between(lastCommandUsage, current);
                    new MessageBuilder()
                            .append("Last usage ")
                            .append(String.valueOf(lastUsageMinutes))
                            .append(" min. ago.")
                            .send(channel);
                }
            }

            if (uploadWinRofl || uploadFailRofl) {
                uploadRoflAction(event, uploadWinRofl);
            }
        } else {
            showErrorMessage("Only one service command may be execute", channel);
        }
    }

    private void doStopAction(MessageCreateEvent event) {

        SettingsController settingsController = SettingsController.getInstance();

        settingsController
                .getVoteController()
                .stop();
        settingsController
                .getServerStatisticTask()
                .stop();
        settingsController
                .saveCommonPreferences();
        for (long serverId : settingsController.getServerListWithConfig()) {
            settingsController.saveServerSideParameters(serverId);
        }
        for (long serverId : settingsController.getServerListWithStatistic()) {
            settingsController.saveServerSideStatistic(serverId);
        }

        new MessageBuilder()
                .append("Bot stopping NOW!")
                .send(event.getChannel()).join();

        BroadCast.sendBroadcastUnsafeUsageCE("call bot stopping", event);

        if (settingsController.getDiscordApi() != null) {
            settingsController.getDiscordApi().disconnect();
        }

        System.exit(0);
    }

    private void doRunShellAction(ArrayList<String> anotherLines, MessageCreateEvent event) {
        BroadCast.sendBroadcastUnsafeUsageCE("call remote debug shell module", event);

        final SettingsController settingsController = SettingsController.getInstance();
        final long channelId = event.getChannel().getId();

        if (settingsController.isEnableRemoteDebug()) {
            anotherLines.stream()
                    .reduce((s1, s2) -> s1 + " " + s2)
                    .ifPresentOrElse(cmd -> {
                        final String command = cmd.replaceAll("`{3}", "");
                        CompletableFuture.runAsync(() -> executeDebugCommand(command, channelId));
                    }, new Thread(() -> {
                        List<MessageAttachment> attachmentList = event.getMessage()
                                .getAttachments();
                        if (attachmentList.size() > 0) {
                            for (MessageAttachment attachment : attachmentList) {
                                try {
                                    byte[] attach = attachment.downloadAsByteArray()
                                            .join();
                                    String buff;
                                    StringBuilder execScript = new StringBuilder();
                                    try (BufferedReader textReader = new BufferedReader(
                                            new InputStreamReader(new ByteArrayInputStream(attach), StandardCharsets.UTF_8))) {
                                        while ((buff = textReader.readLine()) != null) {
                                            execScript.append(buff)
                                                    .append('\n');
                                        }
                                    }
                                    executeDebugCommand(execScript.toString(), event.getChannel().getId());
                                } catch (Exception err) {
                                    new MessageBuilder()
                                            .append("Exception reached while attachment downloading:", MessageDecoration.BOLD)
                                            .appendNewLine()
                                            .append(ExceptionUtils.getStackTrace(err), MessageDecoration.CODE_LONG)
                                            .send(event.getChannel());
                                }
                            }
                        } else {
                            showErrorMessage("Debug script required", event.getChannel());
                        }
                    }));

        } else {
            showErrorMessage("Remote debug not allowed", event.getChannel());
        }
    }

    private synchronized void executeDebugCommand(String cmd, long channelId) {

        DiscordApi discordApi = SettingsController.getInstance().getDiscordApi();
        if (discordApi == null)
            return;
        Optional<TextChannel> mayBeChannel = discordApi.getTextChannelById(channelId);
        if (mayBeChannel.isEmpty())
            return;
        TextChannel channel = mayBeChannel.get();

        PrintStream defaultOut = System.out;
        PrintStream defaultErr = System.err;

        try {
            ByteArrayOutputStream outCache = new ByteArrayOutputStream();
            Object result;
            try (PrintStream overrideOut = new PrintStream(outCache, true)) {
                System.setOut(overrideOut);
                System.setErr(overrideOut);
                Interpreter interpreter = new Interpreter();
                result = interpreter.eval(cmd);
            }
            StringBuilder resultOut = new StringBuilder();
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(outCache.toByteArray())
            ))) {
                String buff;
                while ((buff = bufferedReader.readLine()) != null)
                    resultOut.append(buff);
            }
            if (result != null) {
                resultOut.append('\n')
                        .append("Returned value: ")
                        .append(result.toString());
            }
            if (resultOut.length() == 0)
                resultOut.append(" ");
            new MessageBuilder()
                    .append(resultOut.toString(), MessageDecoration.CODE_LONG)
                    .send(channel);
        } catch (Exception err) {
            new MessageBuilder()
                    .append("Exception reached while script execution:", MessageDecoration.BOLD)
                    .appendNewLine()
                    .append(ExceptionUtils.getStackTrace(err), MessageDecoration.CODE_LONG)
                    .send(channel);
        } finally {
            System.setErr(defaultErr);
            System.setOut(defaultOut);
        }
    }

    private void uploadRoflAction(MessageCreateEvent event, boolean isWin) {
        BroadCast.sendBroadcastUnsafeUsageCE("upload rofl file", event);
        if (event.getMessageAttachments().size() == 0) {
            showErrorMessage("Attach files requred", event.getChannel());
            return;
        }
        Path targetDirectory = isWin ? DiceReaction.getRoflHighPath() : DiceReaction.getRoflLowPath();
        try {
            if (Files.notExists(DiceReaction.getRoflHighPath())) {
                Files.createDirectories(DiceReaction.getRoflHighPath());
            }
            if (Files.notExists(DiceReaction.getRoflLowPath())) {
                Files.createDirectories(DiceReaction.getRoflLowPath());
            }
            for (MessageAttachment attach : event.getMessageAttachments()) {
                String fileName = attach.getFileName();
                byte[] fileData = attach.downloadAsByteArray().join();
                Path tmpFile = Paths.get("./").resolve(fileName);
                try (BufferedOutputStream buffOut = new BufferedOutputStream(Files.newOutputStream(tmpFile))) {
                    buffOut.write(fileData);
                    buffOut.flush();
                }
                Path targetFile = targetDirectory.resolve(fileName);
                Files.move(tmpFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                new MessageBuilder()
                        .append("File " + fileName + " uploaded to " +
                                (isWin ? "win (high)" : "fail (low)") + " rofl collection.")
                        .send(event.getChannel());
            }
        } catch (Exception err) {
            new MessageBuilder()
                    .append("Exception reached while upload rofl file:", MessageDecoration.BOLD)
                    .appendNewLine()
                    .append(ExceptionUtils.getStackTrace(err), MessageDecoration.CODE_LONG)
                    .send(event.getChannel());
        }
    }
}
