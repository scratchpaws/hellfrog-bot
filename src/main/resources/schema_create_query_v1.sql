CREATE TABLE "common_preferences" (
	"key"	TEXT NOT NULL UNIQUE,
	"value"	TEXT,
	"create_date"	INTEGER NOT NULL DEFAULT 0,
	"update_date"	INTEGER NOT NULL DEFAULT 0,
	PRIMARY KEY("key")
);
CREATE TABLE "server_preferences" (
	"id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
	"server_id"	INTEGER NOT NULL,
	"key"	INTEGER NOT NULL,
	"value"	INTEGER,
	"create_date"	INTEGER NOT NULL DEFAULT 0,
	"update_date"	INTEGER NOT NULL DEFAULT 0,
	CONSTRAINT "uniq_serv_key" UNIQUE ("server_id","key")
);
CREATE TABLE "active_votes" (
	"vote_id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
	"server_id"	INTEGER NOT NULL,
	"text_chat_id"	INTEGER NOT NULL,
	"message_id"	INTEGER NOT NULL,
	"finish_date"	INTEGER NOT NULL,
	"vote_text"	TEXT NOT NULL,
	"has_timer"	INTEGER NOT NULL DEFAULT 0,
	"is_exceptional"	INTEGER NOT NULL DEFAULT 0,
	"has_default"	INTEGER NOT NULL DEFAULT 0,
	"win_threshold"	INTEGER NOT NULL DEFAULT 0,
	"create_date"	INTEGER NOT NULL DEFAULT 0,
	"update_date"	INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE "vote_points" (
	"id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
	"vote_id"	INTEGER NOT NULL,
	"point_text"	INTEGER NOT NULL,
	"unicode_emoji"	TEXT,
	"custom_emoji_id"	INTEGER,
	"create_date"	INTEGER NOT NULL DEFAULT 0,
	"update_date"	INTEGER NOT NULL DEFAULT 0,
	FOREIGN KEY("vote_id") REFERENCES "active_votes"("vote_id")
);
CREATE TABLE "bot_owners" (
	"user_id"	INTEGER NOT NULL UNIQUE,
	"create_date"	INTEGER NOT NULL DEFAULT 0,
    "update_date"	INTEGER NOT NULL DEFAULT 0,
	PRIMARY KEY("user_id")
);
CREATE TABLE "user_rights" (
	"id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
	"server_id"	INTEGER NOT NULL,
	"command_prefix"	TEXT NOT NULL,
	"user_id"	INTEGER NOT NULL,
	"create_date"	INTEGER NOT NULL DEFAULT 0,
	"update_date"	INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE "role_rights" (
	"id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
	"server_id"	INTEGER NOT NULL,
	"command_prefix"	TEXT NOT NULL,
	"user_id"	INTEGER NOT NULL,
	"create_date"	INTEGER NOT NULL DEFAULT 0,
	"update_date"	INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE "text_channel_rights" (
	"id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
	"server_id"	INTEGER NOT NULL,
	"command_prefix"	TEXT NOT NULL,
	"user_id"	INTEGER NOT NULL,
	"create_date"	INTEGER NOT NULL DEFAULT 0,
	"update_date"	INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE "category_rights" (
	"id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
	"server_id"	INTEGER NOT NULL,
	"command_prefix"	TEXT NOT NULL,
	"user_id"	INTEGER NOT NULL,
	"create_date"	INTEGER NOT NULL DEFAULT 0,
	"update_date"	INTEGER NOT NULL DEFAULT 0
);