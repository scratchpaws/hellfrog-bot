----------------------
-- Create new entities
----------------------
-- hellfrog.settings.db.h2.CommonPreferencesDAOImpl
-- hellfrog.settings.db.entity.CommonPreference
create table `common_preferences`
(
    `KEY`          varchar(60) not null primary key,
    `string_value` varchar(64) not null,
    `long_value`   bigint      not null,
    `create_date`  timestamp   not null default localtimestamp,
    `update_date`  timestamp   not null default localtimestamp
);

comment on table `common_preferences` is 'Common bot settings';
comment on column `common_preferences`.`KEY` is 'Unique settings key';
comment on column `common_preferences`.`string_value` is 'Settings string value';
comment on column `common_preferences`.`long_value` is 'Settings numeric value';
comment on column `common_preferences`.`create_date` is 'Record create date';
comment on column `common_preferences`.`update_date` is 'Record update date';

-- hellfrog.settings.db.h2.ServerPreferencesDAOImpl
-- hellfrog.settings.db.entity.ServerPreference
create table `server_preferences`
(
    `id`           bigint      not null primary key,
    `server_id`    bigint      not null,
    `KEY`          varchar(60) not null,
    `string_value` varchar(64) not null,
    `long_value`   bigint      not null,
    `bool_value`   bigint      not null,
    `date_value`   timestamp   not null,
    `create_date`  timestamp   not null default localtimestamp,
    `update_date`  timestamp   not null default localtimestamp,
    constraint `uniq_serv_key` unique (`KEY`, `server_id`)
);

comment on table `server_preferences` is 'Settings for discord servers';
comment on column `server_preferences`.`id` is 'Unique record ID';
comment on column `server_preferences`.`server_id` is 'Discord server ID';
comment on column `server_preferences`.`KEY` is 'Unique settings key';
comment on column `server_preferences`.`string_value` is 'Settings string value';
comment on column `server_preferences`.`long_value` is 'Settings numeric value';
comment on column `server_preferences`.`bool_value` is 'Settings logical value';
comment on column `server_preferences`.`date_value` is 'Settings date and time value';
comment on column `server_preferences`.`create_date` is 'Record create date';
comment on column `server_preferences`.`update_date` is 'Record update date';

create sequence `server_preference_ids` start with 1 increment by 50;

-- hellfrog.settings.db.h2.VotesDAOImpl
-- hellfrog.settings.db.entity.Vote
create table `active_votes`
(
    `id`             bigint    not null primary key,
    `server_id`      bigint    not null,
    `text_chat_id`   bigint    not null,
    `message_id`     bigint    not null default 0,
    `vote_text`      varchar(2000),
    `has_timer`      bigint    not null default 0,
    `finish_date`    timestamp,
    `is_exceptional` bigint    not null default 0,
    `has_default`    bigint    not null default 0,
    `win_threshold`  bigint    not null default 0,
    `create_date`    timestamp not null default localtimestamp,
    `update_date`    timestamp not null default localtimestamp
);

comment on table `active_votes` is 'Current active voices';
comment on column `active_votes`.`id` is 'Unique record ID';
comment on column `active_votes`.`server_id` is 'Discord server ID';
comment on column `active_votes`.`text_chat_id` is 'Discord text chat ID, what contain vote message';
comment on column `active_votes`.`message_id` is 'Discord message ID, what contain vote';
comment on column `active_votes`.`vote_text` is 'Voting text';
comment on column `active_votes`.`has_timer` is 'A flag indicating the presence of the voting expiration date (0 - no, 1 - yes)';
comment on column `active_votes`.`finish_date` is 'The date after which the voting will automatically stop';
comment on column `active_votes`.`is_exceptional` is 'A flag indicating that the user can choose only one item in the vote. (0 - no, 1 - yes)';
comment on column `active_votes`.`has_default` is 'A flag indicating that the first item in the vote` is the default item. (0 - no, 1 - yes)';
comment on column `active_votes`.`win_threshold` is 'Numeric threshold for single choice with default vote point and second point';
comment on column `active_votes`.`create_date` is 'Record create date';
comment on column `active_votes`.`update_date` is 'Record update date';

