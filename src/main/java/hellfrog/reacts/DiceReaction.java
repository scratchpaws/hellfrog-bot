package hellfrog.reacts;

import com.vdurmont.emoji.EmojiParser;
import hellfrog.common.BroadCast;
import hellfrog.common.CommonUtils;
import hellfrog.common.MessageUtils;
import hellfrog.core.ServerSideResolver;
import hellfrog.reacts.dice.*;
import hellfrog.settings.SettingsController;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.*;
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
                    "([mм]|[aа])?" +
                    "(\\s?([=<>]{1,2}|(\\+{1,2}|-{1,2}|[xх]{1,2}))\\s?\\d+)*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SEARCH_PATTERN = Pattern.compile(
            "^\\s*" +
                    "([rр]\\s?)?" +
                    "([dд]?\\s?\\d+\\s?[dд]\\s?\\d+|[lд]{2})" +
                    "([mм]|[aа])?" +
                    "(\\s?([=<>]{1,2}|(\\+{1,2}|-{1,2}|[xх]{1,2}))\\s?\\d+)*",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final String DEFAULT_ROLL = "1d20";

    private static final Pattern ROFL_PATTERN = Pattern.compile("^[rр]",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern VALUE_PATTERN = Pattern.compile(
            "\\d{1,5}\\s?[dд]\\s?\\d{1,5}",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern MEDIAN_PATTERN = Pattern.compile("([mм])",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern AVERAGE_PATTERN = Pattern.compile("([aа])",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern VALUE_MODIFIER_PATTERN = Pattern.compile(
            "(\\+{2}|-{2}|[xх]{2})\\s?\\d{1,5}",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SUMMARY_MODIFIER_PATTERN = Pattern.compile(
            "(\\+{1,2}|-{1,2}|[xх]{1,2})\\s?\\d{1,5}",
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
                        long totalSumResult = 0;
                        RandomSource randomSource = RandomSource.getInstance();
                        int success = 0;

                        final List<SumModifier> diceModifiers = parseModifiers(diceValue, false);
                        final List<SumModifier> summaryModifiers = parseModifiers(diceValue, true);

                        final boolean modifyDices = !diceModifiers.isEmpty();
                        final boolean modifyTotalSum = !summaryModifiers.isEmpty();

                        final String diceModifiersString = generatePrintable(diceModifiers);
                        final String summaryModifiersString = generatePrintable(summaryModifiers);

                        final boolean showAverage = AVERAGE_PATTERN.matcher(diceValue).find();
                        final boolean showMedian = MEDIAN_PATTERN.matcher(diceValue).find();

                        final MutableDoubleList allDices = new DoubleArrayList((int)numOfDice);

                        for (long d = 1; d <= numOfDice; d++) {
                            long tr = randomSource.getDice(numOfFaces);
                            final long origin = tr;
                            if (modifyDices) {
                                for (SumModifier diceModifier : diceModifiers) {
                                    tr = diceModifier.modify(tr);
                                }
                            }
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
                                totalSumResult += tr;
                                allDices.add((double)tr);
                            }
                            dicesOutput.append("[");
                            if (!strike) {
                                dicesOutput.append(tr);
                            } else {
                                dicesOutput.append(String.valueOf(tr), MessageDecoration.STRIKEOUT);
                            }
                            if (modifyDices) {
                                dicesOutput.append(" (")
                                        .append(origin)
                                        .append(diceModifiersString)
                                        .append(")");
                            }
                            dicesOutput.append("]");
                            if (d < numOfDice) {
                                dicesOutput.append(" ");
                            }
                        }

                        final long origTotalSum = totalSumResult;
                        for (SumModifier summaryModifier : summaryModifiers) {
                            totalSumResult = summaryModifier.modify(totalSumResult);
                        }

                        final Median medianCalculator = new Median();
                        final Mean averageCalculator = new Mean();
                        final double[] sortedDices = allDices.toSortedArray();
                        final double median = medianCalculator.evaluate(sortedDices);
                        final double average = averageCalculator.evaluate(sortedDices);

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
                                .append("...");

                        if (!showAverage && !showMedian) {
                            msg.append(String.valueOf(totalSumResult), MessageDecoration.BOLD);
                            if (modifyTotalSum) {
                                msg.append(" (")
                                        .append(origTotalSum)
                                        .append(summaryModifiersString)
                                        .append(")");
                            }
                        } else if (showAverage) {
                            msg.append(String.valueOf(average), MessageDecoration.BOLD)
                                    .append(" (average)");
                        } else {
                            msg.append(String.valueOf(median), MessageDecoration.BOLD)
                                    .append(" (median)");
                        }

                        new MessageBuilder()
                                .setEmbed(new EmbedBuilder()
                                        .setTitle(anotherString)
                                        .setColor(Color.CYAN)
                                        .setDescription(msg.getStringBuilder().toString()))
                                .send(textChannel);
                        if (doRofl && !showAverage && !showMedian) {
                            rofling(textChannel, totalSumResult, totalSumResult < max ?
                                    max - 1 : totalSumResult);
                        }
                    }

                    break;
                }
            }
        } catch (Exception err) {
            log.error(err.getMessage(), err);
        }
    }

    @NotNull @UnmodifiableView
    private List<SumModifier> parseModifiers(@NotNull final String diceValue, final boolean getSummaryModifiers) {

        final List<SumModifier> modifiers = new ArrayList<>();
        final Pattern selectedPattern = getSummaryModifiers ? SUMMARY_MODIFIER_PATTERN : VALUE_MODIFIER_PATTERN;
        final Matcher modifierMatcher = selectedPattern.matcher(diceValue.replaceAll("[XхХ]", "x"));

        while (modifierMatcher.find()) {
            final String rawModifier = modifierMatcher.group();
            if (selectedPattern == SUMMARY_MODIFIER_PATTERN) {
                if (rawModifier.contains("++") || rawModifier.contains("--") || rawModifier.contains("xx")) {
                    continue;
                }
            }
            ModifierType modifierType;
            if (rawModifier.startsWith("+")) {
                modifierType = ModifierType.ADD;
            } else if (rawModifier.startsWith("x")) {
                modifierType = ModifierType.MUL;
            } else {
                modifierType = ModifierType.SUB;
            }
            final long modifierValue = CommonUtils.onlyNumbersToLong(rawModifier);
            if (modifierValue > 0) {
                modifiers.add(new SumModifier(modifierValue, modifierType));
            }
        }

        return Collections.unmodifiableList(modifiers);
    }

    private String generatePrintable(@Nullable List<SumModifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return "";
        }
        return modifiers.stream()
                .map(SumModifier::getPrintable)
                .reduce((s1, s2) -> s1 + s2)
                .orElse("");
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

    public static BroadCast.MessagesLogger rebuildRoflIndexes(final boolean sendBroadcastSeparately) {
        final BroadCast.MessagesLogger messagesLogger = BroadCast.getLogger();
        final SettingsController settingsController = SettingsController.getInstance();
        final long highRollImagesChannelId = settingsController.getMainDBController()
                .getCommonPreferencesDAO()
                .getHighRollChannelId();
        final long lowRollImagesChannelId = settingsController.getMainDBController()
                .getCommonPreferencesDAO()
                .getLowRollChannelId();
        Optional<DiscordApi> mayBeApi = Optional.ofNullable(settingsController.getDiscordApi());
        mayBeApi.ifPresentOrElse(discordApi -> {
            List<String> highRoflUrls = new ArrayList<>();
            discordApi.getServerTextChannelById(highRollImagesChannelId).ifPresentOrElse(textChannel -> {
                if (textChannel.canYouReadMessageHistory()
                        && textChannel.canYouSee()) {
                    textChannel.getMessagesAsStream().forEach(message ->
                            message.getAttachments().forEach(messageAttachment ->
                                    highRoflUrls.add(messageAttachment.getUrl().toString())));
                    HIGH_ROFL_LIST.clear();
                    HIGH_ROFL_LIST.addAll(highRoflUrls);
                    messagesLogger.addInfoMessage("Found " + HIGH_ROFL_LIST.size()
                            + " URLs images into channel " + textChannel.getName());
                } else {
                    messagesLogger.addErrorMessage("Unable to see messages and history for " +
                            "channel " + textChannel.getName() + " (channel with high rofl images)");
                }
            }, () ->
                    messagesLogger.addErrorMessage("Unable to find server text channel "
                            + highRollImagesChannelId + " with high rofl images"));
            List<String> lowRoflUrls = new ArrayList<>();
            discordApi.getServerTextChannelById(lowRollImagesChannelId).ifPresentOrElse(textChannel -> {
                if (textChannel.canYouReadMessageHistory()
                        && textChannel.canYouSee()) {
                    textChannel.getMessagesAsStream().forEach(message ->
                            message.getAttachments().forEach(messageAttachment ->
                                    lowRoflUrls.add(messageAttachment.getUrl().toString())));
                    LOW_ROFL_LIST.clear();
                    LOW_ROFL_LIST.addAll(lowRoflUrls);
                    messagesLogger.addInfoMessage("Found " + LOW_ROFL_LIST.size()
                            + " URLs images into channel " + textChannel.getName());
                } else {
                    messagesLogger.addErrorMessage("Unable to see messages and history for " +
                            "channel " + textChannel.getName() + " (channel with low rofl images)");
                }
            }, () -> messagesLogger.addErrorMessage("Unable to find server text channel "
                    + lowRollImagesChannelId + " with low rofl images"));

        }, () -> log.fatal("Unable to rebuild dice reaction rofl indexes - api is null!"));

        if (sendBroadcastSeparately) {
            messagesLogger.send();
        }
        return messagesLogger;
    }

}
