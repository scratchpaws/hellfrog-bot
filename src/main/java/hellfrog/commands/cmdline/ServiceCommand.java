package hellfrog.commands.cmdline;

import groovy.lang.GroovyShell;
import hellfrog.common.BroadCast;
import hellfrog.common.CommonUtils;
import hellfrog.common.MessageUtils;
import hellfrog.core.ServerSideResolver;
import hellfrog.settings.SettingsController;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
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
    private final GroovyShell groovyShell = new GroovyShell();
    private static final String SHELL_IMPORTS =
            """
                    import hellfrog.settings.SettingsController\s
                    import hellfrog.common.BroadCast\s
                    import hellfrog.common.CommonUtils\s
                    import hellfrog.common.MessageUtils\s
                    import hellfrog.core.ServerSideResolver\s
                    import org.javacord.api.entity.message.MessageBuilder\s
                    import org.javacord.api.DiscordApi
                    import org.javacord.api.entity.server.Server
                    import org.javacord.api.entity.emoji.*
                    import org.javacord.api.entity.channel.*
                    def api = SettingsController.getInstance().getDiscordApi()\s
                    """;

    private final Option stopBot = Option.builder("s")
            .longOpt("stop")
            .desc("Disconnect and stop bot")
            .build();

    private final Option memInfo = Option.builder("m")
            .longOpt("mem")
            .desc("Memory info")
            .build();

    private final Option botDate = Option.builder("d")
            .longOpt("date")
            .desc("Get bot current local date and time")
            .build();

    private final Option runGc = Option.builder("g")
            .longOpt("gc")
            .desc("Run garbage collector")
            .build();

    private final Option runtimeShell = Option.builder("r")
            .longOpt("runtime")
            .desc("Execute runtime debug module")
            .build();

    private final Option lastUsage = Option.builder("l")
            .longOpt("last")
            .desc("Show last bot usage")
            .build();

    private final Option secureTransfer = Option.builder("e")
            .longOpt("sec")
            .desc("Enable two-phase transfer from bot private")
            .hasArg()
            .optionalArg(true)
            .argName("text chat")
            .build();

    private final Option executeQuery = Option.builder("q")
            .longOpt("query")
            .desc("Execute raw SQL query in the main DB")
            .build();

    private final Option getDDL = Option.builder("a")
            .longOpt("ddl")
            .desc("Generate DDL for current instance")
            .build();

    private final Option executeJPQL = Option.builder("j")
            .longOpt("jpql")
            .desc("Execute JPQL query")
            .build();

    private final Option createBackup = Option.builder("b")
            .longOpt("backup")
            .desc("Create DB backup")
            .build();

    public ServiceCommand() {
        super(PREF, DESCRIPTIONS);

        super.addCmdlineOption(stopBot, memInfo, botDate, runGc, runtimeShell, lastUsage, secureTransfer,
                executeQuery, getDDL, executeJPQL, createBackup);
        super.setAdminCommand();
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
            showAccessDeniedGlobalMessage(event);
            return;
        }

        SettingsController settingsController = SettingsController.getInstance();

        boolean stopAction = cmdline.hasOption(this.stopBot.getOpt());
        boolean memInfo = cmdline.hasOption(this.memInfo.getOpt());
        boolean getDate = cmdline.hasOption(this.botDate.getOpt());
        boolean runGc = cmdline.hasOption(this.runGc.getOpt());
        boolean runtimeShell = cmdline.hasOption(this.runtimeShell.getOpt());
        boolean lastUsageAction = cmdline.hasOption(this.lastUsage.getOpt());
        boolean secureTransfer = cmdline.hasOption(this.secureTransfer.getOpt());
        boolean executeQuery = cmdline.hasOption(this.executeQuery.getOpt());
        boolean generateDDL = cmdline.hasOption(this.getDDL.getOpt());
        boolean executeJPQL = cmdline.hasOption(this.executeJPQL.getOpt());
        boolean createBackup = cmdline.hasOption(this.createBackup.getOpt());

        if (stopAction ^ memInfo ^ getDate ^ runGc ^ runtimeShell ^ lastUsageAction ^ secureTransfer
                ^ executeQuery ^ generateDDL ^ executeJPQL ^ createBackup) {

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
                        .setEmbed(new EmbedBuilder()
                                .setTimestampToNow()
                                .setTitle("Memory usage")
                                .addField("Free memory:", CommonUtils.humanReadableByteCount(runtime.freeMemory(), false), true)
                                .addField("Max memory:", CommonUtils.humanReadableByteCount(runtime.maxMemory(), false), true)
                                .addField("Total memory:", CommonUtils.humanReadableByteCount(runtime.totalMemory(), false), true)
                                .addField("Allocated memory:", CommonUtils.humanReadableByteCount(allocatedMemory, false), true)
                                .setDescription(runGc ? "GC called" : ""))
                        .send(getMessageTargetByRights(event));
            }

            if (getDate) {
                new MessageBuilder()
                        .setEmbed(new EmbedBuilder()
                                .setTitle("Current date and time: ")
                                .setDescription(CommonUtils.getCurrentGmtTimeAsString())
                                .setTimestampToNow())
                        .send(getMessageTargetByRights(event));
            }

            if (runtimeShell) {
                doRunShellAction(anotherLines, event);
            }

            if (lastUsageAction) {
                Instant lastCommandUsage = settingsController.getLastCommandUsage();
                if (lastCommandUsage == null) {
                    new MessageBuilder()
                            .setEmbed(new EmbedBuilder()
                                    .setTimestampToNow()
                                    .setTitle("Last usage")
                                    .setDescription("No last usage found"))
                            .send(getMessageTargetByRights(event));
                } else {
                    Duration duration = Duration.between(lastCommandUsage, Instant.now());
                    long totalSeconds = duration.toSeconds();
                    long hours = totalSeconds / 3600L;
                    int minutes = (int) ((totalSeconds % 3600L) / 60L);
                    int seconds = (int) (totalSeconds % 60L);
                    String res = (hours > 0 ? hours + " h. " : "")
                            + (minutes > 0 ? minutes + " m. " : "")
                            + seconds + " s. ago";
                    new MessageBuilder()
                            .setEmbed(new EmbedBuilder()
                                    .setTimestampToNow()
                                    .setTitle("Last usage")
                                    .setDescription(res))
                            .send(getMessageTargetByRights(event));
                }
            }

            if (executeQuery) {
                doExecuteQuery(anotherLines, event);
            }

            if (executeJPQL) {
                doExecuteJPQL(anotherLines, event);
            }

            if (generateDDL) {
                event.getMessageAuthor()
                        .asUser()
                        .flatMap(User::getPrivateChannel)
                        .ifPresent(this::generateDDL);
            }

            if (createBackup) {
                settingsController.getMainDBController().createBackup();
            }
        } else {
            showErrorMessage("Only one service command may be execute", event);
        }
    }

    private void doStopAction(@NotNull MessageCreateEvent event) {

        SettingsController settingsController = SettingsController.getInstance();

        settingsController.getAutoBackupService().stop();
        settingsController.getHttpClientsPool().stop();
        settingsController.getVoteController().stop();
        settingsController.getCongratulationsController().stop();
        settingsController.getInvitesController().stop();
        settingsController.getAutoSaveSettingsTask().stop();
        settingsController.getSessionsCheckTask().stop();
        settingsController.getServiceLogsNotificator().stop();
        settingsController.getAutoPromoteService().stop();
        settingsController.getNameCacheService().stop();
        settingsController.saveCommonPreferences();
        settingsController.getServerListWithConfig()
                .forEach(settingsController::saveServerSideParameters);
        settingsController.getServerListWithStatistic()
                .forEach(settingsController::saveServerSideStatistic);

        BroadCast.getLogger()
                .addUnsafeUsageCE("call bot stopping", event)
                .addInfoMessage("Bot stopping NOW!")
                .send();

        if (settingsController.getDiscordApi() != null) {
            settingsController.getDiscordApi().disconnect();
        }

        try {
            settingsController.stopMainDatabase();
        } catch (Exception err) {
            log.fatal(err);
        }
        System.exit(0);
    }

    private void doExecuteQuery(@NotNull ArrayList<String> anotherLines,
                                @NotNull MessageCreateEvent event) {
        BroadCast.getLogger()
                .addUnsafeUsageCE("run SQL query", event)
                .send();

        Optional<String> mayBeQuery = anotherLines.stream()
                .reduce(CommonUtils::reduceNewLine);

        mayBeQuery.ifPresentOrElse(query -> {
            final String command = query.replaceAll("`{3}", "");
            executeQueryAction(command, event.getChannel().getId());
        }, () -> {
            List<MessageAttachment> attachmentList = event.getMessage()
                    .getAttachments();
            if (attachmentList.size() > 0) {
                attachmentList.forEach((attachment) -> {
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
                        executeQueryAction(execScript.toString(), event.getChannel().getId());
                    } catch (Exception err) {
                        new MessageBuilder()
                                .append("Exception reached while attachment downloading:", MessageDecoration.BOLD)
                                .appendNewLine()
                                .append(ExceptionUtils.getStackTrace(err), MessageDecoration.CODE_LONG)
                                .send(event.getChannel());
                    }
                });
            } else {
                showErrorMessage("Query text required", event);
            }
        });
    }

    private void executeQueryAction(@NotNull final String queryTest, final long channelId) {
        CompletableFuture.runAsync(() ->
                Optional.ofNullable(SettingsController.getInstance()
                        .getDiscordApi()).flatMap(api ->
                        api.getTextChannelById(channelId)).ifPresent(channel -> {
                    String result = SettingsController.getInstance().getMainDBController().executeRawQuery(queryTest);
                    MessageUtils.sendLongMessage(result, channel);
                }));
    }

    private void doExecuteJPQL(@NotNull ArrayList<String> anotherLines,
                               @NotNull MessageCreateEvent event) {
        BroadCast.getLogger()
                .addUnsafeUsageCE("run JPQL query", event)
                .send();

        Optional<String> mayBeQuery = anotherLines.stream()
                .reduce(CommonUtils::reduceNewLine);

        mayBeQuery.ifPresentOrElse(query -> {
            final String command = query.replaceAll("`{3}", "");
            executeJPQLAction(command, event.getChannel().getId());
        }, () -> {
            List<MessageAttachment> attachmentList = event.getMessage()
                    .getAttachments();
            if (attachmentList.size() > 0) {
                attachmentList.forEach((attachment) -> {
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
                        executeJPQLAction(execScript.toString(), event.getChannel().getId());
                    } catch (Exception err) {
                        new MessageBuilder()
                                .append("Exception reached while attachment downloading:", MessageDecoration.BOLD)
                                .appendNewLine()
                                .append(ExceptionUtils.getStackTrace(err), MessageDecoration.CODE_LONG)
                                .send(event.getChannel());
                    }
                });
            } else {
                showErrorMessage("Query text required", event);
            }
        });
    }

    private void executeJPQLAction(@NotNull final String queryText, final long channelId) {
        CompletableFuture.runAsync(() ->
                Optional.ofNullable(SettingsController.getInstance()
                        .getDiscordApi()).flatMap(api ->
                        api.getTextChannelById(channelId)).ifPresent(channel -> {
                    String result = SettingsController.getInstance().getMainDBController().executeRawJPQL(queryText);
                    MessageUtils.sendLongMessage(result, channel);
                }));
    }

    private void generateDDL(@NotNull final TextChannel target) {
        byte[] textFile = SettingsController.getInstance()
                .getMainDBController()
                .generateDDL();
        if (textFile.length > 0) {
            new MessageBuilder()
                    .setEmbed(new EmbedBuilder()
                            .setDescription("Generated DDL into attachment")
                            .setTimestampToNow())
                    .addAttachment(textFile, "DDL.sql")
                    .send(target);
        } else {
            new MessageBuilder()
                    .setEmbed(new EmbedBuilder()
                            .setDescription("Generated DDL is empty (operation not supported or error reached)")
                            .setTimestampToNow())
                    .send(target);
        }
    }

    private void doRunShellAction(ArrayList<String> anotherLines, MessageCreateEvent event) {
        BroadCast.getLogger()
                .addUnsafeUsageCE("call remote debug shell module", event)
                .send();

        final SettingsController settingsController = SettingsController.getInstance();
        final long channelId = event.getChannel().getId();

        if (settingsController.isEnableRemoteDebug()) {
            Optional<String> shellCmd = anotherLines.stream()
                    .reduce(CommonUtils::reduceNewLine);

            shellCmd.ifPresentOrElse(cmd -> {
                final String command = cmd.replaceAll("`{3}", "");
                CompletableFuture.runAsync(() -> executeDebugCommand(command, channelId));
            }, () -> {
                List<MessageAttachment> attachmentList = event.getMessage()
                        .getAttachments();
                if (attachmentList.size() > 0) {
                    attachmentList.forEach((attachment) -> {
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
                    });
                } else {
                    showErrorMessage("Debug script required", event);
                }
            });

        } else {
            showErrorMessage("Remote debug not allowed", event);
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
                cmd = SHELL_IMPORTS + cmd;
                result = groovyShell.evaluate(cmd);
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
}