create sequence `active_vote_ids` start with 1 increment by 50;

-- hellfrog.settings.db.h2.VotesDAOImpl
-- hellfrog.settings.db.entity.VotePoint
create table `vote_points`
(
    `id`              bigint        not null primary key,
    `vote_id`         bigint        not null,
    `point_text`      varchar(2000) not null,
    `unicode_emoji`   varchar(12),
    `custom_emoji_id` bigint,
    `create_date`     timestamp     not null default localtimestamp,
    `update_date`     timestamp     not null default localtimestamp,
    constraint `vote_point_fk` foreign key (`vote_id`) references `active_votes` (`id`)
);

comment on table `vote_points` is 'Voting points';
comment on column `vote_points`.`id` is 'Unique record ID';
comment on column `vote_points`.`vote_id` is 'Vote ID from active_votes.id';
comment on column `vote_points`.`point_text` is 'Text describing the voting point';
comment on column `vote_points`.`unicode_emoji` is 'Reaction for selecting an item from a list of standard Unicode emojis';
comment on column `vote_points`.`custom_emoji_id` is 'Reaction for selecting an item from the server''s emoji list (Discord custom emoji ID)';
comment on column `vote_points`.`create_date` is 'Record create date';
comment on column `vote_points`.`update_date` is 'Record update date';

create sequence `vote_point_ids` start with 1 increment by 50;

-- hellfrog.settings.db.h2.VotesDAOImpl
-- hellfrog.settings.db.entity.VoteRoleFilter
create table `vote_roles`
(
    `id`          bigint    not null primary key,
    `vote_id`     bigint    not null,
    `message_id`  bigint    not null default 0,
    `role_id`     bigint    not null,
    `create_date` timestamp not null default localtimestamp,
    `update_date` timestamp not null default localtimestamp,
    constraint `uniq_vote_role` unique (`vote_id`,
                                        `role_id`),
    constraint `vote_role_fk` foreign key (`vote_id`) references `active_votes` (`id`)
);

comment on table `vote_roles` is 'Roles of members who can vote';
comment on column `vote_roles`.`id` is 'Unique record ID';
comment on column `vote_roles`.`vote_id` is 'Vote ID from active_votes.id';
comment on column `vote_roles`.`message_id` is 'Discord message ID, what contain vote (indexed)';
comment on column `vote_roles`.`role_id` is 'Discord member role ID';
comment on column `vote_roles`.`create_date` is 'Record create date';
comment on column `vote_roles`.`update_date` is 'Record update date';

create index `vote_roles_msg` on `vote_roles` (`message_id`);
create sequence `vote_role_ids` start with 1 increment by 50;

-- hellfrog.settings.db.h2.BotOwnersDAOImpl
-- hellfrog.settings.db.entity.BotOwner
create table `bot_owners`
(
    `user_id`     bigint    not null primary key,
    `create_date` timestamp not null default localtimestamp
);

comment on table `bot_owners` is 'Users who can control the bot with the same rights as the creator-owner';
comment on column `bot_owners`.`user_id` is 'Discord user ID';
comment on column `bot_owners`.`create_date` is 'Record create date';

-- hellfrog.settings.db.h2.UserRightsDAOImpl
-- hellfrog.settings.db.entity.UserRight
create table `user_rights`
(
    `id`             bigint      not null primary key,
    `server_id`      bigint      not null,
    `user_id`        bigint      not null,
    `command_prefix` varchar(20) not null,
    `create_date`    timestamp   not null default localtimestamp,
    constraint `uniq_user_right` unique (`server_id`, `command_prefix`, `user_id`)
);

