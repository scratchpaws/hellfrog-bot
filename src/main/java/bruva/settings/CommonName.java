package bruva.settings;

import java.util.Map;

public enum CommonName {
    BOT_PREFIX, BOT_NAME, REMOTE_DEBUG,
    SERVER_TRANSFER, SERVER_TEXTCHAT_TRANSFER;

    public static final Map<CommonName, String> NAMES = Map.of(
            CommonName.BOT_PREFIX, "bot.prefix",
            CommonName.BOT_NAME, "bot.name",
            CommonName.REMOTE_DEBUG, "bot.debug",
            CommonName.SERVER_TRANSFER, "transfer.server.id",
            CommonName.SERVER_TEXTCHAT_TRANSFER, "transfer.textchat.id"
    );

    public static final Map<CommonName, String> DEFAULT_VALUES = Map.of(
            CommonName.BOT_PREFIX, "b>",
            CommonName.BOT_NAME, "Bratha",
            CommonName.REMOTE_DEBUG, "true",
            CommonName.SERVER_TRANSFER, "0",
            CommonName.SERVER_TEXTCHAT_TRANSFER, "0"
    );

    public static final Map<CommonName, String> COMMENTS = Map.of(
            CommonName.BOT_PREFIX, "Bot call prefix",
            CommonName.BOT_NAME, "Bot common name",
            CommonName.REMOTE_DEBUG, "Allow remote debug or not",
            CommonName.SERVER_TRANSFER, "Two phase transfer server id",
            CommonName.SERVER_TEXTCHAT_TRANSFER, "Two phase transfer textchat id"
    );
}
