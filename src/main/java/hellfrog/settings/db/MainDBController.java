package hellfrog.settings.db;

import hellfrog.settings.db.h2.MainDBControllerH2;
import hellfrog.settings.db.sqlite.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;

public abstract class MainDBController implements Closeable, AutoCloseable {

    protected final InstanceType type;
    protected final Logger sqlLog = LogManager.getLogger("DB controller");
    protected final Logger mainLog = LogManager.getLogger("Main");

    public MainDBController(@Nullable InstanceType type) {
        this.type = type;
    }

    public abstract String executeRawQuery(@Nullable String rawQuery);

    public abstract CommonPreferencesDAO getCommonPreferencesDAO();

    public abstract BotOwnersDAO getBotOwnersDAO();

    public abstract ServerPreferencesDAO getServerPreferencesDAO();

    public abstract UserRightsDAO getUserRightsDAO();

    public abstract RoleRightsDAO getRoleRightsDAO();

    public abstract TextChannelRightsDAO getTextChannelRightsDAO();

    public abstract ChannelCategoryRightsDAO getChannelCategoryRightsDAO();

    public abstract VotesDAO getVotesDAO();

    public abstract WtfAssignDAO getWtfAssignDAO();

    public abstract EmojiTotalStatisticDAO getEmojiTotalStatisticDAO();

    public abstract byte[] generateDDL();

    @Override
    public abstract void close();

    @Contract("_ -> new")
    public static @NotNull MainDBController getInstance(@Nullable InstanceType type) throws IOException, SQLException {
        //return new MainDBControllerSQLite(type);
        return new MainDBControllerH2(type);
    }

    public static void destroyTestDatabase() throws IOException {
        MainDBControllerSQLite.destroyTestDatabase();
        MainDBControllerH2.destroyTestDatabase();
    }
}
