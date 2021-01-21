package hellfrog.commands.cmdline;

import com.vdurmont.emoji.EmojiParser;
import hellfrog.common.LongEmbedMessage;
import hellfrog.core.ServerSideResolver;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class EffectiveRightsCommand
        extends BotCommand {

    private static final String PREFIX = "eff";
    private static final String DESCRIPTION = "Get effective rights for users";

    private final Option userOption = Option.builder("u")
            .longOpt("user")
            .hasArgs()
            .valueSeparator(',')
            .argName("User")
            .desc("Select user for rights check")
            .build();

    private final Option channelOption = Option.builder("t")
            .longOpt("channel")
            .hasArgs()
            .optionalArg(true)
            .argName("Channel")
            .desc("Select text channel for check rights. If not arguments - use this text channel.")
            .build();

    public EffectiveRightsCommand() {
        super(PREFIX, DESCRIPTION);
        addCmdlineOption(userOption, channelOption);
        super.setAdminCommand();
        super.enableOnlyServerCommandStrict();
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
                                                   CommandLine cmdline,
                                                   ArrayList<String> cmdlineArgs,
                                                   TextChannel channel,
                                                   MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {

        if (!canExecuteServerCommand(event, server)) {
            showAccessDeniedServerMessage(event);
            return;
        }

        String[] rawUsers = cmdline.getOptionValues(userOption.getOpt());
        String[] rawChannels = cmdline.getOptionValues(channelOption.getOpt());

        if (rawUsers == null || rawUsers.length == 0) {
            showErrorMessage("Users required", event);
            return;
        }

        ServerSideResolver.ParseResult<User> foundUsers =
                ServerSideResolver.resolveUsersList(server, Arrays.asList(rawUsers));
        if (foundUsers.hasNotFound()) {
            showErrorMessage("Users not found: " + foundUsers.getNotFoundStringList(), event);
            return;
        }

        List<ServerTextChannel> channels;
        if (rawChannels != null && rawChannels.length > 0) {
            ServerSideResolver.ParseResult<ServerTextChannel> foundChannels =
                    ServerSideResolver.resolveTextChannelsList(server, Arrays.asList(rawChannels));
            if (foundChannels.hasNotFound()) {
                showErrorMessage("Channels not found: " + foundChannels.getNotFoundStringList(),
                        event);
                return;
            }
            channels = foundChannels.getFound();
        } else {
            channels = new ArrayList<>(1);
            channels.add((ServerTextChannel) channel);
        }

        LongEmbedMessage resultMessage = LongEmbedMessage.withTitleInfoStyle("Effective rights");

        foundUsers.getFound().forEach(user -> {
            resultMessage.append("User", MessageDecoration.BOLD)
                    .append(" ")
                    .appendReadable(server.getDisplayName(user), server)
                    .append(":")
                    .appendNewLine();
            channels.forEach(textChannel -> {
                resultMessage.append(". ")
                        .append("Channel", MessageDecoration.ITALICS)
                        .append(" ")
                        .append(textChannel)
                        .append(":")
                        .appendNewLine();
                Permissions permissions = textChannel.getEffectivePermissions(user);
                Optional<String> allowed = ServerSideResolver.getAllowedGrants(permissions);
                Optional<String> denied = ServerSideResolver.getDeniedGrants(permissions);
                if (allowed.isEmpty() && denied.isEmpty()) {
                    resultMessage.append(".  ")
                            .append(EmojiParser.parseToUnicode(":question:"))
                            .append("The user does not have any specified channel effective permissions.")
                            .appendNewLine();
                } else {
                    allowed.ifPresent(a -> resultMessage.append(".  ")
                            .append(EmojiParser.parseToUnicode(":white_check_mark:"))
                            .append("Allowed: ")
                            .append(a)
                            .appendNewLine());
                    denied.ifPresent(d -> resultMessage.append(".  ")
                            .append(EmojiParser.parseToUnicode(":x:"))
                            .append("Denied: ")
                            .append(d)
                            .appendNewLine());
                }
            });
        });

        super.showMessage(resultMessage, event);
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
    protected void executeCreateMessageEventDirect(CommandLine cmdline,
                                                   ArrayList<String> cmdlineArgs,
                                                   TextChannel channel,
                                                   MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {
        // директы поступать не будут, ограничено
    }
}
