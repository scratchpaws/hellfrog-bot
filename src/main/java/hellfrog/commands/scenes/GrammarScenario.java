package hellfrog.commands.scenes;

import com.google.common.base.Optional;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import hellfrog.common.BroadCast;
import hellfrog.common.CommonUtils;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;
import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.language.Russian;
import org.languagetool.language.Ukrainian;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public class GrammarScenario extends OneShotScenario {

    private static final String PREFIX = "spc";
    private static final String DESCRIPTION = "Spellchecking text";
    private final ReentrantLock langToolLock = new ReentrantLock();
    private final JLanguageTool enTool = new JLanguageTool(new AmericanEnglish());
    private final JLanguageTool ruTool = new JLanguageTool(new Russian());
    private final JLanguageTool uaTool = new JLanguageTool(new Ukrainian());
    private final LanguageDetector languageDetector;

    public GrammarScenario() {
        super(PREFIX, DESCRIPTION);
        super.enableStrictByChannels();

        try {
            List<LanguageProfile> profiles = new LanguageProfileReader().readBuiltIn(List.of(LdLocale.fromString("en"),
                    LdLocale.fromString("ru"), LdLocale.fromString("uk")));
            languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                    .withProfiles(profiles)
                    .build();
        } catch (Exception err) {
            err.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Override
    protected void onPrivate(@NotNull MessageCreateEvent event,
                             @NotNull PrivateChannel privateChannel,
                             @NotNull User user,
                             boolean isBotOwner) {
        CompletableFuture.runAsync(() -> exec(event));
    }

    @Override
    protected void onServer(@NotNull MessageCreateEvent event,
                            @NotNull Server server,
                            @NotNull ServerTextChannel serverTextChannel,
                            @NotNull User user,
                            boolean isBotOwner) {
        CompletableFuture.runAsync(() -> exec(event));
    }

    private void exec(@NotNull final MessageCreateEvent event) {

        final String readableText = super.getReadableMessageContentWithoutPrefix(event);
        if (CommonUtils.isTrStringEmpty(readableText)) {
            super.showInfoMessage("Grammar checker. Usage: " + PREFIX + " [en/us/ru/ua/uk] Your text.\n"
                    + "Supported: english (en/us), russian (ru) and ukrainian (ua/uk).\n"
                    + "You can specify a two-letter language code before Your text, or leave it blank. "
                    + "In this case, the bot will try to determine the language itself.", event);
            return;
        }

        try {
            langToolLock.lock();

            language lang;
            String checkingText;
            boolean autodetect = true;

            String[] spaceSplit = readableText.split(" ", 2);
            if (spaceSplit.length >= 2) {
                String rawPrefix = spaceSplit[0].toLowerCase();
                lang = parseLanguage(rawPrefix);
                if (lang.equals(language.UNK)) {
                    lang = detectLanguage(readableText);
                    checkingText = readableText;
                } else {
                    checkingText = spaceSplit[1];
                    autodetect = false;
                }
            } else {
                lang = detectLanguage(readableText);
                checkingText = readableText;
            }

            if (lang.equals(language.UNK)) {
                super.showErrorMessage("""
                        The language cannot be detected or it's not supported.
                        You can use language code before Your text:
                        en/us - english, ru - russian, ua/uk - ukrainian.""", event);
                return;
            }

            List<RuleMatch> matches;
            JLanguageTool languageTool = switch (lang) {
                case RU -> ruTool;
                case UA -> uaTool;
                default -> enTool;
            };
            try {
                matches = languageTool.check(checkingText);
            } catch (IOException err) {
                String errMsg = String.format("Unable to check text with %s languagetool: %s", lang, err.getMessage());
                log.error(errMsg, err);
                BroadCast.getLogger().addErrorMessage(errMsg).send();
                showErrorMessage("Spellcheck engine error", event);
                return;
            }

            StringBuilder highlightText = new StringBuilder(checkingText);
            StringBuilder errors = new StringBuilder();

            if (autodetect) {
                errors.append("Detected language: ").append(lang).append('\n');
            }

            int shift = 0;
            int pos = 1;
            if (matches.isEmpty()) {
                errors.append("No errors found");
            } else {
                for (RuleMatch match : matches) {
                    final String pointText = "`[" + pos + "]";
                    highlightText.insert(match.getFromPos() + shift, pointText);
                    shift += pointText.length();
                    highlightText.insert(match.getToPos() + shift, "`");
                    shift++;
                    errors.append(pos).append(") ")
                            .append(match.getMessage().replaceAll("</?suggestion>", ""))
                            .append(match.getSuggestedReplacements()
                                    .stream()
                                    .limit(3)
                                    .reduce(CommonUtils::reduceConcat)
                                    .map((s) -> " *(" + s + ")*")
                                    .orElse(""))
                            .append('\n');
                    pos++;
                }
            }

            List<String> listOfMessagesText = CommonUtils.splitEqually(
                    highlightText.toString() + "\n\n" + errors.toString(), 1999);
            for (String msgText : listOfMessagesText) {
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setDescription(msgText);
                java.util.Optional<Message> msg = super.displayMessage(embedBuilder, event.getChannel());
                if (msg.isEmpty()) {
                    return;
                }
            }
        } finally {
            langToolLock.unlock();
        }
    }

    private language detectLanguage(@NotNull final String message) {
        Optional<LdLocale> lang = languageDetector.detect(message);
        if (lang.isPresent()) {
            LdLocale locale = lang.get();
            return parseLanguage(locale.getLanguage());
        } else {
            return language.UNK;
        }
    }

    private language parseLanguage(@NotNull final String rawLang) {
        return switch (rawLang) {
            case "en", "us" -> language.US;
            case "ru" -> language.RU;
            case "ua", "uk" -> language.UA;
            default -> language.UNK;
        };
    }

    private enum language {
        US, RU, UA, UNK
    }
}
