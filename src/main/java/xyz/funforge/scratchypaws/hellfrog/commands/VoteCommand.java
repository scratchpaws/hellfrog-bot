package xyz.funforge.scratchypaws.hellfrog.commands;

import com.vdurmont.emoji.EmojiParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import xyz.funforge.scratchypaws.hellfrog.common.CommonUtils;
import xyz.funforge.scratchypaws.hellfrog.common.MessageUtils;
import xyz.funforge.scratchypaws.hellfrog.core.ServerSideResolver;
import xyz.funforge.scratchypaws.hellfrog.core.VoteController;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;
import xyz.funforge.scratchypaws.hellfrog.settings.old.ActiveVote;
import xyz.funforge.scratchypaws.hellfrog.settings.old.VotePoint;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class VoteCommand
        extends BotCommand {

    private static final String BOT_PREFIX = "vote";
    private static final String DESCRIPTION = "Voting management.";
    private static final String FOOTER = "When specifying voting points, they can be separated " +
            "both by commas (if the separation is without spaces) or by spaces. If the voting " +
            "point is longer than one word, it must be enclosed in quotes. Commas are not " +
            "allowed inside the voting point. It is necessary to specify the descriptive " +
            "text after double dashes and spaces.";
    private final VoteController voteController;

    public VoteCommand() {
        super(BOT_PREFIX, DESCRIPTION);
        super.enableOnlyServerCommandStrict();
        super.enableStrictByChannels();
        this.voteController = SettingsController.getInstance()
                .getVoteController();

        Option timeoutOption = Option.builder("t")
                .hasArg()
                .argName("Minutes")
                .longOpt("timeout")
                .desc("Set the polling timeout in minutes.")
                .build();

        Option emojiOptions = Option.builder("p")
                .hasArgs()
                .argName("Reaction|Point")
                .longOpt("points")
                .desc("Specify voting emoji and voting pints. Comma separated. All emoji must be known by server. " +
                        "The number of emoji must match the number of points of voting.")
                .valueSeparator(',')
                .build();

        Option interruptOption = Option.builder("i")
                .hasArg()
                .argName("Vote ID or \"all\"")
                .longOpt("interrupt")
                .desc("Abort voting with the specified ID. Or interrupt all votes.")
                .build();

        Option rolesFilter = Option.builder("r")
                .hasArgs()
                .argName("Roles")
                .longOpt("role")
                .desc("Only respond to users with specified roles")
                .valueSeparator(',')
                .build();

        Option listOption = Option.builder("l")
                .longOpt("list")
                .desc("List current active votes")
                .build();

        Option chatName = Option.builder("c")
                .longOpt("channel")
                .hasArg()
                .argName("Text channel")
                .desc("Text channel where the poll will be displayed")
                .build();

        Option singleChoose = Option.builder("s")
                .longOpt("single")
                .desc("Single choice (only one option allowed)")
                .build();

        Option defaultPoint = Option.builder("d")
                .longOpt("default")
                .desc("Use first vote point as default if no choose")
                .build();

        Option winThreshold = Option.builder("w")
                .longOpt("win")
                .hasArg()
                .argName("threshold")
                .desc("Set threshold for single choice with default vote point and second point. " +
                        "It also automatically activates the default vote point option.")
                .build();

        super.addCmdlineOption(timeoutOption, emojiOptions,
                interruptOption, rolesFilter, listOption, chatName,
                singleChoose, defaultPoint, winThreshold);
        super.setFooter(FOOTER);
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

        SettingsController settingsController = SettingsController.getInstance();

        boolean listMode = cmdline.hasOption('l');

        boolean hasTimeout = cmdline.hasOption('t');
        if (hasTimeout && !CommonUtils.isLong(cmdline.getOptionValue('t'))) {
            showErrorMessage("Timeout must be a number", channel);
            return;
        }
        long timeout = hasTimeout ? CommonUtils.onlyNumbersToLong(cmdline.getOptionValue('t')) : 0;
        if (hasTimeout && timeout <= 0) {
            showErrorMessage("Timeout must be a positive number great that zero", channel);
            return;
        }

        boolean interruptMode = cmdline.hasOption('i');
        boolean interruptAll = CommonUtils.safeEqualsTrimStr(cmdline.getOptionValue('i'), "all");
        short interruptId = (short) (interruptMode ? CommonUtils.onlyNumbersToLong(cmdline.getOptionValue('i')) : 0);

        if (interruptMode && !CommonUtils.isLong(cmdline.getOptionValue('i')) && !interruptAll) {
            showErrorMessage("Vote ID must be a number or \"all\"", channel);
            return;
        }

        if (interruptMode && interruptId <= 0 && !interruptAll) {
            showErrorMessage("Vote ID must be a positive number great that zero", channel);
            return;
        }

        boolean rolesFilter = cmdline.hasOption('r');
        boolean hasPoints = cmdline.hasOption('p');

        boolean specifiedChannel = cmdline.hasOption('c');
        boolean singleChooseMode = cmdline.hasOption('s');

        boolean hasWinThreshold = cmdline.hasOption('w') && !CommonUtils.isTrStringEmpty(cmdline.getOptionValue('w'));
        long winThreshold = CommonUtils.onlyNumbersToLong(cmdline.getOptionValue('w'));
        if (hasWinThreshold && winThreshold <= 0) {
            showErrorMessage("Win threshold parameter must be a positive number great that zero", channel);
            return;
        }
        boolean defaultChoose = cmdline.hasOption('d') || hasWinThreshold;

        if ((interruptMode || listMode) && (hasPoints || rolesFilter || hasTimeout || specifiedChannel
                || singleChooseMode || defaultChoose)) {
            showErrorMessage("Cannot specify different modes and it arguments " +
                    "of the command at the same time.", channel);
            return;
        }

        if (listMode) {
            List<ActiveVote> activeVotes = settingsController
                    .getServerPreferences(server.getId())
                    .getActiveVotes();

            MessageBuilder resultMessage = new MessageBuilder();
            if (activeVotes.size() > 0) {
                resultMessage.append("Active votes:")
                        .appendNewLine();
            } else {
                resultMessage.append("No active votes");
            }

            activeVotes.stream()
                    .filter((vote) -> !(vote.getMessageId() == null || vote.getTextChatId() == null))
                    .forEachOrdered((vote) -> {
                        Optional<ServerTextChannel> mayBeChannel = server.getTextChannelById(vote.getTextChatId());
                        Instant estimate = vote.isHasTimer() ? Instant.ofEpochSecond(vote.getEndDate()) : Instant.now();
                        Instant currentTime = Instant.now();
                        long estimateMinutes = ChronoUnit.MINUTES.between(currentTime, estimate);
                        if (estimateMinutes <= 0) {
                            estimateMinutes = 0;
                        }
                        String estimateMst = vote.isHasTimer() ? ", estimate in " + estimateMinutes + "min., " : ", ";
                        String channelTag = "[channel not found]";
                        boolean removeVote = false;
                        boolean messageIsExists = false;
                        String voteUrl = "";
                        if (mayBeChannel.isPresent()) {
                            ServerTextChannel textChannel = mayBeChannel.get();
                            channelTag = textChannel.getMentionTag();

                            try {
                                textChannel.getMessageById(vote.getMessageId()).join();
                                messageIsExists = true;

                                voteUrl = "https://discordapp.com/channels/" +
                                        server.getId() + "/" +
                                        textChannel.getId() + "/" +
                                        vote.getMessageId();

                            } catch (Exception err) {
                                removeVote = true;
                            }
                        } else {
                            removeVote = true;
                        }
                        resultMessage.append("* into channel ")
                                .append(channelTag)
                                .append(": id - ")
                                .append(vote.getId())
                                .append(estimateMst)
                                .append("vote text - ")
                                .append(vote.getReadableVoteText())
                                .append(vote.isExceptionalVote() ? " [single choice]" : "")
                                .append(vote.isWithDefaultPoint() ? " [with default point]" : "")
                                .append(vote.getWinThreshold() > 0 ? " [win threshold: " +
                                        vote.getWinThreshold() + "]" : "")
                                .append(". ");
                        if (mayBeChannel.isPresent() && !messageIsExists) {
                            resultMessage.append(" Vote message cannot be found. " +
                                            "Deleted from active votes.",
                                    MessageDecoration.BOLD);
                        } else if (mayBeChannel.isEmpty()) {
                            resultMessage.append(" Text channel cannot be found. " +
                                            "Deleted from active votes.",
                                    MessageDecoration.BOLD);
                        }
                        if (removeVote) {
                            settingsController.getServerPreferences(server.getId())
                                    .getActiveVotes()
                                    .remove(vote);
                        }
                        if (!CommonUtils.isTrStringEmpty(voteUrl)) {
                            resultMessage.appendNewLine()
                                    .append("URL: ")
                                    .append(voteUrl);
                        }
                        resultMessage.appendNewLine();
                    });

            MessageUtils.sendLongMessage(resultMessage, channel);
        } else if (interruptMode) {
            List<ActiveVote> activeVotes = settingsController.getServerPreferences(server.getId())
                    .getActiveVotes();
            if (interruptAll) {
                CompletableFuture.runAsync(() ->
                        activeVotes.forEach(v -> {
                            int cnt = 0;
                            if (canExecuteServerCommand(event, server, v.getTextChatId())) {
                                voteController.interruptVote(server.getId(), v.getId());
                            } else {
                                cnt++;
                            }
                            if (cnt > 0) {
                                showErrorMessage("Some votes cannot be interrupted " +
                                                "(access denied)",
                                        channel);
                            }
                        })
                );
            } else {
                boolean found = false;
                for (ActiveVote activeVote : activeVotes) {
                    if (activeVote.getId() == interruptId) {
                        found = true;
                        if (canExecuteServerCommand(event, server, activeVote.getTextChatId())) {
                            voteController.interruptVote(server.getId(), interruptId);
                        } else {
                            showAccessDeniedServerMessage(channel);
                            return;
                        }
                    }
                }
                if (!found) {
                    showErrorMessage("Unable to find a vote with the specified ID", channel);
                }
            }
        } else {

            Optional<ServerTextChannel> targetChannelOpt = event.getServerTextChannel();
            if (specifiedChannel) {
                targetChannelOpt = ServerSideResolver.resolveChannel(server, cmdline.getOptionValue('c'));
            }
            ServerTextChannel targetChannel;
            if (targetChannelOpt.isPresent()) {
                targetChannel = targetChannelOpt.get();
            } else {
                showErrorMessage("Unable to resolve target server text channel", channel);
                return;
            }

            if (!canExecuteServerCommand(event, server, targetChannel.getId())) {
                showAccessDeniedServerMessage(channel);
                return;
            }

            if (cmdlineArgs.isEmpty() && anotherLines.isEmpty()) {
                showErrorMessage("Descriptive text must be specified", channel);
                return;
            }

            List<String> rawRoles = new ArrayList<>(rolesFilter ? cmdline.getOptionValues('r').length : 0);
            if (rolesFilter) {
                rawRoles.addAll(Arrays.asList(cmdline.getOptionValues('r')));
            }

            List<String> rawPoints = new ArrayList<>(hasPoints ? cmdline.getOptionValues('p').length : 0);
            if (hasPoints) {
                rawPoints.addAll(Arrays.asList(cmdline.getOptionValues('p')));
            }
            if (hasPoints && (rawPoints.size() < 2 || rawPoints.size() % 2 != 0)) {
                showErrorMessage("The number of emoji must match the number of points of voting.", channel);
                return;
            }

            ServerSideResolver.ParseResult<Role> parsedRoles = ServerSideResolver.resolveRolesList(server, rawRoles);
            if (parsedRoles.hasNotFound()) {
                showErrorMessage("The specified roles are not found: " +
                        parsedRoles.getNotFoundStringList(), channel);
                return;
            }

            List<VotePoint> votePoints = new ArrayList<>(rawPoints.size() / 2 + 1);
            Map<Long, KnownCustomEmoji> foundCustomEmoji = new HashMap<>();

            if (!hasPoints) {
                VotePoint positive = new VotePoint();
                positive.setId(2L);
                positive.setEmoji(EmojiParser.parseToUnicode(":thumbsup:"));
                positive.setPointText("Yes");
                votePoints.add(positive);

                VotePoint negative = new VotePoint();
                negative.setId(1L);
                negative.setEmoji(EmojiParser.parseToUnicode(":thumbsdown:"));
                negative.setPointText("No");
                votePoints.add(negative);
            } else {
                long counter = 1L;
                for (int i = 0; i < rawPoints.size(); i += 2) {
                    VotePoint votePoint = new VotePoint();
                    votePoint.setId(counter);

                    String pointText = rawPoints.get(i + 1);
                    votePoint.setPointText(pointText);

                    String rawEmoji = rawPoints.get(i);

                    List<String> naturalEmoji = EmojiParser.extractEmojis(rawEmoji);
                    if (naturalEmoji.isEmpty()) {
                        Optional<KnownCustomEmoji> customEmoji = ServerSideResolver.resolveCustomEmoji(server, rawEmoji);
                        if (customEmoji.isPresent()) {
                            KnownCustomEmoji custom = customEmoji.get();
                            votePoint.setCustomEmoji(custom.getId());
                            foundCustomEmoji.put(custom.getId(), custom);
                        } else {
                            showErrorMessage("Unable to find custom emoji " + rawEmoji + " on server", channel);
                            return;
                        }
                    } else {
                        votePoint.setEmoji(naturalEmoji.get(0));
                    }

                    for (VotePoint another : votePoints) {
                        boolean duplicateFound = false;
                        if (another.getCustomEmoji() != null && votePoint.getCustomEmoji() != null) {
                            if (another.getCustomEmoji().equals(votePoint.getCustomEmoji()))
                                duplicateFound = true;
                        }
                        if (another.getEmoji() != null && votePoint.getEmoji() != null) {
                            if (another.getEmoji().equals(votePoint.getEmoji()))
                                duplicateFound = true;
                        }
                        if (duplicateFound) {
                            showErrorMessage("Emoji duplicates found", channel);
                            return;
                        }
                    }
                    votePoints.add(votePoint);
                    counter++;
                }
            }

            if (defaultChoose && votePoints.size() < 2) {
                showErrorMessage("Voting with the default vote point should have " +
                        "two or more vote points", channel);
                return;
            }

            if (hasWinThreshold && votePoints.size() != 2) {
                showErrorMessage("Voting with the winner threshold " +
                        "should have only two vote points", channel);
                return;
            }

            Instant currentTime = Instant.now();
            Instant futureTime = ChronoUnit.MINUTES.addTo(currentTime, timeout);
            long endDate = futureTime.getEpochSecond();

            ActiveVote newVote = new ActiveVote();
            newVote.setEndDate(endDate);
            newVote.setHasTimer(hasTimeout);
            newVote.setRolesFilter(parsedRoles.getFound()
                    .stream()
                    .map(Role::getId)
                    .collect(Collectors.toList()));
            newVote.setReadableVoteText("");
            final StringBuilder readableVoteText = new StringBuilder();
            cmdlineArgs.stream()
                    .reduce((c1, c2) -> c1 + " " + c2)
                    .ifPresent(readableVoteText::append);
            anotherLines.stream()
                    .reduce((line1, line2) -> line1 + '\n' + line2)
                    .ifPresent(out -> {
                        readableVoteText.append('\n');
                        readableVoteText.append(out);
                    });
            newVote.setReadableVoteText(readableVoteText.toString());
            newVote.setVotePoints(votePoints);
            newVote.setMessageId(null);
            newVote.setTextChatId(null);
            newVote.setExceptionalVote(singleChooseMode);
            newVote.setWinThreshold(winThreshold);
            newVote.setWithDefaultPoint(defaultChoose);

            MessageBuilder resultMessage = new MessageBuilder()
                    .append(newVote.getReadableVoteText())
                    .appendNewLine();
            boolean skipFirstDefault = defaultChoose;
            for (VotePoint votePoint : votePoints) {
                if (skipFirstDefault) {
                    skipFirstDefault = false;
                    continue;
                }
                resultMessage.append("  - ")
                        .append(votePoint.buildVoteString(foundCustomEmoji))
                        .appendNewLine();
            }
            if (defaultChoose && votePoints.size() > 0) {
                VotePoint voteDefault = votePoints.get(0);
                resultMessage.append("default: ")
                        .append(voteDefault.buildVoteString(foundCustomEmoji))
                        .appendNewLine();
            }
            if (hasWinThreshold && winThreshold > 0) {
                resultMessage.append("Selectable voting point will ")
                        .append("WIN", MessageDecoration.BOLD)
                        .append(" if it dial ")
                        .append(String.valueOf(winThreshold), MessageDecoration.BOLD)
                        .append(" votes.")
                        .appendNewLine();
            }
            try {
                Message msg = resultMessage.send(targetChannel).join();
                skipFirstDefault = defaultChoose;
                for (VotePoint votePoint : votePoints) {
                    if (skipFirstDefault) {
                        skipFirstDefault = false;
                        continue;
                    }
                    if (votePoint.getEmoji() != null) {
                        msg.addReaction(votePoint.getEmoji());
                    } else {
                        msg.addReaction(foundCustomEmoji.get(votePoint.getCustomEmoji()));
                    }
                }
                newVote.setMessageId(msg.getId());
                newVote.setTextChatId(targetChannel.getId());

                while (true) {
                    search_unique_id_cycle:
                    {
                        int rnd = ThreadLocalRandom.
                                current()
                                .nextInt(32676);
                        short newId = (short) Math.abs(rnd);
                        List<ActiveVote> anotherVotes = settingsController.getServerPreferences(server.getId())
                                .getActiveVotes();
                        for (ActiveVote another : anotherVotes) {
                            if (another.getId() == newId)
                                break search_unique_id_cycle;
                        }
                        newVote.setId(newId);
                        break;
                    }
                }

                settingsController.getServerPreferences(server.getId())
                        .getActiveVotes()
                        .add(newVote);
                settingsController.saveServerSideParameters(server.getId());

                String voteUrl = "https://discordapp.com/channels/" +
                        server.getId() + "/" +
                        newVote.getTextChatId() + "/" +
                        newVote.getMessageId();

                showInfoMessage("Vote created: " + voteUrl, channel);
            } catch (Exception err) {
                showErrorMessage("Unable to create vote: " + err.getMessage(), channel);
            }
        }
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
        // сюда не доходят
    }
}
