package hellfrog.common;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
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
     * Максимальный таймаут ожидания скачиваниния/выполнения запроса
     */
    Duration OP_WAITING_DURATION = Duration.of(OP_WAITING_TIMEOUT, ChronoUnit.MILLIS);

    /**
     * Единица времени ожидания
     */
    TimeUnit OP_TIME_UNIT = TimeUnit.MILLISECONDS;

    // https://discord.com/developers/docs/resources/channel#embed-limits
    /**
     * Embed titles are limited to 256 characters
     */
    int MAX_EMBED_TITLE_LENGTH = 256;

    /**
     * A field's name is limited to 256 characters
     */
    int MAX_EMBED_FIELD_NAME_LENGTH = 256;

    /**
     * A field's value is limited to 1024 characters
     */
    int MAX_EMBED_FIELD_VALUE_LENGTH = 1024;
}
