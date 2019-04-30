package xyz.funforge.scratchypaws.hellfrog.reactions;

import com.vdurmont.emoji.EmojiParser;
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
import xyz.funforge.scratchypaws.hellfrog.common.CommonUtils;
import xyz.funforge.scratchypaws.hellfrog.common.MessageUtils;
import xyz.funforge.scratchypaws.hellfrog.core.ServerSideResolver;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;

import java.awt.*;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiceReaction
        extends MsgCreateReaction {

    private static final String SHORT_ROLL_PREFIX = "([lLдД]{2})";
    private static final Pattern DICE_PATTERN = Pattern.compile("^([rр])?(d?\\d{1,2}[dд]\\d+|[lд]{2})([=<>]{1,2}\\d+)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SEARCH_PATTERN = Pattern.compile("^\\s*([rр])?(d?\\d{1,2}[dд]\\d+|[lд]{2})([=<>]{1,2}\\d+)?",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final String DEFAULT_ROLL = "1d20";

    private static final Path ROFL_ROOT = Paths.get("./rofls");
    private static final Path ROFL_LOW = ROFL_ROOT.resolve("./min");
    private static final Path ROFL_HIGH = ROFL_ROOT.resolve("./max");

    private static final String PREFIX = "dice";
    private static final String DESCRIPTION = "roll dices (use NdN or ll for roll, " +
            "sample: 1d20, ll. \"r\" prefix add pictures for low and high values.";

    public DiceReaction() {
        super.setCommandPrefix(PREFIX);
        super.setCommandDescription(DESCRIPTION);
        super.enableAccessControl();
        super.enableStrictByChannel();
    }

    @Contract(pure = true)
    public static Path getRoflLowPath() {
        return ROFL_LOW;
    }

    @Contract(pure = true)
    public static Path getRoflHighPath() {
        return ROFL_HIGH;
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

        SettingsController.getInstance().updateLastCommandUsage();
        List<String> lines = Arrays.asList(strMessage.split("\n"));
        for (int i = 0; i < lines.size(); i++) {
            String currentLine = lines.get(i).trim();
            Matcher patternMatcher = DICE_PATTERN.matcher(currentLine);
            if (patternMatcher.find()) {
                String diceValue = patternMatcher.group(0)
                        .replaceAll(SHORT_ROLL_PREFIX, DEFAULT_ROLL)
                        .replaceAll("^d", "");
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

                boolean doRofl = diceValue.toLowerCase().startsWith("r")
                        || diceValue.toLowerCase().startsWith("р");
                boolean doEquals = diceValue.contains("=");
                boolean doGreat = diceValue.contains(">");
                boolean doLess = diceValue.contains("<");
                boolean doFilter = doEquals || doGreat || doLess;

                String[] diceParams = diceValue.trim().split("([dDдД]|[<>=]{1,2})");
                if (diceParams.length >= 2 && diceParams.length <= 3) {
                    long diceNum = CommonUtils.onlyNumbersToLong(diceParams[0]);
                    long varNum = CommonUtils.onlyNumbersToLong(diceParams[1]);
                    long filter = doFilter ? CommonUtils.onlyNumbersToLong(diceParams[2]) : 0;
                    if (diceNum > 0 && varNum > 0 && diceNum <= 10 && varNum <= 100 && filter >= 0 && filter <= 100) {
                        MessageBuilder dicesOutput = new MessageBuilder();
                        long sumResult = 0;
                        ThreadLocalRandom currRnd = ThreadLocalRandom.current();
                        int success = 0;
                        for (long d = 1; d <= diceNum; d++) {
                            boolean strike = doFilter;
                            long tr = currRnd.nextLong(1L, varNum + 1);
                            if (doEquals && doGreat) {
                                if (tr >= filter) {
                                    sumResult += tr;
                                    success++;
                                    strike = false;
                                }
                            } else if (doEquals && doLess) {
                                if (tr <= filter) {
                                    sumResult += tr;
                                    success++;
                                    strike = false;
                                }
                            } else if (doGreat && doLess) {
                                if (tr != filter) {
                                    sumResult += tr;
                                    success++;
                                    strike = false;
                                }
                            } else if (doEquals) {
                                if (tr == filter) {
                                    sumResult += tr;
                                    success++;
                                    strike = false;
                                }
                            } else if (doGreat) {
                                if (tr > filter) {
                                    sumResult += tr;
                                    success++;
                                    strike = false;
                                }
                            } else if (doLess) {
                                if (tr < filter) {
                                    sumResult += tr;
                                    success++;
                                    strike = false;
                                }
                            } else {
                                sumResult += tr;
                            }
                            if (strike) {
                                dicesOutput.append("[", MessageDecoration.STRIKEOUT)
                                        .append(String.valueOf(tr), MessageDecoration.STRIKEOUT)
                                        .append("]", MessageDecoration.STRIKEOUT);
                            } else {
                                dicesOutput.append("[").append(tr).append("]");
                            }
                        }
                        if (doFilter) {
                            dicesOutput.append(" (")
                                    .append(String.valueOf(success))
                                    .append(" success)");
                        }
                        long max = diceNum * varNum + 1;
                        String defName = "You";
                        if (user != null) {
                            defName = MessageUtils.escapeSpecialSymbols(server != null ?
                                    server.getDisplayName(user) :
                                    user.getName());
                        }
                        MessageBuilder msg = new MessageBuilder()
                                .append(defName, MessageDecoration.BOLD)
                                .append(" rolled ")
                                .append(String.valueOf(diceNum))
                                .append("-")
                                .append(String.valueOf(max - 1))
                                .append(" and got ")
                                .append(dicesOutput.getStringBuilder().toString())
                                .append("...")
                                .append(String.valueOf(sumResult), MessageDecoration.BOLD);
                        new MessageBuilder()
                                .setEmbed(new EmbedBuilder()
                                        .setTitle(anotherString)
                                        .setColor(Color.CYAN)
                                        .setDescription(msg.getStringBuilder().toString()))
                                .send(textChannel);
                        if (doRofl) {
                            rofling(textChannel, sumResult, max - 1);
                        }
                    }
                }

                break;
            }
        }
    }

    private void rofling(final TextChannel textChannel, final long result, final long max) {
        if (Files.exists(ROFL_LOW) && Files.exists(ROFL_HIGH) && max >= 2) {
            long lowValue = CommonUtils.getLowValue(max);
            long highValue = CommonUtils.getHighValue(max);
            if (result > lowValue && result < highValue) return;
            Path where = result >= highValue ? ROFL_HIGH : ROFL_LOW;
            long count = 0;
            try (DirectoryStream<Path> listen = Files.newDirectoryStream(where, Files::isRegularFile)) {
                for (Path ignored : listen) count++;
            } catch (IOException ignore) {
                return;
            }
            if (count == 0) return;
            long selectedNumber = ThreadLocalRandom.current()
                    .nextLong(1, count + 1);
            long counter = 0;
            try (DirectoryStream<Path> listen = Files.newDirectoryStream(where, Files::isRegularFile)) {
                for (Path entry : listen) {
                    counter++;
                    if (counter == selectedNumber) {
                        byte[] readied = Files.readAllBytes(entry);
                        String fileName = entry.getFileName().toString();
                        new MessageBuilder()
                                .addAttachment(readied, fileName)
                                .send(textChannel);
                        break;
                    }
                }
            } catch (IOException ignore) {
            }
        }
    }
}
