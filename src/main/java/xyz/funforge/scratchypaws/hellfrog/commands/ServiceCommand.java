package xyz.funforge.scratchypaws.hellfrog.commands;

import bsh.Interpreter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.core.entity.permission.PermissionsImpl;
import xyz.funforge.scratchypaws.hellfrog.common.BroadCast;
import xyz.funforge.scratchypaws.hellfrog.common.CommonUtils;
import xyz.funforge.scratchypaws.hellfrog.common.MessageUtils;
import xyz.funforge.scratchypaws.hellfrog.common.OptionalUtils;
import xyz.funforge.scratchypaws.hellfrog.core.ServerSideResolver;
import xyz.funforge.scratchypaws.hellfrog.reactions.DiceReaction;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

        Option secureTransfer = Option.builder("sec")
                .desc("Enable two-phase transfer from bot private")
                .hasArg()
                .optionalArg(true)
                .argName("text chat")
                .build();

        Option grabServerData = Option.builder("grab")
                .desc("Grab server info data as possible")
                .hasArg()
                .optionalArg(true)
                .argName("server id")
                .build();

        super.addCmdlineOption(stopBot, memInfo, botDate, inviteUrl, runGc, runtimeShell, lastUsage,
                uploadFailRofl, uploadWinRofl, secureTransfer, grabServerData);

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
        boolean secureTransfer = cmdline.hasOption("sec");
        boolean grabServerData = cmdline.hasOption("grab");
        String transferChat = cmdline.getOptionValue("sec", "");
        String grabServerRawId = cmdline.getOptionValue("grab", "");

        if (stopAction ^ memInfo ^ getDate ^ getInvite ^ runGc ^ runtimeShell ^ lastUsageAction ^
                uploadWinRofl ^ uploadFailRofl ^ secureTransfer ^ grabServerData) {

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
                        .send(channel);
            }

            if (getDate) {
                new MessageBuilder()
                        .setEmbed(new EmbedBuilder()
                                .setTitle("Current date and time: ")
                                .setDescription(CommonUtils.getCurrentGmtTimeAsString())
                                .setTimestampToNow())
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
                            .setEmbed(new EmbedBuilder()
                                    .setTimestampToNow()
                                    .setTitle("Last usage")
                                    .setDescription("No last usage found"))
                            .send(channel);
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
                            .send(channel);
                }
            }

            if (uploadWinRofl || uploadFailRofl) {
                uploadRoflAction(event, uploadWinRofl);
            }

            if (secureTransfer) {
                MessageUtils.deleteMessageIfCan(event.getMessage());
                event.getServer().ifPresent(s -> {
                    Optional<ServerTextChannel> mayBeChannel =
                            ServerSideResolver.resolveChannel(s, transferChat);
                    OptionalUtils.ifPresentOrElse(mayBeChannel, ch -> {
                        settingsController.setServerTransfer(s.getId());
                        settingsController.setServerTextChatTransfer(ch.getId());
                        settingsController.saveCommonPreferences();
                        new MessageBuilder()
                                .setEmbed(new EmbedBuilder()
                                        .setDescription("Enabled two-phase transfer from bot private" +
                                                " to text channel " + ch.getMentionTag())
                                        .setFooter("This message will be removed after 3 sec.")
                                        .setTimestampToNow())
                                .send(channel).thenAccept(msg1 -> {
                            try {
                                Thread.sleep(3_000L);
                            } catch (InterruptedException ignore) {
                            }
                            msg1.delete();
                        });
                    }, () -> {
                        settingsController.setServerTransfer(null);
                        settingsController.setServerTextChatTransfer(null);
                        settingsController.saveCommonPreferences();
                        new MessageBuilder()
                                .setEmbed(new EmbedBuilder()
                                        .setDescription("Two-phase transfer disabled")
                                        .setFooter("This message will be removed after 3 sec.")
                                        .setTimestampToNow())
                                .send(channel).thenAccept(msg1 -> {
                            try {
                                Thread.sleep(3_000L);
                            } catch (InterruptedException ignore) {
                            }
                            msg1.delete();
                        });
                    });
                });
            }

            if (grabServerData) {
                long serverId = 0L;
                if (!CommonUtils.isTrStringEmpty(grabServerRawId) && !CommonUtils.isLong(grabServerRawId)) {
                    showErrorMessage("Parameter must be a server id", channel);
                    return;
                }
                if (event.getServer().isPresent()) {
                    serverId = event.getServer().get().getId();
                }
                if (!CommonUtils.isTrStringEmpty(grabServerRawId) && CommonUtils.isLong(grabServerRawId)) {
                    serverId = CommonUtils.onlyNumbersToLong(grabServerRawId);
                }
                if (serverId == 0L) {
                    showErrorMessage("Server Id required", channel);
                    return;
                }

                event.getApi().getServerById(serverId).ifPresentOrElse(server ->
                                executeGrabServerInfo(server, channel),
                        () -> showErrorMessage("Server not found by this id", channel));
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
        settingsController.getServerListWithConfig()
                .forEach(settingsController::saveServerSideParameters);
        settingsController.getServerListWithStatistic()
                .forEach(settingsController::saveServerSideStatistic);

        User yourself = event.getApi().getYourself();
        String userData = "Stop called by " + event.getMessageAuthor().getId() +
                (event.getMessageAuthor().asUser()
                        .map(user -> " (" + user.getDiscriminatedName() + ") ")
                        .orElse(" "));
        new MessageBuilder()
                .setEmbed(new EmbedBuilder()
                        .setAuthor(yourself)
                        .setTimestampToNow()
                        .setTitle("WARNING")
                        .setDescription("Bot stopping NOW!")
                        .setFooter(userData))
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
            Optional<String> shellCmd = anotherLines.stream()
                    .reduce((s1, s2) -> s1 + " " + s2);

            OptionalUtils.ifPresentOrElse(shellCmd, cmd -> {
                final String command = cmd.replaceAll("`{3}", "");
                CompletableFuture.runAsync(() -> executeDebugCommand(command, channelId));
            }, new Thread(() -> {
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
        if (event.getMessageAttachments().isEmpty()) {
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

    private void executeGrabServerInfo(final Server server, final TextChannel channel) {
        StringBuilder sw = new StringBuilder();
        MessageBuilder res = new MessageBuilder();
        sw.append("--- Members list: ---\n");
        User owner = server.getOwner();
        for (User member : server.getMembers()) {
            String displayName = member.getDisplayName(server);
            String id = member.getIdAsString();
            String discriminatedName = member.getDiscriminatedName();
            sw.append(displayName).append('\n')
                    .append("  full name: ").append(discriminatedName).append('\n')
                    .append("  id: ").append(id).append('\n');
            if (owner.equals(member))
                sw.append("  This user is a server owner\n");
            server.getRoles(member)
                    .stream()
                    .map(Role::getName)
                    .filter(r -> !r.equals("@everyone"))
                    .reduce((r1, r2) -> r1 + ", " + r2)
                    .ifPresent(r -> sw.append("  roles: ").append(r).append('\n'));
        }
        sw.append("--- Roles list: ---\n");
        List<Role> roles = new ArrayList<>(server.getRoles());
        roles.sort((o1, o2) -> o2.getPosition() - o1.getPosition());
        for (Role role : roles) {
            String name = role.getName();
            String id = role.getIdAsString();
            sw.append(name).append('\n')
                    .append("  id: ").append(id).append('\n');
            role.getColor()
                    .ifPresent(c ->
                            sw.append("  color: #")
                                    .append(Integer.toHexString(c.getRed()))
                                    .append(Integer.toHexString(c.getGreen()))
                                    .append(Integer.toHexString(c.getBlue()))
                                    .append('\n'));
            sw.append("  mention tag: ").append(role.getMentionTag()).append('\n');
            sw.append("  position: ").append(role.getPosition()).append('\n');
            sw.append("  grants:").append('\n');
            ServerSideResolver.getAllowedGrants(role).ifPresent(list -> {
                sw.append("    allowed:\n");
                for (String line : CommonUtils.addLinebreaks(list, 110).split("\n"))
                    sw.append("      ").append(line).append('\n');
            });
            ServerSideResolver.getDeniedGrants(role).ifPresent(list -> {
                sw.append("    denied: ").append(list).append('\n');
                for (String line : CommonUtils.addLinebreaks(list, 110).split("\n"))
                    sw.append("      ").append(line).append('\n');
            });
        }
        server.getCustomEmojis().stream()
                .map(KnownCustomEmoji::getMentionTag)
                .reduce(CommonUtils::reduceConcat)
                .ifPresent(list ->
                        sw.append("--- Emoji list: ---\n")
                                .append(CommonUtils.addLinebreaks(list, 120))
                );
        byte[] attach = sw.toString().getBytes(StandardCharsets.UTF_8);
        res.addAttachment(attach, "MemberList.txt");
        res.send(channel);
    }
}
