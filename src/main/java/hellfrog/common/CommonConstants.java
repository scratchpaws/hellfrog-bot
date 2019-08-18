package hellfrog.common;

import java.util.concurrent.TimeUnit;

public interface CommonConstants {

    /**
     * Максимальный размер файла для аттача
     */
    int MAX_FILE_SIZE = 8_388_608; // 8 Мб

    /**
     * Максимальное время ожидания скачивания/редактирования/отправки сообщений
     */
    long OP_WAITING_TIMEOUT = 10_000L;

    /**
     * Единица времени ожидания
     */
    TimeUnit OP_TIME_UNIT = TimeUnit.MILLISECONDS;

    /**
     * Канал для отправки служебных уведомлений (вместо привата владельцу),
     * таких как старт бота, стоп, попытка выполнить обновление каким-либо
     * владельцем, попытка выполнить отладочную команду (srv -r).
     * Что бы оно не спамило мне в приват.
     * В данный момент это канал на сервере {@link #OFFICIAL_SERVER}
     *
     * @see hellfrog.commands.cmdline.ServiceCommand {@link BroadCast}
     */
    long SERVICE_MESSAGES_CHANNEL = 612659329392443422L;

    /**
     * Официальный сервер
     */
    long OFFICIAL_SERVER = 612645599132778517L;

    /**
     * ID канала с наборами изображений, используемый при выпадании высоких результатов
     * ролл-реакции.
     *
     * @see hellfrog.reacts.DiceReaction
     */
    long HIGH_ROLL_IMAGES_CHANNEL = 612654844679028736L;

    /**
     * ID канала с наборами изображений, используемый при выпадании низких результатов
     * ролл-реакции
     *
     * @see hellfrog.reacts.DiceReaction
     */
    long LOG_ROLL_IMAGES_CHANNEL = 612654929219158021L;
}
