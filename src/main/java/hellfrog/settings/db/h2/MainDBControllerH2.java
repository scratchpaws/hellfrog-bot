package hellfrog.settings.db.h2;

import hellfrog.common.CodeSourceUtils;
import hellfrog.settings.db.*;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.EnumSet;

public class MainDBControllerH2
        extends MainDBController {

    private final StandardServiceRegistry registry;
    private final Metadata metadata;
    private final SessionFactory sessionFactory;
    private boolean closed = false;

    private static final String SETTINGS_DIR_NAME = "settings";
    private static final String PROD_DB_FILE_NAME = "hellfrog_main";
    private static final String TEST_DB_FILE_NAME = "hellfrog_test";
    private static final String BACKUP_DB_FILE_NAME = "hellfrog_backup";
    private static final String DB_EXTENSION = ".mv.db";

    public MainDBControllerH2(@Nullable InstanceType type) throws IOException, SQLException {
        super(type);
        sqlLog.info("Starting database");
        if (type == null) {
            type = InstanceType.PROD;
        }
        Path codeSourcePath = CodeSourceUtils.getCodeSourceParent();
        Path settingsPath = codeSourcePath.resolve(SETTINGS_DIR_NAME);
        Path pathToDb = switch (type) {
            case PROD -> settingsPath.resolve(PROD_DB_FILE_NAME);
            case TEST -> settingsPath.resolve(TEST_DB_FILE_NAME);
            case BACKUP -> settingsPath.resolve(BACKUP_DB_FILE_NAME);
        };
        String JDBC_PREFIX = "jdbc:h2:";
        String connectionURL = JDBC_PREFIX + pathToDb.toString();
        try {
            if (!Files.exists(settingsPath) || !Files.isDirectory(settingsPath)) {
                Files.createDirectory(settingsPath);
            }
        } catch (IOException err) {
            mainLog.fatal("Unable to create settings directory: " + err);
            throw err;
        }
        try {
            Configuration configuration = new Configuration();
            configuration.setProperty(Environment.DRIVER, "org.h2.Driver");
            configuration.setProperty(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
            configuration.setProperty(Environment.USER, "sa");
            configuration.setProperty(Environment.PASS, "sa");
            configuration.setProperty(Environment.HBM2DDL_AUTO, "update");
            configuration.setProperty(Environment.URL, connectionURL);
            if (type.equals(InstanceType.TEST)) {
                configuration.setProperty(Environment.FORMAT_SQL, "true");
                configuration.setProperty(Environment.SHOW_SQL, "true");
            }
            configuration.setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
            CodeSourceUtils.entitiesCollector().forEach(configuration::addAnnotatedClass);
            registry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties())
                    .build();
            MetadataSources metadataSources = new MetadataSources(registry);
            metadata = metadataSources.getMetadataBuilder().build();
            sessionFactory = metadata.buildSessionFactory();
            AutoSession.setSessionFactory(sessionFactory);
        } catch (Exception err) {
            String errMsg = String.format("Unable to create session factory: %s", err.getMessage());
            sqlLog.fatal(errMsg, err);
            throw new SQLException(err);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            sqlLog.info("Closing database (Shutdown hook)");
            close();
            sqlLog.info("Database is closed (Shutdown hook)");
        }));

        sqlLog.info("Database opened");
    }

    public static void destroyTestDatabase() throws IOException {
        Path codeSourcePath = CodeSourceUtils.getCodeSourceParent();
        Path settingsPath = codeSourcePath.resolve(SETTINGS_DIR_NAME);
        Path pathToDb = settingsPath.resolve(TEST_DB_FILE_NAME + DB_EXTENSION);
        Files.deleteIfExists(pathToDb);
    }

    public void generateDDL(String fileName) throws Exception {
        if (metadata != null) {
            SchemaExport schemaExport = new SchemaExport();
            schemaExport.setDelimiter(";");
            schemaExport.setFormat(true);
            schemaExport.setOutputFile(fileName);
            EnumSet<TargetType> targers = EnumSet.of(TargetType.SCRIPT);
            schemaExport.execute(targers, SchemaExport.Action.CREATE, metadata);
        } else {
            throw new RuntimeException("Session factory is closed");
        }
    }

    @Override
    public String executeRawQuery(@Nullable String rawQuery) {
        return null;
    }

    @Override
    public CommonPreferencesDAO getCommonPreferencesDAO() {
        return null;
    }

    @Override
    public BotOwnersDAO getBotOwnersDAO() {
        return null;
    }

    @Override
    public ServerPreferencesDAO getServerPreferencesDAO() {
        return null;
    }

    @Override
    public UserRightsDAO getUserRightsDAO() {
        return null;
    }

    @Override
    public RoleRightsDAO getRoleRightsDAO() {
        return null;
    }

    @Override
    public TextChannelRightsDAO getTextChannelRightsDAO() {
        return null;
    }

    @Override
    public ChannelCategoryRightsDAO getChannelCategoryRightsDAO() {
        return null;
    }

    @Override
    public VotesDAO getVotesDAO() {
        return null;
    }

    @Override
    public WtfAssignDAO getWtfAssignDAO() {
        return null;
    }

    @Override
    public EmojiTotalStatisticDAO getEmojiTotalStatisticDAO() {
        return null;
    }

    @Override
    public void close() {
        if (!closed) {
            sessionFactory.close();
            registry.close();
            sqlLog.info("Database closed");
            closed = true;
        }
    }
}
