package bruva.settings.Entity;

import java.util.Map;

public enum CommonName {
    BOT_PREFIX, BOT_NAME, REMOTE_DEBUG, BOT_OWNERS;

    public static final Map<CommonName, String> NAMES = Map.of(
            CommonName.BOT_PREFIX, "bot.prefix",
            CommonName.BOT_NAME, "bot.name",
            CommonName.REMOTE_DEBUG, "bot.debug"
    );

    public static final Map<CommonName, String> DEFAULT_VALUES = Map.of(
            CommonName.BOT_PREFIX, "b>",
            CommonName.BOT_NAME, "Bratha",
            CommonName.REMOTE_DEBUG, "false"
    );

    public static final Map<CommonName, String> COMMENTS = Map.of(
            CommonName.BOT_PREFIX, "Bot call prefix",
            CommonName.BOT_NAME, "Bot common name",
            CommonName.REMOTE_DEBUG, "Allow remote debug or not"
    );
}
