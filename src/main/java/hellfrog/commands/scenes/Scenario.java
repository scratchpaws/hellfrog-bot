package hellfrog.commands.scenes;

import hellfrog.commands.ACLCommand;
import hellfrog.common.CodeSourceUtils;
import hellfrog.common.CommonConstants;
import hellfrog.common.CommonUtils;
import hellfrog.core.SessionState;
import hellfrog.settings.SettingsController;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.Messageable;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Абстрактный интерактивный сценарий, который отображается для пользователя
 * в виде одного или нескольких диалогов выбора и действия.
 * Каждый сценарий разбит на один либо несколько шагов, отображаемый в зависимости от выбора
 * реакции или ввода сообщения пользователя.<p>
 * <p>
 * Сценарий, как и любая другая команда бота, активируется вводом текстового сообщения,
 * содержащего префикс бота и префикс команды.<p>
 * <p>
 * Каждый сценарий является stateless-объектом, и может одновременно
 * обрабатывать множество разных событий от разных пользователей в разных каналах.
 * Все шаги сценария при выполнении пользователями действий помещают информацию
 * о промежуточном состоянии выполнения сценария пользователем в хранилище
 * состояния сценария - {@link SessionState}.<p>
 * <p>
 * Основываясь на информации промежуточого состояния и источника поступления события,
 * сценарий выполняет тот либо иной шаг. Для каждого источника поступления события - новое сообщение,
 * новая реакция (с разбивкой на то, приватный это канал или текстовый канал сервера) - используется
 * собственный метод обработки. Выполнив действия, шаг сценария либо сохраняет новое состояние в
 * хранилище, либо отбрасывает старое состояние в хранилище обратно (в случае ошибки либо неприменимости шага),
 * либо более не сохраняет ничего (в случае завершения сценария)<p>
 *
 * <h2><a id="lifecycle">Жизненный цикл сценария</a></h2>
 * <ul>
 *     <li>Экземпляры объекта сценариев, наследующие данный класс, инициализируются при запуске бота</li>
 *     <li>При поступлении текстового сообщения, содержащего префикс либо меншен бота и префикс команды
 *     сценария, со стороны обработкика событий {@link hellfrog.core.EventsListener}
 *     вызывается {@link #firstRun(MessageCreateEvent)}</li>
 *     <li>В зависимости от источника события, будь то приватный канал или канал сервера,
 *     в {@link #firstRun(MessageCreateEvent)} выполняются
 *     первичные проверки наличия у пользователя прав на выполнение команды, либо на возможность
 *     выполнения команды в привате</li>
 *     <li>При успешном прохождении проверок для приватного канала вызывается
 *     {@link #executePrivateFirstRun(MessageCreateEvent, PrivateChannel, User, boolean)},
 *     для серверного текстового канала вызывается
 *     {@link #executeServerFirstRun(MessageCreateEvent, Server, ServerTextChannel, User, boolean)}</li>
 *     <li>Данные методы выполняют первичные шаги сценария, отображая диалоговое сообщение с реакциями
 *     (при необходимости)</li>
 *     <li>Если необходимы дальнейшие шаги, то данные методы создают сессию пользователя
 *     {@link SessionState}, сохраняя информацию непосредственно о сценарии, пользователе, коде шага,
 *     канале-источнике, и информацию о созданном диалоговом сообщении с реакциями (еали в дальнейшем
 *     нужно обработать нажатия на реакции в этом сообщении)</li>
 *     <li>При необходимости, сессия {@link SessionState} может сохранять любой объект с любым типом данных.
 *     Таким образом можно переносить объекты между шагами сценария. Посредством методов
 *     {@link SessionState#putValue(String, Object)} и {@link SessionState#getValue(String, Class)}
 *     сохраняются и извлекаются объекты</li>
 *     <li>Сессия помещается в список сессий посредством {@link #commitState(SessionState)}</li>
 *     <li>Далее обработчик событий {@link hellfrog.core.EventsListener} проверяет каждое событие нового сообщения
 *     и каждую реакцию на соответствие ранее сохранённым сессиям {@link SessionState}</li>
 *     <li>При нахождении соответствия, в зависимости от типа события, вызываются методы обработки
 *     последующих шагов:
 *     {@link #executeMessageStep(MessageCreateEvent, SessionState)} для сообщений и
 *     {@link #executeReactionStep(SingleReactionEvent, SessionState)} для реакций</li>
 *     <li>В свою очередь каждый из данных методов повторно выполняет проверку наличия у пользователя
 *     прав на выполнение команды сценария (если это событие текстового канала)</li>
 *     <li>И далее вызывает:
 *     {@link #privateMessageStep(MessageCreateEvent, PrivateChannel, User, SessionState, boolean)} для
 *     приватных сообщений,
 *     {@link #serverMessageStep(MessageCreateEvent, Server, ServerTextChannel, User, SessionState, boolean)} для
 *     сообщений из текстовых каналов сервера,
 *     {@link #privateReactionStep(boolean, SingleReactionEvent, PrivateChannel, User, SessionState, boolean)} для
 *     приватных реакций,
 *     {@link #serverReactionStep(boolean, SingleReactionEvent, Server, ServerTextChannel, User, SessionState, boolean)}
 *     для реакций из текстовых каналов сервера.</li>
 *     <li>Данные методы в процессе выполнения шага сценария либо создают новый объект сессии пользователя
 *     с новыми данными, необходимыми для последующих шагов, либо ничего не создают (при завершении сценария),
 *     либо возвращают <code>false</code>, указывающую на ошибку/невозможность обработки шага.</li>
 *     <li>Возврат <code>false</code> из вышеуказанных методов интерпретируется методами
 *     {@link #executeMessageStep(MessageCreateEvent, SessionState)} и
 *     {@link #executeReactionStep(SingleReactionEvent, SessionState)} как ошибку при выполнении шага,
 *     либо невозможность выполнения шага, в этом случае клон сессии пользователя возвращается обратко в список
 *     сохранённых сессий со сброшенным таймаутом посредством {@link #rollbackState(SessionState)}</li>
 *     <li>По истечению таймаута сессии сценарий для пользователя прерывается, данная проверка проводится
 *     в {@link hellfrog.core.SessionsCheckTask}</li>
 *     <li>Инициализированные экземпляры сценариев уничтожаются только при остановке бота</li>
 * </ul>
 */
public abstract class Scenario
        extends ACLCommand
        implements CommonConstants {

    /**
     * Список всех найденных инициализированных экземпляров сценариев, основанных
     * на данном абстрактном сценарии
     */
    private static final List<Scenario> ALL = CodeSourceUtils.childClassInstancesCollector(Scenario.class);

    /**
     * Инициализация
     *
     * @param prefix      обязательный префикс сценарция. Указывает на то, какой командой будет
     *                    запускаться сценарий
     * @param description описание сценария, отображается в справке по доступным командам
     */
    public Scenario(@NotNull String prefix, @NotNull String description) {
        super(prefix, description);
    }

    /**
     * Возвращает список всех найденных инициализированных экземпляров сценариев, основанных на данном
     * абстрактном сценарции.
     *
     * @return список сценариев
     */
    @Contract(pure = true)
    public static List<Scenario> all() {
        return ALL;
    }

    /**
     * Проверка, что данный сценарий может быть запущен с помощью переданной команды
     *
     * @param rawCommand команда для запуска
     * @return если <code>true</code>, то может быть запущен
     */
    public boolean canExecute(String rawCommand) {
        return !CommonUtils.isTrStringEmpty(rawCommand)
                && rawCommand.strip().equalsIgnoreCase(super.getPrefix());
    }

    /**
     * Первичный запуск сценария по команде.
     * Если условия для вызова сценарция выполняются (наличие прав, возможность работы
     * в указанном текстовом канале), то в зависимости от типа текстового канала
     * будет выполнена первичная сцена (с отображением первого диалога пользователя),
     * и создана сессия, содержащая информацию о запущенном сценарии для данного пользователя.
     * В зависимости от источтика, после проведения проверок доступа,
     * будут вызваны:
     * {@link #executePrivateFirstRun(MessageCreateEvent, PrivateChannel, User, boolean)}
     * если сообщение поступило из привата пользователя,
     * {@link #executeServerFirstRun(MessageCreateEvent, Server, ServerTextChannel, User, boolean)}
     * если сообщение поступило из текстового канала сервера
     *
     * @param event событие нового текстового сообщения, по которому запущен сценарий
     */
    public final void firstRun(final @NotNull MessageCreateEvent event) {

        super.updateLastUsage();

        boolean isBotOwner = canExecuteGlobalCommand(event);

        event.getMessageAuthor().asUser().ifPresent(user -> {
            event.getServerTextChannel().ifPresent(serverTextChannel -> {
                if (!canExecuteServerCommand(event, serverTextChannel.getServer())) {
                    showAccessDeniedServerMessage(event);
                    return;
                }
                executeServerFirstRun(event, serverTextChannel.getServer(), serverTextChannel, user, isBotOwner);
            });
            event.getPrivateChannel().ifPresent(privateChannel -> {
                if (isOnlyServerCommand()) {
                    showErrorMessage("This command can't be run into private channel", event);
                    return;
                }
                executePrivateFirstRun(event, privateChannel, user, isBotOwner);
            });
        });
    }

    /**
     * Первичный запуск сценария по команде в привате.
     * К моменту вызова метода уже выполнена проверка, может ли сценарий выполняться в приватных каналах.
     *
     * @param event          событие нового текстового сообщения
     * @param privateChannel приватный канал пользователя, как источник события
     * @param user           пользователь, вызвавший команду
     * @param isBotOwner     признак, является ли пользователь владельцем бота
     * @see #firstRun(MessageCreateEvent)
     */
    protected abstract void executePrivateFirstRun(@NotNull MessageCreateEvent event,
                                                   @NotNull PrivateChannel privateChannel,
                                                   @NotNull User user,
                                                   boolean isBotOwner);

    /**
     * Первичный запуск сценария по команде в канале сервера.
     * К моменту вызова метода уже выполнена проверка, может ли сценарий вызываться пользователем на сервере.
     *
     * @param event             событие нового текстового сообщения
     * @param server            сервер, откуда поступило сообщение
     * @param serverTextChannel текстовый канал, откуда поступило сообщение
     * @param user              пользователь, вызвавший команду
     * @param isBotOwner        признак, является ли пользователь владельцем бота
     * @see #firstRun(MessageCreateEvent)
     */
    protected abstract void executeServerFirstRun(@NotNull MessageCreateEvent event,
                                                  @NotNull Server server,
                                                  @NotNull ServerTextChannel serverTextChannel,
                                                  @NotNull User user,
                                                  boolean isBotOwner);

    /**
     * Последующее выполнение сценария в виде какого-либо шага при поступлении сообщения.
     * Обработчик вызывается при поступлении сообщения в чате при условии, что
     * сессия сценария {@link SessionState} действительна для данного сообщения в текстовом канале.
     * В зависимости от источтика, после проведения проверок доступа,
     * будут вызваны:
     * {@link #privateMessageStep(MessageCreateEvent, PrivateChannel, User, SessionState, boolean)}
     * если сообщение поступило из приватного канала пользователя,
     * {@link #serverMessageStep(MessageCreateEvent, Server, ServerTextChannel, User, SessionState, boolean)}
     * если сообщение поступило из текстоого канала сервера.
     * При успешном завершении шага сценария старая сессия отбрасывается и заменяется новой.
     * В момент выполнения шага сценария старая сессия извлекается из общего списка сессий.
     *
     * @param event        событие нового сообщения
     * @param sessionState состояние запущенного для пользователя сценария в текстовом чате
     * @see hellfrog.core.EventsListener
     */
    public final void executeMessageStep(@NotNull MessageCreateEvent event,
                                         @NotNull SessionState sessionState) {
        super.updateLastUsage();

        boolean doRollback = true;
        boolean isBotOwner = canExecuteGlobalCommand(event);

        Optional<User> mayBeUser = event.getMessageAuthor().asUser();
        if (mayBeUser.isPresent()) {
            User user = mayBeUser.get();
            Optional<ServerTextChannel> mayBeServerChannel = event.getServerTextChannel();
            if (mayBeServerChannel.isPresent()) {
                ServerTextChannel serverTextChannel = mayBeServerChannel.get();
                Server server = serverTextChannel.getServer();
                if (!canExecuteServerCommand(event, server)) {
                    dropPreviousStateEmoji(sessionState);
                    showAccessDeniedServerMessage(event);
                    doRollback = false;
                } else {
                    if (serverMessageStep(event, server, serverTextChannel, user, sessionState, isBotOwner)) {
                        doRollback = false;
                    }
                }
            }
            Optional<PrivateChannel> mayBePrivateChannel = event.getPrivateChannel();
            if (mayBePrivateChannel.isPresent()) {
                PrivateChannel privateChannel = mayBePrivateChannel.get();
                if (privateMessageStep(event, privateChannel, user, sessionState, isBotOwner)) {
                    doRollback = false;
                }
            }
        }

        if (doRollback) {
            rollbackState(sessionState);
        }
    }

    /**
     * Последующая выполнение сценария в привате (сообщение).
     * Сценарий обрабатывается в зависимости от состояния, сохранённого
     * в сессии пользователя {@link SessionState}.
     * К моменту вызова данного метода уже выполнена проверка, возможно ли выполнение сценария
     * в привате пользователя.
     *
     * @param event          событие создания нового сообщения
     * @param privateChannel приватный канал, откуда поступило сообщение
     * @param user           автор сообщения
     * @param sessionState   сохранённое состояние сценарция в виде сессии
     * @param isBotOwner     признак, что автор сообщения является владельцем бота
     * @return успешность завершения шага сценария. Если <code>true</code> - шаг
     * выполнен успешно, и старая сессия отбрасывается. Если <code>false</code> - шаг
     * выполнен неудачно, старая сессия возвращается в список сессий обратно.
     * @see #executeMessageStep(MessageCreateEvent, SessionState)
     */
    protected abstract boolean privateMessageStep(@NotNull MessageCreateEvent event,
                                                  @NotNull PrivateChannel privateChannel,
                                                  @NotNull User user,
                                                  @NotNull SessionState sessionState,
                                                  boolean isBotOwner);

    /**
     * Последующая выполнение сценария в текстовом канале сервера (сообщение).
     * Сценарий обрабатывается в зависимости от состояния, сохранённого
     * в сессии пользователя {@link SessionState}.
     * К моменту вызова данного метода уже выполнена проверка, есть ли у пользователя необходимые
     * права работы с командой сценария
     *
     * @param event             событие создания нового сообщения
     * @param server            сервер, откуда поступило сообщение
     * @param serverTextChannel текстовый канал, откуда поступило сообщение
     * @param user              автор сообщения
     * @param sessionState      сохранённое состояние сценарция в виде сессии
     * @param isBotOwner        признак, что автор сообщения является владельцем бота
     * @return успешность завершения шага сценария. Если <code>true</code> - шаг
     * выполнен успешно, и старая сессия отбрасывается. Если <code>false</code> - шаг
     * выполнен неудачно, старая сессия возвращается в список сессий обратно.
     */
    protected abstract boolean serverMessageStep(@NotNull MessageCreateEvent event,
                                                 @NotNull Server server,
                                                 @NotNull ServerTextChannel serverTextChannel,
                                                 @NotNull User user,
                                                 @NotNull SessionState sessionState,
                                                 boolean isBotOwner);

    /**
     * Последующее выполнение сценария в виде какого-либо шага при поступлении реакции.
     * Обработчик вызывается при поступлении реакции для ранее созданного диалогового
     * сообщения в чате при условии, что сессия сценария {@link SessionState} действительна
     * для данной реакции данного сообщения в текстовом канале.
     * В зависимости от источтика, после проведения проверок доступа,
     * будут вызваны:
     * {@link #privateReactionStep(boolean, SingleReactionEvent, PrivateChannel, User, SessionState, boolean)}
     * если реакция поступила для сообщения из приватного канала пользователя,
     * {@link #serverReactionStep(boolean, SingleReactionEvent, Server, ServerTextChannel, User, SessionState, boolean)}
     * если реакция поступила из текстоого канала сервера.
     * При успешном завершении шага сценария старая сессия отбрасывается и заменяется новой.
     * В момент выполнения шага сценария старая сессия извлекается из общего списка сессий.
     *
     * @param event        событие реакции (добавление/удаление)
     * @param sessionState состояние запущенного для пользователя сценария в текстовом чате
     * @see hellfrog.core.EventsListener
     */
    public final void executeReactionStep(@NotNull SingleReactionEvent event,
                                          @NotNull SessionState sessionState) {
        super.updateLastUsage();
        boolean isAddReaction = event instanceof ReactionAddEvent;
        boolean isBotOwner = canExecuteGlobalCommand(event);
        User user = event.getUser();
        boolean doRollback = true;

        Optional<ServerTextChannel> mayBeServerChannel = event.getServerTextChannel();
        if (mayBeServerChannel.isPresent()) {
            ServerTextChannel serverTextChannel = mayBeServerChannel.get();
            Server server = serverTextChannel.getServer();
            if (!canExecuteServerCommand(event, server)) {
                dropPreviousStateEmoji(sessionState);
                showAccessDeniedServerMessage(event);
                if (isAddReaction) {
                    ((ReactionAddEvent) event).removeReaction();
                }
                doRollback = false;
            } else {
                if (serverReactionStep(isAddReaction, event, server, serverTextChannel,
                        user, sessionState, isBotOwner)) {
                    doRollback = false;
                }
            }
        }
        Optional<PrivateChannel> mayBePrivateChannel = event.getPrivateChannel();
        if (mayBePrivateChannel.isPresent()) {
            PrivateChannel privateChannel = mayBePrivateChannel.get();
            if (privateReactionStep(isAddReaction, event, privateChannel, user, sessionState, isBotOwner)) {
                doRollback = false;
            }
        }

        if (doRollback) {
            rollbackState(sessionState);
            if (isAddReaction) {
                ((ReactionAddEvent) event).removeReaction();
            }
        }
    }

    /**
     * Последующая выполнение сценария в привате (реакция).
     * Сценарий обрабатывается в зависимости от состояния, сохранённого
     * в сессии пользователя {@link SessionState}.
     * К моменту вызова данного метода уже выполнена проверка, возможно ли выполнение сценария
     * в привате пользователя.
     *
     * @param isAddReaction  признак, что это добавление новой реакции
     * @param event          событие реакции (добавление/удаление)
     * @param privateChannel приватный канал, откуда поступила реакция
     * @param user           автор реакции
     * @param sessionState   сохранённое состояние сценарция в виде сессии
     * @param isBotOwner     признак, что автор реакции является владельцем бота
     * @return успешность завершения шага сценария. Если <code>true</code> - шаг
     * выполнен успешно, и старая сессия отбрасывается. Если <code>false</code> - шаг
     * выполнен неудачно, старая сессия возвращается в список сессий обратно.
     * @see #executeReactionStep(SingleReactionEvent, SessionState)
     */
    protected abstract boolean privateReactionStep(boolean isAddReaction,
                                                   @NotNull SingleReactionEvent event,
                                                   @NotNull PrivateChannel privateChannel,
                                                   @NotNull User user,
                                                   @NotNull SessionState sessionState,
                                                   boolean isBotOwner);

    /**
     * Последующая выполнение сценария в текстовом канале сервера (реакция).
     * Сценарий обрабатывается в зависимости от состояния, сохранённого
     * в сессии пользователя {@link SessionState}.
     * К моменту вызова данного метода уже выполнена проверка, есть ли у пользователя необходимые
     * права работы с командой сценария
     *
     * @param isAddReaction     признак, что это добавление новой реакции
     * @param event             событие реакции (добавление/удаление)
     * @param server            сервер, откуда поступила реакция
     * @param serverTextChannel текстовый канал, откуда поступила реакция
     * @param user              автор реакции
     * @param sessionState      сохранённое состояние сценарция в виде сессии
     * @param isBotOwner        признак, что автор реакции является владельцем бота
     * @return успешность завершения шага сценария. Если <code>true</code> - шаг
     * выполнен успешно, и старая сессия отбрасывается. Если <code>false</code> - шаг
     * выполнен неудачно, старая сессия возвращается в список сессий обратно.
     * @see #executeReactionStep(SingleReactionEvent, SessionState)
     */
    protected abstract boolean serverReactionStep(boolean isAddReaction,
                                                  @NotNull SingleReactionEvent event,
                                                  @NotNull Server server,
                                                  @NotNull ServerTextChannel serverTextChannel,
                                                  @NotNull User user,
                                                  @NotNull SessionState sessionState,
                                                  boolean isBotOwner);

    /**
     * Отрисовка диалогового окна либо сообщения с переданным
     * Embed внутри, с ожиданием получения сообщения каналом.
     *
     * @param embedBuilder подготовленный embed
     * @param target       целевой канал для отправки сообщения
     * @return отправленное сообщение (либо не отправленное)
     */
    protected Optional<Message> displayMessage(@Nullable EmbedBuilder embedBuilder,
                                               @Nullable Messageable target) {
        if (embedBuilder == null || target == null) {
            return Optional.empty();
        }

        embedBuilder.setTimestampToNow();
        Optional.ofNullable(SettingsController.getInstance().getDiscordApi()).ifPresent(api ->
                embedBuilder.setAuthor(api.getYourself()));
        embedBuilder.setColor(Color.green);

        try {
            return Optional.ofNullable(new MessageBuilder()
                    .setEmbed(embedBuilder)
                    .send(target)
                    .get(OP_WAITING_TIMEOUT, TimeUnit.MILLISECONDS));
        } catch (Exception err) {
            log.error("Unable to send message: " + err.getMessage(), err);
            return Optional.empty();
        }
    }

    /**
     * Перерисовка сообщения либо диалогового окна сообщения из сессии.
     *
     * @param embedBuilder подготовленный embed для замены в оригинальном сообщении
     * @param sessionState сохранённое состояние сценарция в виде сессии
     * @return изменённое сообщение (либо не изменённое, если пусто)
     */
    protected Optional<Message> rewriteMessage(@NotNull EmbedBuilder embedBuilder,
                                               @NotNull SessionState sessionState) {
        Optional<DiscordApi> mayBeApi = Optional.ofNullable(SettingsController.getInstance().getDiscordApi());
        if (mayBeApi.isPresent()) {
            DiscordApi discordApi = mayBeApi.get();
            embedBuilder.setTimestampToNow();
            embedBuilder.setAuthor(discordApi.getYourself());
            embedBuilder.setColor(Color.green);
            Optional<TextChannel> mayBeTextChannel = discordApi.getTextChannelById(sessionState.getMessageId());
            if (mayBeTextChannel.isPresent()) {
                TextChannel textChannel = mayBeTextChannel.get();
                try {
                    Message message = textChannel.getMessageById(sessionState.getMessageId())
                            .get(OP_WAITING_TIMEOUT, TimeUnit.MILLISECONDS);
                    try {
                        message.removeAllReactions().get(OP_WAITING_TIMEOUT, TimeUnit.MILLISECONDS);
                    } catch (Exception delErr) {
                        log.error("Unable to send message: " + delErr.getMessage(), delErr);
                        return Optional.empty();
                    }
                    try {
                        message.edit(embedBuilder).get(OP_WAITING_TIMEOUT, TimeUnit.MILLISECONDS);
                        return Optional.of(message);
                    } catch (Exception sendErr) {
                        log.error("Unable to send message: " + sendErr.getMessage(), sendErr);
                        return Optional.empty();
                    }
                } catch (Exception err) {
                    log.error("Unable to send message: " + err.getMessage(), err);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Добавление реакции сообщению с ожиданием проставления реакций сообщению
     *
     * @param message         сообщение, которому необходимо добавить реакции
     * @param resolvedEmojies кастомные эмодзи для реакций
     * @param unicodeEmojies  unicode-эмодзи для реакций
     * @return успех проставления всех реакций
     */
    protected boolean addReactions(@NotNull Message message,
                                   @Nullable List<KnownCustomEmoji> resolvedEmojies,
                                   @Nullable List<String> unicodeEmojies) {

        boolean existsResolvedEmoji = resolvedEmojies != null && !resolvedEmojies.isEmpty();
        boolean existsUnicodeEmoji = unicodeEmojies != null && !unicodeEmojies.isEmpty();
        if (!existsResolvedEmoji && !existsUnicodeEmoji) {
            return false;
        }

        boolean success = true;
        if (existsResolvedEmoji) {
            for (KnownCustomEmoji knownCustomEmoji : resolvedEmojies) {
                try {
                    message.addReaction(knownCustomEmoji).get(OP_WAITING_TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (Exception err) {
                    log.error("Unable to add reaction: " + err.getMessage(), err);
                    success = false;
                }
            }
        }
        if (existsUnicodeEmoji) {
            for (String unicodeEmoji : unicodeEmojies) {
                try {
                    message.addReaction(unicodeEmoji).get(OP_WAITING_TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (Exception err) {
                    log.error("Unable to add reaction: " + err.getMessage(), err);
                    success = false;
                }
            }
        }
        return success;
    }

    /**
     * Удаление всех реакций в сообщении сессии пользователя.
     *
     * @param sessionState сохранённое состояние сценарция в виде сессии
     * @return успешность удаления реакций
     */
    protected boolean dropPreviousStateEmoji(@NotNull SessionState sessionState) {
        DiscordApi api = SettingsController.getInstance().getDiscordApi();
        if (api == null) return false;

        Optional<TextChannel> mayBeChannel = api.getTextChannelById(sessionState.getTextChannelId());
        if (mayBeChannel.isPresent()) {
            TextChannel textChannel = mayBeChannel.get();
            try {
                Message msg = textChannel.getMessageById(sessionState.getMessageId())
                        .get(OP_WAITING_TIMEOUT, TimeUnit.MILLISECONDS);
                try {
                    msg.removeAllReactions().get(OP_WAITING_TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (Exception delErr) {
                    log.error("Unable to delete all reactions by message: channel id: "
                            + sessionState.getTextChannelId() + ", message id: "
                            + sessionState.getMessageId(), delErr);
                }
                return true;
            } catch (Exception err) {
                log.error("Unable to fetch history message: channel id: "
                        + sessionState.getTextChannelId() + ", message id: "
                        + sessionState.getMessageId(), err);
                return false;
            }
        }

        return false;
    }

    /**
     * Сохранить состояние сценария. Используется для новых состояний при
     * успешном завершении шага.
     * Сохранённая сессия помещается в список сессий.
     *
     * @param sessionState сохранённое состояние сценарция в виде сессии
     */
    protected void commitState(@NotNull SessionState sessionState) {
        SessionState.all().add(sessionState);
    }

    /**
     * Откат состояния сценария. Используется при неудачном завершении шага.
     * Клон сессии пользователя, извлечённой при обработке дальнейших шагов сценария,
     * помещается в список сессий со сброшенным таймаутом сессии.
     *
     * @param sessionState сохранённое состояние сценария в виде сессии
     */
    protected void rollbackState(@NotNull SessionState sessionState) {
        SessionState.all().add(sessionState.resetTimeout());
    }

    /**
     * Проверка, что в событии реакции эмодзи является указанной unicode-эмодзи
     *
     * @param event        событие поступления новой реакции
     * @param unicodeEmoji unicode-эмодзи
     * @return соответствие
     */
    protected boolean equalsUnicodeReaction(@NotNull SingleReactionEvent event,
                                            @NotNull String unicodeEmoji) {
        return event.getEmoji().asUnicodeEmoji()
                .map(unicodeEmoji::equals)
                .orElse(false);
    }

    /**
     * Проверка, что в событии реакций эмодзи является одной из двух указанных unicode-эмодзи
     *
     * @param event       событие поступления новой реакции
     * @param firstEmoji  первая unicode-эмодзи
     * @param secondEmoji вторая unicode-эмодзи
     * @return соответствией первой либо второй unicode-эмодзи
     */
    protected boolean equalsUnicodeReactions(@NotNull SingleReactionEvent event,
                                             @NotNull String firstEmoji, @NotNull String secondEmoji) {
        return this.equalsUnicodeReaction(event, firstEmoji)
                || this.equalsUnicodeReaction(event, secondEmoji);
    }
}
