package xyz.funforge.scratchypaws.hellfrog.core;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import xyz.funforge.scratchypaws.hellfrog.common.CodeSourceUtils;
import xyz.funforge.scratchypaws.hellfrog.common.InMemoryAttach;
import xyz.funforge.scratchypaws.hellfrog.common.MessageUtils;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import static xyz.funforge.scratchypaws.hellfrog.common.MessageUtils.extractAttaches;

/**
 * Двухфазная передача сообщения из привата в канал с последующей анонимизацией (экспериментально).
 * <br/>
 * В теории способ может усложнить утечку данных (затруднение создания снимков экрана, забивание спамом ботов, считывающих
 * ивенты создания/редактирования сообщений)
 * <br/>
 * Вначале создаётся сообщение (и соответственно - ивента) с мусором.
 * {@link #sendInitialRandomMessage(ServerTextChannel, List, boolean)}
 * Далее производится редактирование сообщения произвольное число раз (с соответствующей генерацией ивентов
 * редактирования) с заполнением произвольным содержимым. Автор подставляется произвольным образом из общего списка
 * участников сервера.
 * {@link #rewritePhaseMessage(Message, List, boolean)}
 * <br/>
 * Затем текст замещается реальным сообщением (с отображением реального автора), переданным пользователем в приват бота.
 * В таком виде сообщение отображается несколько секунд.
 * {@link #writeRealMessageBody(Message, String, MessageAuthor, boolean)}
 * <br/>
 * В конечном этапе сообщение редактирование с замещением текста и автора на мусор и произвольных участников. По
 * аналогии с первоначальным перемешиванием. И наконец текст меняется на реальное сообщение, но с анонимизацией автора.
 * {@link #anonymousMessage(Message, String, boolean)}
 */
public class TwoPhaseTransfer {

    /**
     * Алфавит используется для генерации мусорного сообщения. Если присутствует файл с фразами - будет использоваться
     * файл
     */
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXZYabcdefghijklmnopqresuvwxzy";
    /**
     * Длина мусорных сообщений
     */
    private static final int PHASE_MSG_LENGTH = 100;
    /**
     * Минимальное число мусорных редактирований
     */
    private static final int MIN_REWRITE = 1;
    /**
     * Максимальное число мусорных редактирований
     */
    private static final int MAX_REWRITE = 3;
    /**
     * Сообщение в футере, без аттача
     */
    private static final String FOOTER_MESSAGE = "Wait 5 sec...";

    private static final String INITIAL_FOOTER_WITH_ATTACHER = "Wait 5 sec... (message contains attaches or urls)";
    /**
     * Сообщение в футере для передаваемых сообщений с аттачем (нотификация, что содержится аттач)
     */
    private static final String FOOTER_WITH_ATTACHES = "Wait 5 sec (message contains attachments that are displayed after anonymization)..";

    /**
     * Сообщение в футере для конечного анонимизированного сообщения, если оно содержит аттачи
     */
    private static final String ANONYMOUS_FOOTER_WITH_ATTACHES = "Message contains attaches or urls";
    /**
     * Максимальное время ожидания скачивания/редактирования/отправки сообщений
     */
    private static final long OP_WAITING_TIMEOUT = 10_000L;
    /**
     * Время, которое сообщение отображается с реальным пользователем, отправившим сообщение
     */
    private static final long ANONYMOUS_TIMEOUT = 5_000L;
    /**
     * Путь к фразам, используемым для генерации мусорных данных
     */
    private static final Path PATH_TO_PHRASES_FILE;
    /**
     * Список фраз, используемых для генерации мусорных данных
     */
    private static volatile List<String> randomLines;

    /**
     * Максимальный размер файла для аттача
     */
    private static final int MAX_FILE_SIZE = 8_388_608; // 8 Мб

    /**
     * Паттерн локальных сообщений
     */
    private static final Pattern IGNORE_LINES_PATTERN = Pattern.compile("^(/{2}|\\+)");

