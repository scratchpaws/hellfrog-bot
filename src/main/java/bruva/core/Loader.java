package bruva.core;

import bruva.settings.SettingsController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Loader {

    private static final Logger log = LogManager.getLogger(Loader.class.getSimpleName());

    public static void main(String... args) throws Exception {
        log.info("Starting database");
        HibernateUtils.getSessionFactory();
        log.info("Database is started");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Closing database");
            HibernateUtils.close();
            log.info("Database is closed");
        }));

        SettingsController settingsController = SettingsController.getInstance();
        settingsController.getApiKey().ifPresentOrElse(s ->
                System.out.println("API key is: " + s),
                () -> System.out.println("Unable to load api key"));

        settingsController.getBotPrefix().ifPresentOrElse(s ->
                System.out.println("Bot prefix is: " + s),
                () -> System.out.println("Unable to get bot prefix"));
        settingsController.isRemoteDebugEnabled().ifPresentOrElse(b ->
                System.out.println("Bot remote debug " + (b ? "enabled" : "disabled")),
                () -> System.out.println("Unable to get bot state"));

        if (settingsController.setBotName("Bruva")) {
            System.out.println("Bot name changed");
        } else {
            System.out.println("Bot name is not changed");
        }

        if (settingsController.setBotPrefix("b>")) {
            System.out.println("Bot prefix is changed");
        } else {
            System.out.println("Bot prefix is not changed");
        }

        if (settingsController.setRemoteDebugEnable(true)) {
            System.out.println("Remote debug enabled");
        } else {
            System.out.println("Cannot change remote debug state");
        }

        settingsController.getBotName().ifPresentOrElse(s ->
                        System.out.println("Bot name is: " + s),
                () -> System.out.println("Unable to load bot name"));
        settingsController.getBotPrefix().ifPresentOrElse(s ->
                        System.out.println("Bot prefix is: " + s),
                () -> System.out.println("Unable to get bot prefix"));
        settingsController.isRemoteDebugEnabled().ifPresentOrElse(b ->
                        System.out.println("Bot remote debug " + (b ? "enabled" : "disabled")),
                () -> System.out.println("Unable to get bot state"));
        //HibernateUtils.generateDDL("./ddl.txt");
    }
}
