package xyz.funforge.scratchypaws.hellfrog.commands;

import besus.utils.collection.Sequental;
import org.apache.commons.cli.*;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.Messageable;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;
import xyz.funforge.scratchypaws.hellfrog.common.CodeSourceUtils;
import xyz.funforge.scratchypaws.hellfrog.common.CommonUtils;
import xyz.funforge.scratchypaws.hellfrog.core.AccessControlCheck;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Основные функции команд бота
 */
public abstract class BotCommand {
    private static final int ERROR_MESSAGE = 0;
    private static final int INFO_MESSAGE = 1;
    private static final int HELP_USAGE_WIDTH = 512;
    private static final List<BotCommand> ALL_COMMANDS =
            CodeSourceUtils.childClassInstancesCollector(BotCommand.class);
    private final String prefix;
    private final String description;
    final private Options control = new Options();
    private String helpUsage = null;
    private boolean strictByChannels = false;
    private boolean onlyServerCommand = false;
    private String footer = "";
    private boolean updateLastUsage = true;

    public BotCommand(String botPrefix, String description) {
        this.prefix = botPrefix;
        this.description = description;

        Option helpOption = Option.builder("h")
                .longOpt("help")
                .desc("Show help usage")
                .build();

        control.addOption(helpOption);
    }

    public static Sequental<BotCommand> all() {
        return Sequental.of(ALL_COMMANDS)
                .repeatable();
    }

    final void addCmdlineOption(Option... options) {
        for (Option option : options) {
            control.addOption(option);
        }
    }

    final void enableStrictByChannels() {
        this.strictByChannels = true;
    }

    final void enableOnlyServerCommandStrict() {
        this.onlyServerCommand = true;
    }

    final void disableUpdateLastCommandUsage() {
        this.updateLastUsage = false;
    }

    void setFooter(String footer) {
        if (!CommonUtils.isTrStringEmpty(footer))
            this.footer = footer;
    }

    private String generateHelpUsage() {
        HelpFormatter formatter = new HelpFormatter();

        StringWriter stringWriter = new StringWriter();
        SettingsController settingsController = SettingsController.getInstance();
        try (PrintWriter printWriter = new PrintWriter(stringWriter, true)) {
            formatter.printHelp(printWriter, HELP_USAGE_WIDTH, settingsController.getGlobalCommonPrefix() + " " +
                            getPrefix(),
                    getCommandDescription(),
                    control, 1, 1, footer, true);
        }

        return stringWriter.toString();
    }

    /**
     * Получить префикс команды для её вызова
     *
     * @return префикс команды
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Выполнить команду при обработке события создания сообщения
     *
     * @param event        событие
     * @param rawCmdline   распарсенная командная строка
     * @param anotherLines остальные строки сообщения команды, не являющиеся
     *                     самой командой (расположены на новых строках)
     */
    public void executeCreateMessageEvent(MessageCreateEvent event, String[] rawCmdline, ArrayList<String> anotherLines) {

        if (updateLastUsage) {
            SettingsController.getInstance().updateLastCommandUsage();
        }

        TextChannel channel = event.getChannel();
        if (rawCmdline.length < 2) {
            showHelp(event);
            return;
        }

        rawCmdline = Arrays.copyOfRange(rawCmdline, 1, rawCmdline.length);
        CommandLineParser parser = new DefaultParser();
        Optional<Server> mayBeServer = event.getServer();

        try {
            CommandLine cmdline = parser.parse(control, rawCmdline);
            ArrayList<String> cmdlineArgs = cleanArgsDelimiter(cmdline.getArgs());

            if (cmdline.hasOption('h')) {
                showHelp(event);
            } else {
                if (mayBeServer.isPresent()) {
                    Server server = mayBeServer.get();
                    executeCreateMessageEventServer(server, cmdline, cmdlineArgs, channel, event, anotherLines);
                } else {
                    if (!onlyServerCommand) {
                        executeCreateMessageEventDirect(cmdline, cmdlineArgs, channel, event, anotherLines);
                    } else {
                        showErrorMessage("This command can't be run into private channel", channel);
                    }
                }
            }
        } catch (ParseException err) {
            String errMsg = "Unable to parse command arguments: " + err.getMessage();
            showErrorMessage(errMsg, channel);
        }
    }

    /**
     * Обработка команды, поступившей из текстового канала сервера.
     *
     * @param server       Сервер текстового канала, откуда поступило сообщение
     * @param cmdline      обработанные аргументы командной строки
     * @param cmdlineArgs  оставшиеся значения командной строки
     * @param channel      текстовый канал, откуда поступило сообщение
     * @param event        событие сообщения
     * @param anotherLines другие строки в команде, не относящиеся к команде
     */
    protected abstract void executeCreateMessageEventServer(Server server,
                                                            CommandLine cmdline,
                                                            ArrayList<String> cmdlineArgs,
                                                            TextChannel channel,
                                                            MessageCreateEvent event,
                                                            ArrayList<String> anotherLines);

