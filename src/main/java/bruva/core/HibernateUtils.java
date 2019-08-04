package bruva.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import java.util.EnumSet;
import java.util.concurrent.locks.ReentrantLock;

public class HibernateUtils {

    private static StandardServiceRegistry registry = null;
    private static SessionFactory sessionFactory = null;
    private static Metadata metadata = null;
    private static ReentrantLock creationLock = new ReentrantLock();
    private static final Logger log = LogManager.getLogger(HibernateUtils.class.getSimpleName());
    private static String hibernateRegistryName = "";

    public static void setHibernateRegistryName(String hibernateRegistryName) {
        HibernateUtils.hibernateRegistryName = hibernateRegistryName;
    }

    public static SessionFactory getSessionFactory() throws Exception {
        if (sessionFactory == null) {
            creationLock.lock();
            try {
                if (sessionFactory == null) {
                    try {
                        if (hibernateRegistryName != null && !hibernateRegistryName.trim().isEmpty()) {
                            registry = new StandardServiceRegistryBuilder()
                                    .configure(hibernateRegistryName)
                                    .build();
                        } else {
                            registry = new StandardServiceRegistryBuilder()
                                    .configure()
                                    .build();
                        }
                        MetadataSources metadataSources = new MetadataSources(registry);
                        metadata = metadataSources.getMetadataBuilder().build();
                        sessionFactory = metadata.buildSessionFactory();
                    } catch (Exception err) {
                        log.error("Unable to create session factory", err);
                        close();
                        throw new Exception("Unable to create session factory", err);
                    }
                }
            } finally {
                creationLock.unlock();
            }
        }
        return sessionFactory;
    }

    public static void generateDDL(String fileName) throws Exception {
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

    public static void close() {
        if (metadata != null) {
            metadata = null;
        }
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        if (registry != null) {
            registry.close();
        }
    }
}