comment on table `user_rights` is 'List of members who have access to any commands of the bot';
comment on column `user_rights`.`id` is 'Unique record ID';
comment on column `user_rights`.`server_id` is 'Discord server ID';
comment on column `user_rights`.`user_id` is 'Discord member ID';
comment on column `user_rights`.`command_prefix` is 'Bot command prefix';
comment on column `user_rights`.`create_date` is 'Record create date';

create sequence `user_right_ids` start with 1 increment by 50;

-- hellfrog.settings.db.h2.RoleRightsDAOImpl
-- hellfrog.settings.db.entity.RoleRight
create table `role_rights`
(
    `id`             bigint      not null primary key,
    `server_id`      bigint      not null,
    `role_id`        bigint      not null,
    `command_prefix` varchar(20) not null,
    `create_date`    timestamp   not null default localtimestamp,
    constraint `uniq_role_right` unique (`server_id`, `command_prefix`, `role_id`)
);

comment on table `role_rights` is 'List of roles who have access to any commands of the bot';
comment on column `role_rights`.`id` is 'Unique record ID';
comment on column `role_rights`.`server_id` is 'Discord server ID';
comment on column `role_rights`.`role_id` is 'Discord members role ID';
comment on column `role_rights`.`command_prefix` is 'Bot command prefix';
comment on column `role_rights`.`create_date` is 'Record create date';

create sequence `role_right_ids` start with 1 increment by 50;

-- hellfrog.settings.db.h2.ChannelRightsDAOImpl
-- hellfrog.settings.db.entity.ChannelRight
create table `channel_rights`
(
    `id`             bigint      not null primary key,
    `server_id`      bigint      not null,
    `channel_id`     bigint      not null,
    `command_prefix` varchar(20) not null,
    `create_date`    timestamp   not null default localtimestamp,
    constraint `uniq_channel_right` unique (`server_id`, `command_prefix`, `channel_id`)
);

comment on table `channel_rights` is 'List of server channels where allowed execute any commands of the bot';
comment on column `channel_rights`.`id` is 'Unique record ID';
comment on column `channel_rights`.`server_id` is 'Discord server ID';
comment on column `channel_rights`.`channel_id` is 'Discord channel ID';
comment on column `channel_rights`.`command_prefix` is 'Bot command prefix';
comment on column `channel_rights`.`create_date` is 'Record create date';

create sequence `channel_right_ids` start with 1 increment by 50;

-- hellfrog.settings.db.h2.ChannelCategoryRightsDAOImpl
-- hellfrog.settings.db.entity.CategoryRight
create table `category_rights`
(
    `id`             bigint      not null primary key,
    `server_id`      bigint      not null,
    `category_id`    bigint      not null,
    `command_prefix` varchar(20) not null,
    `create_date`    timestamp   not null default localtimestamp,
    constraint `uniq_category_right` unique (`server_id`, `command_prefix`, `category_id`)
);

comment on table `category_rights` is 'List of server channels categories where allowed execute any commands of the bot';
comment on column `category_rights`.`id` is 'Unique record ID';
comment on column `category_rights`.`server_id` is 'Discord server ID';
comment on column `category_rights`.`category_id` is 'Discord channels category ID';
comment on column `category_rights`.`command_prefix` is 'Bot command prefix';
comment on column `category_rights`.`create_date` is 'Record create date';

create sequence `category_right_ids` start with 1 increment by 50;

-- hellfrog.settings.db.h2.WtfAssignDAOImpl
-- hellfrog.settings.db.entity.WtfEntry
create table `wtf_assigns`
(
    `id`          bigint    not null primary key,
    `server_id`   bigint    not null,
    `author_id`   bigint    not null,
    `target_id`   bigint    not null,
    `description` varchar(2000),
    `create_date` timestamp not null default localtimestamp,
    `update_date` timestamp not null default localtimestamp,
    constraint `uniq_wft_assign` unique (`server_id`,
                                       `author_id`,
                                       `target_id`)
);

