package hellfrog.settings.db;

import hellfrog.common.ResourcesLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;

/**
 * WTF assigns DAO
 * <p>
 * CREATE TABLE "wtf_assigns" (
 * 	"id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
 * 	"server_id"	INTEGER NOT NULL,
 * 	"author_id"	INTEGER NOT NULL,
 * 	"target_id"	INTEGER NOT NULL,
 * 	"description"	TEXT,
 * 	"image_url"	TEXT,
 * 	"create_date"	INTEGER NOT NULL DEFAULT 0,
 * 	"update_date"	INTEGER NOT NULL DEFAULT 0
 * );
 * </p>
 */
public class WtfAssignDAO {

    private final Logger log = LogManager.getLogger("WTF assign");
    private final Connection connection;

    public WtfAssignDAO(@NotNull Connection connection) {
        this.connection = connection;
        ResourcesLoader.initFileResources(this, WtfAssignDAO.class);
    }
}
