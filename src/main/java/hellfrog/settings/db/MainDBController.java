package hellfrog.settings.db;

import hellfrog.settings.db.h2.MainDBControllerH2;
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

    public abstract String executeRawJPQL(@Nullable String queryText);

    public abstract CommonPreferencesDAO getCommonPreferencesDAO();

    public abstract BotOwnersDAO getBotOwnersDAO();

    public abstract ServerPreferencesDAO getServerPreferencesDAO();

    public abstract UserRightsDAO getUserRightsDAO();

    public abstract RoleRightsDAO getRoleRightsDAO();

    public abstract ChannelRightsDAO getChannelRightsDAO();

    public abstract ChannelCategoryRightsDAO getChannelCategoryRightsDAO();

    public abstract VotesDAO getVotesDAO();

    public abstract WtfAssignDAO getWtfAssignDAO();

    public abstract TotalStatisticDAO getTotalStatisticDAO();

    public abstract EntityNameCacheDAO getEntityNameCacheDAO();

    public abstract AutoPromoteRolesDAO getAutoPromoteRolesDAO();

    public abstract RoleAssignDAO getRoleAssignDAO();

    public abstract CommunityControlDAO getCommunityControlDAO();

    public abstract byte[] generateDDL();

    public abstract void createBackup();

    @Override
    public abstract void close();

    @Contract("_ -> new")
    public static @NotNull MainDBController getInstance(@Nullable InstanceType type) throws IOException, SQLException {
        return new MainDBControllerH2(type);
    }

    public static void destroyTestDatabase() throws IOException {
        MainDBControllerH2.destroyTestDatabase();
    }
}