comment on table `wtf_assigns` is 'Comments left by members about other members on the server';
comment on column `wtf_assigns`.`id` is 'Unique record ID';
comment on column `wtf_assigns`.`server_id` is 'Discord server ID';
comment on column `wtf_assigns`.`author_id` is 'Discord member ID by comment author';
comment on column `wtf_assigns`.`target_id` is 'Discord member ID by member about whom the comment was written';
comment on column `wtf_assigns`.`description` is 'Comment message';
comment on column `wtf_assigns`.`create_date` is 'Record create date';
comment on column `wtf_assigns`.`update_date` is 'Record update date';

create sequence `wtf_assign_ids` start with 1 increment by 50;

-- hellfrog.settings.db.h2.WtfAssignDAOImpl
-- hellfrog.settings.db.entity.WtfEntryAttach
create table `wtf_assigns_attaches`
(
    `id`          bigint        not null primary key,
    `entry_id`    bigint        not null,
    `uri`         varchar(2000) not null,
    `create_date` timestamp     not null default localtimestamp,
    constraint `wtf_attach_fk` foreign key (`entry_id`) references `wtf_assigns` (`id`),
    constraint `wtf_assigns_attaches` unique (`entry_id`, `uri`)
);

comment on table `wtf_assigns_attaches` is 'Attaches for comments that left by members';
comment on column `wtf_assigns_attaches`.`id` is 'Unique record ID';
comment on column `wtf_assigns_attaches`.`entry_id` is 'Comment ID. See wtf_assigns.id';
comment on column `wtf_assigns_attaches`.`uri` is 'Attachment URI';
comment on column `wtf_assigns_attaches`.`create_date` is 'Record create date';

create sequence `wtf_assigns_attach_ids` start with 1 increment by 50;

-- hellfrog.settings.db.h2.TotalStatisticDAOImpl
-- hellfrog.settings.db.entity.EmojiTotalStatistic
create table `emoji_total_statistics`
(
    `id`           bigint    not null primary key,
    `server_id`    bigint    not null,
    `emoji_id`     bigint    not null,
    `usages_count` bigint    not null default 0,
    `last_usage`   timestamp not null default localtimestamp,
    `create_date`  timestamp not null default localtimestamp,
    `update_date`  timestamp not null default localtimestamp,
    constraint `uniq_total_emoji_stat` unique (server_id,
                                             emoji_id)
);

comment on table `emoji_total_statistics` is 'Custom Discord emoji total statistics by servers';
comment on column `emoji_total_statistics`.`id` is 'Unique record ID';
comment on column `emoji_total_statistics`.`server_id` is 'Discord server ID';
comment on column `emoji_total_statistics`.`emoji_id` is 'Discord custom emoji ID';
comment on column `emoji_total_statistics`.`usages_count` is 'Custom emoji usages count';
comment on column `emoji_total_statistics`.`last_usage` is 'Custom emoji last usage';
comment on column `emoji_total_statistics`.`create_date` is 'Record create date';
comment on column `emoji_total_statistics`.`update_date` is 'Record update date';

create sequence `emoji_total_stat_ids` start with 1 increment by 50;

-- hellfrog.settings.db.h2.TotalStatisticDAOImpl
-- hellfrog.settings.db.entity.TextChannelTotalStatistic
create table `text_channel_total_stats`
(
    `id`                bigint    not null primary key,
    `server_id`         bigint    not null,
    `text_channel_id`   bigint    not null,
    `user_id`           bigint    not null,
    `messages_count`    bigint    not null default 0,
    `last_message_date` timestamp not null default localtimestamp,
    `symbols_count`     bigint    not null default 0,
    `bytes_count`       bigint    not null default 0,
    `create_date`       timestamp not null default localtimestamp,
    `update_date`       timestamp not null default localtimestamp,
    constraint `uniq_text_channel_total_stat` unique (`server_id`, `text_channel_id`, `user_id`)
);

