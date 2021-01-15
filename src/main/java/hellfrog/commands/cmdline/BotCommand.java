package hellfrog.commands.cmdline;

import hellfrog.commands.ACLCommand;
import hellfrog.common.CodeSourceUtils;
import hellfrog.common.CommonUtils;
import hellfrog.settings.SettingsController;
import org.apache.commons.cli.*;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Основные функции команд бота
 */
public abstract class BotCommand
        extends ACLCommand {

    private static final int HELP_USAGE_WIDTH = 512;
    private static final List<BotCommand> ALL_COMMANDS =
            CodeSourceUtils.childClassInstancesCollector(BotCommand.class);
    final private Options control = new Options();
    private String helpUsage = null;
    private String footer = "";

    public BotCommand(String botPrefix, String description) {
        super(botPrefix, description);

        Option helpOption = Option.builder("h")
                .longOpt("help")
                .desc("Show help usage")
                .build();

        control.addOption(helpOption);
    }

    public static List<BotCommand> all() {
        return ALL_COMMANDS;
    }

    final void addCmdlineOption(Option... options) {
        for (Option option : options) {
            control.addOption(option);
        }
    }

    void setFooter(String footer) {
        if (!CommonUtils.isTrStringEmpty(footer))
            this.footer = footer;
    }

    private String generateHelpUsage() {
        HelpFormatter formatter = new HelpFormatter();

        final String botPrefix = SettingsController.getInstance()
                .getMainDBController()
                .getCommonPreferencesDAO()
                .getBotPrefix();
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter, true)) {
            formatter.printHelp(printWriter, HELP_USAGE_WIDTH, botPrefix + " " +
                            getPrefix(),
                    getCommandDescription(),
                    control, 1, 1, footer, true);
        }

        return stringWriter.toString();
    }

    /**
     * Выполнить команду при обработке события создания сообщения
     *
     * @param event        событие
     * @param rawCmdline   распарсенная командная строка
     * @param anotherLines остальные строки сообщения команды, не являющиеся
     *                     самой командой (расположены на новых строках)
     */
    public void executeCreateMessageEvent(@NotNull MessageCreateEvent event,
                                          @NotNull String[] rawCmdline,
                                          ArrayList<String> anotherLines) {

        super.updateLastUsage();

        if (hasRateLimits(event)) {
            return;
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
                    if (!super.isOnlyServerCommand()) {
                        executeCreateMessageEventDirect(cmdline, cmdlineArgs, channel, event, anotherLines);
                    } else {
                        showErrorMessage("This command can't be run into private channel", event);
                    }
                }
            }
        } catch (ParseException err) {
            String errMsg = "Unable to parse command arguments: " + err.getMessage();
            showErrorMessage(errMsg, event);
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

    private ArrayList<String> cleanArgsDelimiter(@NotNull String[] args) {
        ArrayList<String> result = new ArrayList<>(args.length);
        for (String item : args) {
            if (!item.equals("--"))
                result.add(item);
        }
        return result;
    }

    List<String> getOptionalArgsList(CommandLine cmdline, char arg) {
        String[] values = cmdline.getOptionValues(arg);
        return values != null && values.length > 0
                ? Arrays.asList(values)
                : new ArrayList<>(0);
    }
}
