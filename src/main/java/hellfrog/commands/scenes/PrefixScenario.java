package hellfrog.commands.scenes;

import hellfrog.core.SessionState;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;

public class PrefixScenario
    extends Scenario {

    private static final String PREFIX = "prefix";
    private static final String DESCRIPTION = "Change a bot commands prefix";

    public PrefixScenario() {
        super(PREFIX, DESCRIPTION);
    }

    /**
     * Инициализация выполнения сценария. Вызывается при вводе соответствующей команды,
     * соответствующей префиксу сценария
     *
     * @param event событие нового сообщения
     */
    @Override
    protected void executeFirstRun(@NotNull MessageCreateEvent event) {
        
    }

    /**
     * Последующее выполнение сценария. Вызывается при поступлении сообщения в чате
     *
     * @param event        событие нового сообщения
     * @param sessionState состояние запущенного для пользователя сценария в текстовом чате
     */
    @Override
    public void executeMessageStep(@NotNull MessageCreateEvent event, @NotNull SessionState sessionState) {

    }

    /**
     * Последующее выполнение сценария. Вызывается при добалении либо удалении эмодзи в текстовом чате
     * на сообщении, созданном в сценарии ранее
     *
     * @param event        событие реакции (добавление/удаление)
     * @param sessionState состояние запущенного для пользователя сценария в текстовом чате
     */
    @Override
    public void executeReactionStep(@NotNull SingleReactionEvent event, @NotNull SessionState sessionState) {

    }
}
