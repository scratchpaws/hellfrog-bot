package hellfrog.settings.db;

import org.jetbrains.annotations.NotNull;

public interface CommonPreferencesDAO {

    long NAN_LONG = 0L;
    String NAN_STRING = "0";

    long OFFICIAL_SERVER_DEFAULT = 612645599132778517L;
    long SERVICE_CHANNEL_DEFAULT = 612659329392443422L;
    long HIGH_ROLL_CHANNEL_KEY_DEFAULT = 612654844679028736L;
    long LOW_ROLL_CHANNEL_DEFAULT = 612654929219158021L;

    long FUN_BLUSH_CHANNEL = 0L;
    long FUN_HUG_CHANNEL = 0L;
    long FUN_KISS_CHANNEL = 0L;
    long FUN_LOVE_CHANNEL = 0L;
    long FUN_PAT_CHANNEL = 0L;
    long FUN_SHOCK_CHANNEL = 0L;
    long FUN_SLAP_CHANNEL = 0L;
    long FUN_CUDDLE_CHANNEL = 0L;
    long FUN_DANCE_CHANNEL = 0L;
    long FUN_LICK_CHANNEL = 0L;
    long FUN_BITE_CHANNEL = 0L;

    String PREFIX_DEFAULT = ">>";
    String API_KEY_DEFAULT = "";
    String BOT_NAME_DEFAULT = "HellFrog";
    String PREFIX_KEY = "bot.prefix";
    String API_KEY = "api.key";
    String BOT_NAME_KEY = "bot.name";
    String OFFICIAL_SERVER_KEY = "bot.server";
    String SERVICE_CHANNEL_KEY = "bot.service.channel";
    String HIGH_ROLL_CHANNEL_KEY = "bot.high.channel";
    String LOW_ROLL_CHANNEL_KEY = "bot.low.channel";

    String FUN_BLUSH_CHANNEL_KEY = "bot.fun.blush.channel";
    String FUN_HUG_CHANNEL_KEY = "bot.fun.hug.channel";
    String FUN_KISS_CHANNEL_KEY = "bot.fun.kiss.channel";
    String FUN_LOVE_CHANNEL_KEY = "bot.fun.love.channel";
    String FUN_PAT_CHANNEL_KEY = "bot.fun.pat.channel";
    String FUN_SHOCK_CHANNEL_KEY = "bot.fun.shock.channel";
    String FUN_SLAP_CHANNEL_KEY = "bot.fun.slap.channel";
    String FUN_CUDDLE_CHANNEL_KEY = "bot.fun.cuddle.channel";
    String FUN_DANCE_CHANNEL_KEY = "bot.fun.dance.channel";
    String FUN_LICK_CHANNEL_KEY = "bot.fun.lick.channel";
    String FUN_BITE_CHANNEL_KEY = "bot.fun.bite.channel";

    String getApiKey();

    String setApiKey(@NotNull String newApiKey);

    String getBotPrefix();

    String setBotPrefix(@NotNull String newBotPrefix);

    String getBotName();

    String setBotName(@NotNull String newBotName);

    long getOfficialBotServerId();

    long setOfficialBotServerId(long newServerId);

    long getBotServiceChannelId();

    long setBotServiceChannelId(long newBotServiceChannelId);

    long getHighRollChannelId();

    long setHighRollChannelId(long newHighRollChannelId);

    long getLowRollChannelId();

    long setLowRollChannelId(long newLowRollChannelId);

    long getFunBlushChannel();

    long setFunBlushChannel(long newFunBlushChannelId);

    long getFunHugChannel();

    long setFunHugChannel(long newHugBlushChannelId);

    long getFunKissChannel();

    long setFunKissChannel(long newFunKissChannelId);

    long getFunLoveChannel();

    long setFunLoveChannel(long newFunLoveChannelId);

    long getFunPatChannel();

    long setFunPatChannel(long newFunPatChannelId);

    long getFunShockChannel();

    long setFunShockChannel(long newFunShockChannelId);

    long getFunSlapChannel();

    long setFunSlapChannel(long newFunSlapChannelId);

    long getFunCuddleChannel();

    long setFunCuddleChannel(long newFunCuddleChannelId);

    long getFunDanceChannel();

    long setFunDanceChannel(long newFunDanceChannelId);

    long getFunLickChannel();

    long setFunLickChannel(long newFunLickChannelId);

    long getFunBiteChannel();

    long setFunBiteChannel(long newFunBiteChannelId);
}