    static {
        try {
            PATH_TO_PHRASES_FILE = CodeSourceUtils.resolve("two_phase.txt");
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        replaceLined();
    }

    /**
     * Менеджер управления настроек
     */
    private SettingsController settingsController = SettingsController.getInstance();

    /**
     * Пересчитывание либо первоначальное считывание файла с фразами.
     * Если он присутствует - файл будет считан.
     */
    public static void replaceLined() {
        try {
            if (Files.exists(PATH_TO_PHRASES_FILE)) {
                randomLines = Files.readAllLines(PATH_TO_PHRASES_FILE, StandardCharsets.UTF_8);
            } else {
                randomLines = new ArrayList<>(0);
            }
        } catch (IOException err) {
            randomLines = new ArrayList<>(0);
        }
    }

    /**
     * Получить путь к файлу с фразами для генерации мусорных сообщений.
     * Используется для замены в команде обновления
     *
     * @return путь к файлу с фразами
     */
    @Contract(pure = true)
    public static Path getPathToPhrasesFile() {
        return PATH_TO_PHRASES_FILE;
    }

    /**
     * Точка входа и обработки ивента поступающего сообщения
     *
     * @param event ивент нового сообщения
     */
    void transferAction(@NotNull MessageCreateEvent event) {
        if (event.getMessageAuthor().isYourself()) return;

        Long serverTransfer = settingsController.getServerTransfer();
        Long textChannelTransfer = settingsController.getServerTextChatTransfer();

        if (serverTransfer != null && textChannelTransfer != null) {

            event.getApi().getServerById(serverTransfer).ifPresent(server ->
                    server.getTextChannelById(textChannelTransfer).ifPresent(ch -> {
                        MessageAuthor author = event.getMessageAuthor();
                        String messageContent = event.getMessageContent();

                        boolean isServerTextChatMessage = false;
                        if (event.getServerTextChannel().isPresent()) {
                            ServerTextChannel channel = event.getServerTextChannel().get();
                            long srvId = channel.getServer().getId();
                            long chtId = channel.getId();
                            if (srvId != serverTransfer || chtId != textChannelTransfer) return;
                            if (!IGNORE_LINES_PATTERN.matcher(messageContent).find()) return;
                            isServerTextChatMessage = true;
                        }

                        List<InMemoryAttach> attaches = extractAttaches(event.getMessageAttachments());

                        if (isServerTextChatMessage) {
                            CompletableFuture.runAsync(() -> {
                                try {
                                    Thread.sleep(1_000L);
                                } catch (InterruptedException ignore) {
                                }
                                event.getMessage().delete();
                            });
                        }

                        CompletableFuture.runAsync(() -> parallelTransferAction(server, ch, author, messageContent, attaches));
                    }));
        }
    }

    /**
     * Отделённый подпроцесс, где выполняется двухфазная передача.
     * Вынесено в отдельный поток, т.к. выполняется длительное время.
     *
     * @param server         сервер для отображения сообщений
     * @param ch             канал для отображения сообщений
     * @param author         автор сообщения
     * @param messageContent содержимое сообщения
     * @param attachments    вложения сообщений
     */
    private void parallelTransferAction(Server server, ServerTextChannel ch, MessageAuthor author, String messageContent,
                                        List<InMemoryAttach> attachments) {
        Message msg = null;

        SettingsController.getInstance().updateLastCommandUsage();
        try {
            messageContent = ServerSideResolver.findReplaceSimpleEmoji(messageContent, server);
            List<String> urls = MessageUtils.extractAllUrls(messageContent);
            //List<User> membersList = new ArrayList<>(server.getMembers());
            boolean hasAttachment = !urls.isEmpty() || !attachments.isEmpty();

            //msg = sendInitialRandomMessage(ch, membersList, hasAttachment).get(OP_WAITING_TIMEOUT, TimeUnit.SECONDS);
            msg = sendInitialMessageWithBody(ch, messageContent, author, hasAttachment).get(OP_WAITING_TIMEOUT, TimeUnit.SECONDS);
            //rewritePhaseMessage(msg, membersList, hasAttachment);
            //writeRealMessageBody(msg, messageContent, author, hasAttachment);
            MessageUtils.writeUrls(urls, ch);
            MessageUtils.sendAttachments(attachments, ch);
            sleepAction();
            //rewritePhaseMessage(msg, membersList, hasAttachment);
            anonymousMessage(msg, messageContent, hasAttachment);

        } catch (Exception err) {
            if (msg != null)
                msg.delete();
        }
    }

    /**
     * Записать в тело сообщения Embed с реальным текстом сообщения
     *
     * @param msg            отправленное сообщение
     * @param messageContent реальный текст сообщения
     * @param author         реальный автор первоначального сообщения
     * @param hasAttachments признак наличия вложений либо ссылок
     * @throws InterruptedException при отмене извлечения данных
     * @throws ExecutionException   при отправке данных и ожидании ОК произошла ошибка
     * @throws TimeoutException     при превышении таймаута ожидания
     */
    private void writeRealMessageBody(@NotNull Message msg,
                                      String messageContent,
                                      MessageAuthor author,
                                      boolean hasAttachments)
            throws InterruptedException, ExecutionException, TimeoutException {

        msg.edit(new EmbedBuilder()
                .setAuthor(author)
                .setDescription(messageContent)
                .setTimestampToNow()
                .setFooter(hasAttachments ? FOOTER_WITH_ATTACHES : FOOTER_MESSAGE))
                .get(OP_WAITING_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Анонимизация сообщения (удаление автора), финальный этап жизненного цикла двухфазного сообщения.
     *
     * @param msg            исходное отпрвленное сообщение
     * @param rawMessage     реаьлный текст сообщения
     * @param hasAttachments признак, что сообщение содержит вложения
     */
    private void anonymousMessage(Message msg, String rawMessage, boolean hasAttachments) {
        msg.edit(new EmbedBuilder()
                .setAuthor("<anonymous>")
                .setDescription(rawMessage)
                .setFooter(hasAttachments ? ANONYMOUS_FOOTER_WITH_ATTACHES : null)
                .setTimestamp(msg.getCreationTimestamp()));
    }

    /**
     * Пауза (перед анонимизацией)
     */
    private void sleepAction() {
        try {
            Thread.sleep(ANONYMOUS_TIMEOUT);
        } catch (InterruptedException ignore) {
        }
    }

    /**
     * Отправка инициирующего сообщения с мусором.
     * Первый этап передачи двухфазного сообщения.
     *
     * @param channel        канал, в который передаются двухфазные сообщения
     * @param membersList    список участников сервера (для произвольной подстановки)
     * @param hasAttachments признак наличия аттачей либо ссылок в сообщении
     * @return ожидаемое сообщение
     */
    private CompletableFuture<Message> sendInitialRandomMessage(ServerTextChannel channel,
                                                                List<User> membersList,
                                                                boolean hasAttachments) {
        MessageBuilder msg = new MessageBuilder()
                .setEmbed(buildRandomEmbed(membersList, hasAttachments));

        return msg.send(channel);
    }

    private CompletableFuture<Message> sendInitialMessageWithBody(ServerTextChannel channel,
                                                                  String messageContent,
                                                                  MessageAuthor author,
                                                                  boolean hasAttachments) {
        return new MessageBuilder()
                .setEmbed(new EmbedBuilder()
                        .setAuthor(author)
                        .setDescription(messageContent)
                        .setTimestampToNow()
                        .setFooter(hasAttachments ? INITIAL_FOOTER_WITH_ATTACHER : FOOTER_MESSAGE))
                .send(channel);
    }

    /**
     * Генерация Embed с мусорным содержимым.
     *
     * @param membersList    список учистников (для произвольной подстановки)
     * @param hasAttachments признак наличия аттачей либо ссылок в сообщении
     * @return сконфигурированный Embed
     */
    private EmbedBuilder buildRandomEmbed(List<User> membersList, boolean hasAttachments) {
        return new EmbedBuilder()
                .setFooter(hasAttachments ? FOOTER_WITH_ATTACHES : FOOTER_MESSAGE)
                .setTimestampToNow()
                .setDescription(getPhaseMessage())
                .setAuthor(getRandomUser(membersList));
    }

    /**
     * Генерация мусорной фразы для первоначального сообщения и первичных редактирований.
     *
     * @return мусорная фраза
     */
    @NotNull
    private String getPhaseMessage() {
        StringBuilder msg = new StringBuilder(PHASE_MSG_LENGTH);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        if (randomLines != null && randomLines.size() > 0) {
            String rndLine = "";
            while (msg.length() + rndLine.length() + 1 <= PHASE_MSG_LENGTH) {
                msg.append(rndLine);
                if (rndLine.length() > 0)
                    msg.append(" ");
                rndLine = randomLines.get(rnd.nextInt(randomLines.size()));
            }
        } else {
            for (int i = 0; i < PHASE_MSG_LENGTH; i++)
                msg.append(ALPHABET.charAt(rnd.nextInt(ALPHABET.length())));
        }
        return msg.toString();
    }

    /**
     * Перезапись сообщения мусорными данными.
     *
     * @param msg            первоначальное отправленное сообщение
     * @param membersList    список учистников (для произвольной подстановки)
     * @param hasAttachments признак наличия аттачей либо ссылок в сообщении
     * @throws InterruptedException при отмене извлечения данных
     * @throws ExecutionException   при отправке данных и ожидании ОК произошла ошибка
     * @throws TimeoutException     при превышении таймаута ожидания
     */
    private void rewritePhaseMessage(Message msg, List<User> membersList, boolean hasAttachments)
            throws InterruptedException, ExecutionException, TimeoutException {

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final int rewritesTotal = rnd.nextInt(MIN_REWRITE, MAX_REWRITE + 1);

        for (int i = MIN_REWRITE; i < rewritesTotal; i++) {
            msg.edit(buildRandomEmbed(membersList, hasAttachments)).get(OP_WAITING_TIMEOUT, TimeUnit.SECONDS);
        }
    }

    /**
     * Получить произвольного участника из предложенного списка (из списка участников сервера)
     *
     * @param memberList список участников с сервера
     * @return произвольный участник
     */
    private User getRandomUser(List<User> memberList) {
        if (memberList == null || memberList.size() == 0) {
            return settingsController.getDiscordApi().getYourself();
        }

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int selectedUser = rnd.nextInt(memberList.size());
        return memberList.get(selectedUser);
    }
}