    /**
     * Обработка команды, поступившей из привата.
     *
     * @param cmdline      обработанные аргументы командной строки
     * @param cmdlineArgs  оставшиеся значения командной строки
     * @param channel      текстовый канал, откуда поступило сообщение
     * @param event        событие сообщения
     * @param anotherLines другие строки в команде, не относящиеся к команде
     */
    protected abstract void executeCreateMessageEventDirect(CommandLine cmdline,
                                                            ArrayList<String> cmdlineArgs,
                                                            TextChannel channel,
                                                            MessageCreateEvent event,
                                                            ArrayList<String> anotherLines);

    boolean canExecuteServerCommand(MessageCreateEvent event, Server server,
                                    long... anotherTargetChannel) {
        return AccessControlCheck.canExecuteOnServer(getPrefix(), event, server, strictByChannels, anotherTargetChannel);
    }

    boolean canExecuteGlobalCommand(@NotNull MessageCreateEvent event) {
        return AccessControlCheck.canExecuteGlobalCommand(event);
    }

    /**
     * Показывает краткое описание выполняемой функции команды.
     *
     * @return краткое описание
     */
    public String getCommandDescription() {
        return description;
    }

    /**
     * Показывает справку по использованию команды.
     *
     * @return справка по использованию команды.
     */
    private String getCommandHelpUsage() {
        if (helpUsage == null)
            helpUsage = generateHelpUsage();

        return helpUsage;
    }

    private void showHelp(MessageCreateEvent event) {
        Optional<User> mayBeUser = event.getMessageAuthor().asUser();
        TextChannel channel = event.getChannel();
        if (mayBeUser.isPresent() && (channel instanceof ServerTextChannel)) {
            User user = mayBeUser.get();
            new MessageBuilder()
                    .appendCode("", getCommandHelpUsage())
                    .send(user);
        } else {
            new MessageBuilder()
                    .appendCode("", getCommandHelpUsage())
                    .send(channel);
        }
    }

    private ArrayList<String> cleanArgsDelimiter(String[] args) {
        ArrayList<String> result = new ArrayList<>(args.length);
        for (String item : args) {
            if (!item.equals("--"))
                result.add(item);
        }
        return result;
    }

    void showAccessDeniedGlobalMessage(Messageable messageable) {
        showErrorMessage("Only bot owners can do it.", messageable);
    }

    void showAccessDeniedServerMessage(Messageable messageable) {
        showErrorMessage("Only allowed users and roles can do it.", messageable);
    }

    void showErrorMessage(String textMessage, Messageable messageable) {
        showEmbedMessage(textMessage, messageable, ERROR_MESSAGE);
    }

    void showErrorMessageByRights(String textMessage, MessageCreateEvent event) {
        showEmbedMessageByRights(textMessage, event, ERROR_MESSAGE);
    }

    void showInfoMessageByRights(String textMessage, MessageCreateEvent event) {
        showEmbedMessageByRights(textMessage, event, INFO_MESSAGE);
    }

    void showInfoMessage(String textMessage, Messageable messageable) {
        showEmbedMessage(textMessage, messageable, INFO_MESSAGE);
    }

    private void showEmbedMessageByRights(String textMessage, MessageCreateEvent event, int type) {
        event.getMessageAuthor().asUser().ifPresent(user -> {
            event.getServer().ifPresent(server ->
                    event.getServerTextChannel().ifPresent(serverTextChannel -> {
                        if (server.isAdmin(user) || server.isOwner(user)) {
                            showEmbedMessage(textMessage, serverTextChannel, type);
                        } else {
                            showEmbedMessage(textMessage, user, type);
                        }
                    }));
            if (!event.isServerMessage()) {
                showEmbedMessage(textMessage, user, type);
            }
        });
    }

    private void showEmbedMessage(String textMessage, Messageable target, int type) {
        Color messageColor = Color.BLACK;
        switch (type) {
            case ERROR_MESSAGE:
                messageColor = Color.RED;
                break;

            case INFO_MESSAGE:
                messageColor = Color.CYAN;
                break;
        }
        User yourself = SettingsController.getInstance().getDiscordApi().getYourself();
        new MessageBuilder()
                .setEmbed(new EmbedBuilder()
                        .setColor(messageColor)
                        .setDescription(textMessage)
                        .setTimestampToNow()
                        .setFooter(type == ERROR_MESSAGE ? "This message will automatically delete in 20 seconds." : null)
                        .setAuthor(yourself))
                .send(target).thenAccept(m -> {
            if (type == ERROR_MESSAGE) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(20_000L);
                        if (m.canYouDelete())
                            m.delete();
                    } catch (InterruptedException ignore) {
                    }
                });
            }
        });
    }

    List<String> getOptionalArgsList(CommandLine cmdline, char arg) {
        String[] values = cmdline.getOptionValues(arg);
        return values != null && values.length > 0
                ? Arrays.asList(values)
                : new ArrayList<>(0);
    }
}
