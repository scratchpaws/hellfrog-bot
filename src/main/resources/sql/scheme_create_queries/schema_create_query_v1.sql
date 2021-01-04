-- hellfrog.settings.db.sqlite.CommonPreferencesDAOImpl
CREATE TABLE "common_preferences"
(
    "key"         TEXT    NOT NULL UNIQUE,
    "value"       TEXT    NOT NULL,
    "create_date" INTEGER NOT NULL DEFAULT 0,
    "update_date" INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("key")
);
-- hellfrog.settings.db.sqlite.ServerPreferencesDAOImpl
CREATE TABLE "server_preferences"
(
    "id"          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    "server_id"   INTEGER NOT NULL,
    "key"         TEXT    NOT NULL,
    "value"       TEXT,
    "create_date" INTEGER NOT NULL DEFAULT 0,
    "update_date" INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT "uniq_serv_key" UNIQUE ("server_id", "key")
);
-- hellfrog.settings.db.sqlite.VotesDAOImpl
CREATE TABLE "active_votes"
(
    "vote_id"        INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    "server_id"      INTEGER NOT NULL,
    "text_chat_id"   INTEGER NOT NULL,
    "message_id"     INTEGER NOT NULL,
    "finish_date"    INTEGER NOT NULL,
    "vote_text"      TEXT    NOT NULL,
    "has_timer"      INTEGER NOT NULL DEFAULT 0,
    "is_exceptional" INTEGER NOT NULL DEFAULT 0,
    "has_default"    INTEGER NOT NULL DEFAULT 0,
    "win_threshold"  INTEGER NOT NULL DEFAULT 0,
    "create_date"    INTEGER NOT NULL DEFAULT 0,
    "update_date"    INTEGER NOT NULL DEFAULT 0
);
-- hellfrog.settings.db.sqlite.VotesDAOImpl
CREATE TABLE "vote_points"
(
    "id"              INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    "vote_id"         INTEGER NOT NULL,
    "point_text"      INTEGER NOT NULL,
    "unicode_emoji"   TEXT,
    "custom_emoji_id" INTEGER,
    "create_date"     INTEGER NOT NULL DEFAULT 0,
    "update_date"     INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY ("vote_id") REFERENCES "active_votes" ("vote_id")
);
-- hellfrog.settings.db.sqlite.VotesDAOImpl
CREATE TABLE "vote_roles"
(
    "id"          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    "vote_id"     INTEGER NOT NULL,
    "message_id"  INTEGER NOT NULL,
    "role_id"     INTEGER NOT NULL,
    "create_date" INTEGER NOT NULL DEFAULT 0,
    "update_date" INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY ("vote_id") REFERENCES "active_votes" ("vote_id")
);
CREATE UNIQUE INDEX "uniq_vote_role" ON "vote_roles" (
                                                      "vote_id",
                                                      "role_id"
    );
