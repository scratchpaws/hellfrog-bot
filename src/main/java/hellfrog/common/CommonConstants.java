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
}
