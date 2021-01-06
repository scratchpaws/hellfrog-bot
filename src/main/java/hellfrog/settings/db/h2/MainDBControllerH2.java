package hellfrog.settings.db.h2;

import hellfrog.common.CodeSourceUtils;
import hellfrog.common.CommonUtils;
import hellfrog.core.LogsStorage;
import hellfrog.settings.db.*;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Calendar;
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

    private final BotOwnersDAO botOwnersDAO;
    private final CommonPreferencesDAO commonPreferencesDAO;
    private final ServerPreferencesDAO serverPreferencesDAO;
    private final UserRightsDAO userRightsDAO;
    private final RoleRightsDAO roleRightsDAO;
    private final ChannelRightsDAO channelRightsDAO;
    private final ChannelCategoryRightsDAO categoryRightsDAO;
    private final TotalStatisticDAO totalStatisticDAO;
    private final WtfAssignDAO wtfAssignDAO;
    private final VotesDAO votesDAO;
    private final EntityNameCacheDAO entityNameCacheDAO;
    private final AutoPromoteRolesDAO autoPromoteRolesDAO;
    private final RoleAssignDAO roleAssignDAO;

    private final String connectionURL;
    private final String connectionLogin;
    private final String connectionPassword;

    public MainDBControllerH2(@Nullable InstanceType type) throws IOException, SQLException {
        super(type);
        sqlLog.info("Starting database");
        if (type == null) {
            type = InstanceType.PROD;
        }
        Path codeSourcePath = CodeSourceUtils.getCodeSourceParent();
        Path settingsPath = codeSourcePath.resolve(SETTINGS_DIR_NAME);
        String currentDateTime = String.format("_%tF_%<tH-%<tM-%<tS", Calendar.getInstance());
        Path pathToDb = switch (type) {
            case PROD -> settingsPath.resolve(PROD_DB_FILE_NAME);
            case TEST -> settingsPath.resolve(TEST_DB_FILE_NAME);
            case BACKUP -> settingsPath.resolve(BACKUP_DB_FILE_NAME + currentDateTime);
        };
        String JDBC_PREFIX = "jdbc:h2:";
        this.connectionURL = JDBC_PREFIX + pathToDb.toString();
        this.connectionLogin = "sa";
        this.connectionPassword = "sa";
        try {
            if (!Files.exists(settingsPath) || !Files.isDirectory(settingsPath)) {
                Files.createDirectory(settingsPath);
            }
        } catch (IOException err) {
            mainLog.fatal("Unable to create settings directory: " + err);
            throw err;
        }
        SchemaVersionCheckerH2 versionCheckerH2 = new SchemaVersionCheckerH2(this.connectionURL, this.connectionLogin, this.connectionPassword);
        final boolean requiredMigration = versionCheckerH2.checkSchema();
        try {
            try (HibernateXmlCfgGenerator xmlCfgGenerator = new HibernateXmlCfgGenerator(sqlLog)) {
                xmlCfgGenerator.setProperty(Environment.DRIVER, "org.h2.Driver");
                xmlCfgGenerator.setProperty(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
                xmlCfgGenerator.setProperty(Environment.USER, connectionLogin);
                xmlCfgGenerator.setProperty(Environment.PASS, connectionPassword);
                xmlCfgGenerator.setProperty(Environment.HBM2DDL_AUTO, "validate");
                xmlCfgGenerator.setProperty(Environment.URL, connectionURL);
                if (type.equals(InstanceType.TEST)) {
                    xmlCfgGenerator.setProperty(Environment.FORMAT_SQL, "true");
                    //xmlCfgGenerator.setProperty(Environment.SHOW_SQL, "true");
                }
                xmlCfgGenerator.setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
                CodeSourceUtils.entitiesCollector().forEach(xmlCfgGenerator::addAnnotatedClass);
                xmlCfgGenerator.create();

                registry = new StandardServiceRegistryBuilder()
                        .configure(xmlCfgGenerator.getFile())
                        .build();
            }

            MetadataSources metadataSources = new MetadataSources(registry);
            metadata = metadataSources.getMetadataBuilder().build();
            sessionFactory = metadata.buildSessionFactory();

            final AutoSessionFactory autoSessionFactory = new AutoSessionFactory(sessionFactory);
            botOwnersDAO = new BotOwnersDAOImpl(autoSessionFactory);
            commonPreferencesDAO = new CommonPreferencesDAOImpl(autoSessionFactory);
            serverPreferencesDAO = new ServerPreferencesDAOImpl(autoSessionFactory);
            userRightsDAO = new UserRightsDAOImpl(autoSessionFactory);
            roleRightsDAO = new RoleRightsDAOImpl(autoSessionFactory);
            channelRightsDAO = new ChannelRightsDAOImpl(autoSessionFactory);
            categoryRightsDAO = new ChannelCategoryRightsDAOImpl(autoSessionFactory);
            wtfAssignDAO = new WtfAssignDAOImpl(autoSessionFactory);
            votesDAO = new VotesDAOImpl(autoSessionFactory);
            totalStatisticDAO = new TotalStatisticDAOImpl(autoSessionFactory);
            entityNameCacheDAO = new EntityNameCacheDAOImpl(autoSessionFactory);
            autoPromoteRolesDAO = new AutoPromoteRolesDAOImpl(autoSessionFactory);
            roleAssignDAO = new RoleAssignDAOImpl(autoSessionFactory);

        } catch (Exception err) {
            String errMsg = String.format("Unable to create session factory: %s", err.getMessage());
            sqlLog.fatal(errMsg, err);
            throw new SQLException(err);
        }
        if (requiredMigration && type.equals(InstanceType.PROD)) {
            try {
                versionCheckerH2.convertLegacy(this);
            } catch (NullPointerException err) {
                String errMsg = String.format("Legacy conversion error: %s", err.getMessage());
                sqlLog.fatal(errMsg, err);
                throw new SQLException(err);
            }
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
    public byte[] generateDDL() {
        if (metadata != null) {
            Path tempFile = null;
            try {
                SchemaExport schemaExport = new SchemaExport();
                schemaExport.setDelimiter(";");
                schemaExport.setFormat(true);
                Path rootPath = CodeSourceUtils.getCodeSourceParent();
                tempFile = Files.createTempFile(rootPath, "hellfrog_ddl_", ".sql");
                schemaExport.setOutputFile(tempFile.toString());
                EnumSet<TargetType> targers = EnumSet.of(TargetType.SCRIPT);
                schemaExport.execute(targers, SchemaExport.Action.CREATE, metadata);
                return Files.readAllBytes(tempFile);
            } catch (Exception err) {
                String errMsg = String.format("Unable to generate DDL: %s", err.getMessage());
                LogsStorage.addErrorMessage(errMsg);
                sqlLog.error(errMsg, err);
            } finally {
                try {
                    if (tempFile != null)
                        Files.deleteIfExists(tempFile);
                } catch (IOException warn) {
                    String warnMsg = String.format("Unable to delete temporary DDL file \"%s\": %s",
                            tempFile, warn.getMessage());
                    LogsStorage.addWarnMessage(warnMsg);
                    sqlLog.warn(warnMsg, warn);
                }
            }
        }
        return new byte[0];
    }

    @Override
    public String executeRawQuery(@Nullable String rawQuery) {
        if (CommonUtils.isTrStringEmpty(rawQuery)) {
            return "(Query is empty or null)";
        }
        if (closed) {
            return "(Connection is closed)";
        }
        StringBuilder result = new StringBuilder();
        try (Connection connection = DriverManager.getConnection(connectionURL, connectionLogin, connectionPassword)) {
            try (Statement statement = connection.createStatement()) {
                boolean hasResult = statement.execute(rawQuery);
                if (!hasResult) {
                    int updateCount = statement.getUpdateCount();
                    return "Successful. Updated " + updateCount + " rows";
                } else {
                    try (ResultSet resultSet = statement.getResultSet()) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            String label = metaData.getColumnLabel(i);
                            result.append(label);
                            if (i < metaData.getColumnCount()) {
                                result.append("|");
                            }
                        }
                        result.append('\n');
                        result.append("-".repeat(Math.max(0, result.length())));
                        result.append('\n');
                        while (resultSet.next()) {
                            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                                String value = resultSet.getString(i);
                                result.append(value);
                                if (i < metaData.getColumnCount()) {
                                    result.append("|");
                                }
                            }
                            result.append('\n');
                        }
                    }
                }
            }
        } catch (SQLException err) {
            return err.getMessage();
        }

        return result.toString();
    }

    @Override
    public CommonPreferencesDAO getCommonPreferencesDAO() {
        return commonPreferencesDAO;
    }

    @Override
    public BotOwnersDAO getBotOwnersDAO() {
        return botOwnersDAO;
    }

    @Override
    public ServerPreferencesDAO getServerPreferencesDAO() {
        return serverPreferencesDAO;
    }

    @Override
    public UserRightsDAO getUserRightsDAO() {
        return userRightsDAO;
    }

    @Override
    public RoleRightsDAO getRoleRightsDAO() {
        return roleRightsDAO;
    }

    @Override
    public ChannelRightsDAO getTextChannelRightsDAO() {
        return channelRightsDAO;
    }

    @Override
    public ChannelCategoryRightsDAO getChannelCategoryRightsDAO() {
        return categoryRightsDAO;
    }

    @Override
    public VotesDAO getVotesDAO() {
        return votesDAO;
    }

    @Override
    public WtfAssignDAO getWtfAssignDAO() {
        return wtfAssignDAO;
    }

    @Override
    public TotalStatisticDAO getTotalStatisticDAO() {
        return totalStatisticDAO;
    }

    @Override
    public EntityNameCacheDAO getEntityNameCacheDAO() {
        return entityNameCacheDAO;
    }

    @Override
    public AutoPromoteRolesDAO getAutoPromoteRolesDAO() {
        return autoPromoteRolesDAO;
    }

    @Override
    public RoleAssignDAO getRoleAssignDAO() {
        return roleAssignDAO;
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