CREATE INDEX "vote_roles_msg" ON "vote_roles" ("message_id");
-- hellfrog.settings.db.sqlite.BotOwnersDAOImpl
CREATE TABLE "bot_owners"
(
    "user_id"     INTEGER NOT NULL UNIQUE,
    "create_date" INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("user_id")
);
-- hellfrog.settings.db.sqlite.UserRightsDAOImpl
CREATE TABLE "user_rights"
(
    "id"             INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    "server_id"      INTEGER NOT NULL,
    "command_prefix" TEXT    NOT NULL,
    "user_id"        INTEGER NOT NULL,
    "create_date"    INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX "uniq_user_right" ON "user_rights" (
                                                        "server_id",
                                                        "command_prefix",
                                                        "user_id"
    );
-- hellfrog.settings.db.sqlite.RoleRightsDAOImpl
CREATE TABLE "role_rights"
(
    "id"             INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    "server_id"      INTEGER NOT NULL,
    "command_prefix" TEXT    NOT NULL,
    "role_id"        INTEGER NOT NULL,
    "create_date"    INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX "uniq_role_right" ON "role_rights" (
                                                        "server_id",
                                                        "command_prefix",
                                                        "role_id"
    );
-- hellfrog.settings.db.sqlite.ChannelRightsDAOImpl
CREATE TABLE "text_channel_rights"
(
    "id"             INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    "server_id"      INTEGER NOT NULL,
    "command_prefix" TEXT    NOT NULL,
    "channel_id"     INTEGER NOT NULL,
    "create_date"    INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX "uniq_channel_right" ON "text_channel_rights" (
                                                                   "server_id",
                                                                   "command_prefix",
                                                                   "channel_id"
    );
-- hellfrog.settings.db.sqlite.ChannelCategoryRightsDAOImpl
CREATE TABLE "category_rights"
(
    "id"             INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    "server_id"      INTEGER NOT NULL,
    "command_prefix" TEXT    NOT NULL,
    "category_id"    INTEGER NOT NULL,
    "create_date"    INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX "uniq_category_right" ON "category_rights" (
                                                                "server_id",
                                                                "command_prefix",
                                                                "category_id"
    );
-- hellfrog.settings.db.sqlite.WtfAssignDAOImpl
CREATE TABLE "wtf_assigns"
(
    "id"          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    "server_id"   INTEGER NOT NULL,
    "author_id"   INTEGER NOT NULL,
    "target_id"   INTEGER NOT NULL,
    "description" TEXT,
    "image_url"   TEXT,
    "create_date" INTEGER NOT NULL DEFAULT 0,
    "update_date" INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX "uniq_wft_assign" ON "wtf_assigns" (
                                                        "server_id",
                                                        "author_id",
                                                        "target_id"
    );
CREATE TABLE "emoji_total_statistics"
(
    "id"           INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    "server_id"    INTEGER NOT NULL,
    "emoji_id"     INTEGER NOT NULL,
    "usages_count" INTEGER NOT NULL,
    "last_usage"   INTEGER NOT NULL,
    "create_date"  INTEGER NOT NULL DEFAULT 0,
    "update_date"  INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX "uniq_total_emoji_stat" ON "emoji_total_statistics" (
                                                                         "server_id",
                                                                         "emoji_id"
    );
CREATE TABLE "user_total_statistics"
(
    "id"                INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    "server_id"         INTEGER NOT NULL,
    "user_id"           INTEGER NOT NULL,
    "messages_count"    INTEGER NOT NULL DEFAULT 0,
    "last_message_date" INTEGER NOT NULL DEFAULT 0,
    "symbols_count"     INTEGER NOT NULL DEFAULT 0,
    "bytes_count"       INTEGER NOT NULL DEFAULT 0,
    "create_date"       INTEGER NOT NULL DEFAULT 0,
    "update_date"       INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX "uniq_user_total_stat" ON "user_total_statistics" (
                                                                       "server_id",
                                                                       "user_id"
    );
CREATE TABLE "text_channel_total_stats"
(
    "id"                INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    "server_id"         INTEGER NOT NULL,
    "text_channel_id"   INTEGER NOT NULL,
    "messages_count"    INTEGER NOT NULL DEFAULT 0,
    "last_message_date" INTEGER NOT NULL DEFAULT 0,
    "symbols_count"     INTEGER NOT NULL DEFAULT 0,
    "bytes_count"       INTEGER NOT NULL DEFAULT 0,
    "create_date"       INTEGER NOT NULL DEFAULT 0,
    "update_date"       INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX "uniq_txtchan_total_stat" ON "text_channel_total_stats" (
                                                                             "server_id",
                                                                             "text_channel_id"
    );
CREATE TABLE "text_chan_user_total_stats"
(
    "id"                INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    "server_id"         INTEGER NOT NULL,
    "text_channel_id"   INTEGER NOT NULL,
    "user_id"           INTEGER NOT NULL,
    "messages_count"    INTEGER NOT NULL DEFAULT 0,
    "last_message_date" INTEGER NOT NULL DEFAULT 0,
    "symbols_count"     INTEGER NOT NULL DEFAULT 0,
    "bytes_count"       INTEGER NOT NULL DEFAULT 0,
    "create_date"       INTEGER NOT NULL DEFAULT 0,
    "update_date"       INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX "uniq_txtchan_user_total_stat" ON "text_chan_user_total_stats" (
                                                                                    "server_id",
                                                                                    "text_channel_id",
                                                                                    "user_id"
    );
CREATE TABLE "persistence_roles"
(
    "id"          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    "server_id"   INTEGER NOT NULL,
    "user_id"     INTEGER NOT NULL,
    "role_id"     INTEGER NOT NULL,
    "create_date" INTEGER NOT NULL DEFAULT 0,
    "update_date" INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX "uniq_persist_role" ON "persistence_roles" (
                                                                "server_id",
                                                                "user_id",
                                                                "role_id"
    );
CREATE TABLE "auto_promotes"
(
    "server_id"   INTEGER NOT NULL UNIQUE,
    "role_id"     INTEGER NOT NULL,
    "timeout"     INTEGER NOT NULL DEFAULT 0,
    "enabled"     INTEGER NOT NULL DEFAULT 0,
    "create_date" INTEGER NOT NULL DEFAULT 0,
    "update_date" INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("server_id")
);
PRAGMA user_version = 1