comment on table `text_channel_total_stats` is 'General statistics on messages and members in channels';
comment on column `text_channel_total_stats`.`id` is 'Unique record ID';
comment on column `text_channel_total_stats`.`server_id` is 'Discord server ID';
comment on column `text_channel_total_stats`.`text_channel_id` is 'Discord channel ID';
comment on column `text_channel_total_stats`.`user_id` is 'Discord member ID';
comment on column `text_channel_total_stats`.`messages_count` is 'Total messages count';
comment on column `text_channel_total_stats`.`last_message_date` is 'Last message date by user in channel';
comment on column `text_channel_total_stats`.`symbols_count` is 'Total symbols count';
comment on column `text_channel_total_stats`.`bytes_count` is 'Total bytes count';
comment on column `text_channel_total_stats`.`create_date` is 'Record create date';
comment on column `text_channel_total_stats`.`update_date` is 'Record update date';

create sequence `text_channel_total_stat_idx` start with 1 increment by 50;

-- hellfrog.settings.db.h2.AutoPromoteRolesDAOImpl
-- hellfrog.settings.db.entity.AutoPromoteConfig
create table `auto_promote_configs`
(
    `id`          bigint    not null primary key,
    `server_id`   bigint    not null,
    `role_id`     bigint    not null,
    `timeout`     bigint    not null default 0,
    `create_date` timestamp not null default localtimestamp,
    constraint `uniq_auto_promote_role_cfg` unique (`server_id`, `role_id`)
);

comment on table `auto_promote_configs` is 'List of auto-assigned roles';
comment on column `auto_promote_configs`.`id` is 'Unique record ID';
comment on column `auto_promote_configs`.`server_id` is 'Discord server ID';
comment on column `auto_promote_configs`.`role_id` is 'Discord server role ID';
comment on column `auto_promote_configs`.`timeout` is 'The time, in seconds, after which all new members are assigned a role';
comment on column `auto_promote_configs`.`create_date` is 'Record create date';

create sequence `auto_promote_config_idx` start with 1 increment by 50;

-- hellfrog.settings.db.h2.RoleAssignDAOImpl
-- hellfrog.settings.db.entity.RoleAssign
create table `role_assign_queue`
(
    `id`          bigint    not null primary key,
    `server_id`   bigint    not null,
    `user_id`     bigint    not null,
    `role_id`     bigint    not null,
    `assign_date` timestamp not null,
    `create_date` timestamp not null default localtimestamp
);

comment on table `role_assign_queue` is 'A queue of assigned roles for server members';
comment on column `role_assign_queue`.`id` is 'Unique record ID';
comment on column `role_assign_queue`.`server_id` is 'Discord server ID';
comment on column `role_assign_queue`.`user_id` is 'Discord member ID';
comment on column `role_assign_queue`.`role_id` is 'Discord server role ID';
comment on column `role_assign_queue`.`assign_date` is 'Date and time the specified role was assigned';
comment on column `role_assign_queue`.`create_date` is 'Record create date';

create sequence `role_assign_queue_idx` start with 1 increment by 50;

-- hellfrog.settings.db.h2.EntityNameCacheDAOImpl
-- hellfrog.settings.db.entity.EntityNameCache
create table `names_cache`
(
    `entity_id`   bigint       not null primary key,
    `entity_name` varchar(120) not null,
    `entity_type` varchar(30)  not null,
    `create_date` timestamp    not null default localtimestamp,
    `update_date` timestamp    not null default localtimestamp
);

comment on table `names_cache` is 'Discord entity names global cache';
comment on column `names_cache`.`entity_id` is 'Discord entity ID';
comment on column `names_cache`.`entity_name` is 'Discord entity display name. For users use discrimination name';
comment on column `names_cache`.`entity_type` is 'Discord entity type name';
comment on column `names_cache`.`create_date` is 'Record create date';
comment on column `names_cache`.`update_date` is 'Record update date';

