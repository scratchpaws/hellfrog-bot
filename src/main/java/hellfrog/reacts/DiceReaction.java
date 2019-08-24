package hellfrog.reacts;

import com.vdurmont.emoji.EmojiParser;
import hellfrog.common.BroadCast;
import hellfrog.common.CommonUtils;
import hellfrog.common.MessageUtils;
import hellfrog.core.ServerSideResolver;
import hellfrog.settings.SettingsController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiceReaction
        extends MsgCreateReaction {

    private static final String SHORT_ROLL_PREFIX = "([lLдД]{2})";
    private static final Pattern DICE_PATTERN = Pattern.compile(
            "([rр]\\s?)?" +
                    "([dд]?\\s?\\d+\\s?[dд]\\s?\\d+|[lд]{2})" +
                    "(\\s?([=<>]{1,2}|[+\\-])\\s?\\d+)*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SEARCH_PATTERN = Pattern.compile(
            "^\\s*" +
                    "([rр]\\s?)?" +
                    "([dд]?\\s?\\d+\\s?[dд]\\s?\\d+|[lд]{2})" +
                    "(\\s?([=<>]{1,2}|[+\\-])\\s?\\d+)*",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final String DEFAULT_ROLL = "1d20";

    private static final Pattern ROFL_PATTERN = Pattern.compile("^[rр]",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern VALUE_PATTERN = Pattern.compile(
            "\\d{1,5}\\s?[dд]\\s?\\d{1,5}",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern MODIFIER_PATTERN = Pattern.compile(
            "[+\\-]\\s?\\d{1,5}",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern FILTER_PATTERN = Pattern.compile(
            "[=<>]{1,2}\\s?\\d{1,5}",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final List<String> LOW_ROFL_LIST = new CopyOnWriteArrayList<>();
    private static final List<String> HIGH_ROFL_LIST = new CopyOnWriteArrayList<>();
    private static final long MAX_DICES = 100L;
    private static final long MAX_FACES = 1000L;

    private static final String PREFIX = "dice";
    private static final String DESCRIPTION = "roll dices (use NdN or ll for roll, " +
            "sample: 1d20, ll. \"r\" prefix add pictures for low and high values.";

    private static final Logger log = LogManager.getLogger(DiceReaction.class.getSimpleName());

    public DiceReaction() {
        super.setCommandPrefix(PREFIX);
        super.setCommandDescription(DESCRIPTION);
        super.enableAccessControl();
        super.enableStrictByChannel();
    }

    @Override
    public boolean canReact(MessageCreateEvent event) {
        String messageString = event.getMessageContent();
        Matcher matcher = SEARCH_PATTERN.matcher(messageString);
        return matcher.find();
    }

    @Override
    void parallelExecuteReact(String strMessage, @Nullable Server server,
                              @Nullable User user, TextChannel textChannel,
                              Instant messageCreateDate, Message sourceMessage) {

        try {
            SettingsController.getInstance().updateLastCommandUsage();
            List<String> lines = Arrays.asList(strMessage.split("\n"));
            for (int i = 0; i < lines.size(); i++) {
                String currentLine = lines.get(i).trim();
                Matcher patternMatcher = DICE_PATTERN.matcher(currentLine);
                if (patternMatcher.find()) {
                    String diceValue = patternMatcher.group()
                            .replaceAll(SHORT_ROLL_PREFIX, DEFAULT_ROLL);
                    String anotherString = EmojiParser.parseToUnicode(":game_die: ")
                            + CommonUtils.cutLeftString(currentLine, patternMatcher.group(0)).trim();
                    if ((i + 1) < lines.size()) {
                        Optional<String> anotherLast = lines.subList(i + 1, lines.size())
                                .stream()
                                .reduce((l1, l2) -> l1 + '\n' + l2);
                        if (anotherLast.isPresent()) {
                            anotherString += '\n' + anotherLast.get();
                        }
                    }

                    if (server != null) {
                        anotherString = ServerSideResolver.resolveMentions(server, anotherString);
                    }

                    boolean doRofl = ROFL_PATTERN.matcher(diceValue).find();
                    Matcher valueMatcher = VALUE_PATTERN.matcher(diceValue);
                    if (!valueMatcher.find()) return;
                    String rawDiceValues = valueMatcher.group();
                    String[] rawDiceArrayValues = rawDiceValues.split("[dDдД]");
                    long numOfDice = rawDiceArrayValues.length == 2
                            ? CommonUtils.onlyNumbersToLong(rawDiceArrayValues[0])
                            : 1L;
                    long numOfFaces = rawDiceArrayValues.length == 2
                            ? CommonUtils.onlyNumbersToLong(rawDiceArrayValues[1])
                            : 20L;

                    if (numOfDice > 0 && numOfDice <= MAX_DICES
                            && numOfFaces > 0 && numOfFaces <= MAX_FACES) {

                        List<SumModifier> resultModifiers = new ArrayList<>();
                        Matcher foundModifiers = MODIFIER_PATTERN.matcher(diceValue);
                        while (foundModifiers.find()) {
                            String rawModifier = foundModifiers.group();
                            boolean addition = rawModifier.startsWith("+");
                            long modifierValue = CommonUtils.onlyNumbersToLong(rawModifier);
                            if (modifierValue > 0) {
                                resultModifiers.add(new SumModifier(modifierValue, addition));
                            }
                        }

                        List<ResultFilter> resultFilters = new ArrayList<>();
                        Matcher filterModifiers = FILTER_PATTERN.matcher(diceValue);
                        while (filterModifiers.find()) {
                            String rawFilter = filterModifiers.group();
                            FilterType filterType = FilterType.NOP;
                            if (rawFilter.startsWith(">=")) {
                                filterType = FilterType.GE;
                            } else if (rawFilter.startsWith("<=")) {
                                filterType = FilterType.LE;
                            } else if (rawFilter.startsWith("<>")) {
                                filterType = FilterType.NE;
                            } else if (rawFilter.startsWith("=")) {
                                filterType = FilterType.EQ;
                            } else if (rawFilter.startsWith("<")) {
                                filterType = FilterType.LT;
                            } else if (rawFilter.startsWith(">")) {
                                filterType = FilterType.GT;
                            }
                            long filterValue = CommonUtils.onlyNumbersToLong(rawFilter);
                            if (filterValue > 0 && !filterType.equals(FilterType.NOP)) {
                                resultFilters.add(new ResultFilter(filterType, filterValue));
                            }
                        }

                        MessageBuilder dicesOutput = new MessageBuilder();
                        long sumResult = 0;
                        ThreadLocalRandom currRnd = ThreadLocalRandom.current();
                        int success = 0;
                        for (long d = 1; d <= numOfDice; d++) {
                            long tr = currRnd.nextLong(1L, numOfFaces + 1);
                            boolean strike = false;
                            if (!resultFilters.isEmpty()) {
                                for (ResultFilter resultFilter : resultFilters) {
                                    if (resultFilter.notOk(tr)) {
                                        strike = true;
                                        break;
                                    }
                                }
                            }
                            if (!strike) {
                                success++;
                                sumResult += tr;
                                dicesOutput.append("[").append(tr).append("]");
                            } else {
                                dicesOutput.append("[", MessageDecoration.STRIKEOUT)
                                        .append(String.valueOf(tr), MessageDecoration.STRIKEOUT)
                                        .append("]", MessageDecoration.STRIKEOUT);
                            }
                        }
                        long modifiedSum = sumResult;
                        long commonModifier = 0L;
                        for (SumModifier resultModifier : resultModifiers) {
                            modifiedSum = resultModifier.modify(modifiedSum);
                            commonModifier = resultModifier.modify(commonModifier);
                        }
                        if (modifiedSum < 0L) {
                            modifiedSum = 0L;
                        }
                        if (!resultFilters.isEmpty()) {
                            dicesOutput.append(" (")
                                    .append(String.valueOf(success))
                                    .append(" success)");
                        }
                        long max = numOfDice * numOfFaces + 1;
                        String defName = "You";
                        if (user != null) {
                            defName = MessageUtils.escapeSpecialSymbols(server != null ?
                                    server.getDisplayName(user) :
                                    user.getName());
                        }
                        MessageBuilder msg = new MessageBuilder()
                                .append(defName, MessageDecoration.BOLD)
                                .append(" rolled ")
                                .append(String.valueOf(numOfDice))
                                .append("-")
                                .append(String.valueOf(max - 1))
                                .append(" and got ")
                                .append(dicesOutput.getStringBuilder().toString())
                                .append("...")
                                .append(String.valueOf(modifiedSum), MessageDecoration.BOLD);
                        if (!resultModifiers.isEmpty()) {
                            msg.append(" (")
                                    .append(sumResult)
                                    .append(commonModifier >= 0L ? "+" : "")
                                    .append(commonModifier)
                                    .append(")");
                        }
                        new MessageBuilder()
                                .setEmbed(new EmbedBuilder()
                                        .setTitle(anotherString)
                                        .setColor(Color.CYAN)
                                        .setDescription(msg.getStringBuilder().toString()))
                                .send(textChannel);
                        if (doRofl) {
                            rofling(textChannel, modifiedSum, modifiedSum < max ?
                                    max - 1 : modifiedSum);
                        }
                    }

                    break;
                }
            }
        } catch (Exception err) {
            log.error(err.getMessage(), err);
        }
    }

    private void rofling(final TextChannel textChannel, final long result, final long max) {
        if (!LOW_ROFL_LIST.isEmpty() && !HIGH_ROFL_LIST.isEmpty()) {
            long lowValue = CommonUtils.getLowValue(max);
            long highValue = CommonUtils.getHighValue(max);
            if (result > lowValue && result < highValue) return;
            List<String> where = result >= highValue ? HIGH_ROFL_LIST : LOW_ROFL_LIST;
            int selectedNumber = ThreadLocalRandom.current()
                    .nextInt(0, where.size());
            String imageUrl = where.get(selectedNumber);
            new MessageBuilder()
                    .setEmbed(new EmbedBuilder()
                            .setImage(imageUrl))
                    .send(textChannel);
        }
    }

    public static void rebuildRoflIndexes() {
        Optional<DiscordApi> mayBeApi = Optional.ofNullable(SettingsController.getInstance().getDiscordApi());
        mayBeApi.ifPresentOrElse(discordApi -> {
            List<String> highRoflUrls = new ArrayList<>();
            discordApi.getServerTextChannelById(HIGH_ROLL_IMAGES_CHANNEL).ifPresentOrElse(textChannel -> {
                if (textChannel.canYouReadMessageHistory()
                        && textChannel.canYouSee()) {
                    textChannel.getMessagesAsStream().forEach(message ->
                            message.getAttachments().forEach(messageAttachment ->
                                    highRoflUrls.add(messageAttachment.getUrl().toString())));
                    HIGH_ROFL_LIST.clear();
                    HIGH_ROFL_LIST.addAll(highRoflUrls);
                    BroadCast.sendServiceMessage("Found " + HIGH_ROFL_LIST.size()
                            + " URLs images into channel " + textChannel.getName());
                } else {
                    BroadCast.sendServiceMessage("Unable to see messages and history for " +
                            "channel " + textChannel.getName() + " (channel with high rofl images)");
                }
            }, () ->
                    BroadCast.sendServiceMessage("Unable to find server text channel "
                            + HIGH_ROLL_IMAGES_CHANNEL + " with high rofl images"));
            List<String> lowRoflUrls = new ArrayList<>();
            discordApi.getServerTextChannelById(LOG_ROLL_IMAGES_CHANNEL).ifPresentOrElse(textChannel -> {
                if (textChannel.canYouReadMessageHistory()
                        && textChannel.canYouSee()) {
                    textChannel.getMessagesAsStream().forEach(message ->
                            message.getAttachments().forEach(messageAttachment ->
                                    lowRoflUrls.add(messageAttachment.getUrl().toString())));
                    LOW_ROFL_LIST.clear();
                    LOW_ROFL_LIST.addAll(lowRoflUrls);
                    BroadCast.sendServiceMessage("Found " + LOW_ROFL_LIST.size()
                            + " URLs images into channel " + textChannel.getName());
                } else {
                    BroadCast.sendServiceMessage("Unable to see messages and history for " +
                            "channel " + textChannel.getName() + " (channel with low rofl images)");
                }
            }, () -> BroadCast.sendServiceMessage("Unable to find server text channel "
                    + LOG_ROLL_IMAGES_CHANNEL + " with low rofl images"));

        }, () -> log.fatal("Unable to rebuild dice reaction rofl indexes - api is null!"));
    }

    private static class SumModifier {

        private final long value;
        private final boolean isAddition;

        @Contract(pure = true)
        SumModifier(long value, boolean isAddition) {
            this.value = value;
            this.isAddition = isAddition;
        }

        long modify(final long currentSum) {
            if (value > 0) {
                return isAddition ? currentSum + value : currentSum - value;
            }
            return currentSum;
        }
    }

    private static class ResultFilter {

        private final FilterType filterType;
        private final long filterValue;

        @Contract(pure = true)
        ResultFilter(FilterType filterType, long filterValue) {
            this.filterType = filterType != null ? filterType : FilterType.NOP;
            this.filterValue = filterValue;
        }

        boolean isOk(final long value) {
            if (filterValue > 0) {
                switch (filterType) {
                    case EQ:
                        return value == filterValue;
                    case LT:
                        return value < filterValue;
                    case GT:
                        return value > filterValue;
                    case LE:
                        return value <= filterValue;
                    case GE:
                        return value >= filterValue;
                    case NE:
                        return value != filterValue;
                    case NOP:
                        return true;
                }
            }
            return true;
        }

        boolean notOk(final long value) {
            return !isOk(value);
        }
    }

    private enum FilterType {
        EQ, LT, GT, LE, GE, NE, NOP
    }
}