-- hellfrog.settings.db.h2.EntityNameCacheDAOImpl
-- hellfrog.settings.db.entity.ServerNameCache
create table `server_names_cache`
(
    `id` bigint not null primary key,
    `server_id` bigint not null,
    `entity_id` bigint not null,
    `entity_name` varchar(120) not null,
    `create_date` timestamp    not null default localtimestamp,
    `update_date` timestamp    not null default localtimestamp,
    constraint `uniq_server_name` unique (`server_id`, `entity_id`)
);

comment on table `server_names_cache` is 'Discord servers entity display names cache';
comment on column `server_names_cache`.`id` is 'Unique record ID';
comment on column `server_names_cache`.`server_id` is 'Discord server ID';
comment on column `server_names_cache`.`entity_id` is 'Discord entity ID';
comment on column `server_names_cache`.`entity_name` is 'Discord entity display name';
comment on column `server_names_cache`.`create_date` is 'Record create date';
comment on column `server_names_cache`.`update_date` is 'Record update date';

create sequence `server_name_cache_idx` start with 1 increment by 50;

-- hellfrog.settings.db.h2.CommunityControlDAOImpl
-- hellfrog.settings.db.entity.CommunityControlSettings
create table `community_control_settings`
(
    `id`                 bigint       not null    primary key,
    `server_id`          bigint       not null,
    `assign_role_id`     bigint       not null    default 0,
    `threshold`          bigint       not null    default 0,
    `unicode_emoji`      varchar(12),
    `custom_emoji_id`    bigint       not null    default 0,
    `create_date`        timestamp    not null    default localtimestamp,
    `update_date`        timestamp    not null    default localtimestamp,
    constraint `uniq_community_control` unique (`server_id`)
);

comment on table `community_control_settings` is 'Community users control settings';
comment on column `community_control_settings`.`id` is 'Unique record ID';
comment on column `community_control_settings`.`server_id` is 'Discord server ID';
comment on column `community_control_settings`.`assign_role_id` is 'Role ID that obtained by exceeding the number of reactions';
comment on column `community_control_settings`.`threshold` is 'The threshold at which the role is assigned';
comment on column `community_control_settings`.`unicode_emoji` is 'Unicode emoji for which the role is assigned';
comment on column `community_control_settings`.`custom_emoji_id` is 'Discord custom emoji ID for which the role is assigned';
comment on column `community_control_settings`.`create_date` is 'Record create date';
comment on column `community_control_settings`.`update_date` is 'Record update date';

create sequence `community_control_setting_idx` start with 1 increment by 50;

-- hellfrog.settings.db.h2.CommunityControlDAOImpl
-- hellfrog.settings.db.entity.CommunityControlUser
create table `community_control_users`
(
    `id`             bigint       not null    primary key,
    `server_id`      bigint       not null,
    `user_id`        bigint       not null,
    `create_date`    timestamp    not null    default localtimestamp,
    constraint `uniq_community_control_user` unique (`server_id`, `user_id`)
);

create index `community_control_users_srv_idx` on community_control_users (server_id);

comment on table `community_control_users` is 'User for community control';
comment on column `community_control_users`.`id` is 'Unique record ID';
comment on column `community_control_users`.`server_id` is 'Discord server ID';
comment on column `community_control_users`.`user_id` is 'Discord server member ID';
comment on column `community_control_users`.`create_date` is 'Record create date';

create sequence `community_control_user_idx` start with 1 increment by 50;

-----------------------------
-- Schema versions table init
-----------------------------
create table `schema_versions`
(
    `version`     bigint not null primary key,
    `script_name` varchar(120),
    `apply_date`  timestamp default localtimestamp,
    constraint `schema_ver_uniq_script` unique (`script_name`)
);

comment on table `schema_versions` is 'A table listing all installed database migration schemes. The current version is the largest version of the script number.';
comment on column `schema_versions`.`version` is 'Migration script version number';
comment on column `schema_versions`.`script_name` is 'Migration script name';
comment on column `schema_versions`.`apply_date` is 'Migration script execution date and time';

-- Current schema version
insert into `schema_versions` (`version`, `script_name`)
values (1, '001_schema_create_query.sql');
