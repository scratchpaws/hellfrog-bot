-----------------------------
-- Schema versions table init
-----------------------------
create table schema_versions
(
    version     bigint not null primary key,
    script_name varchar(60),
    apply_date  timestamp default sysdate,
    constraint schema_ver_uniq_script unique (script_name)
);

-- Current schema version
insert into schema_versions (version, script_name)
values (1, '001_schema_create_query.sql');

----------------------
-- Create new entities
----------------------
-- hellfrog.settings.db.h2.CommonPreferencesDAOImpl
-- hellfrog.settings.db.entity.CommonPreference
create table common_preferences
(
    key          varchar(60) not null primary key,
    string_value varchar(64) not null,
    long_value   bigint      not null,
    create_date  timestamp   not null default sysdate,
    update_date  timestamp   not null default sysdate
);

comment on table common_preferences is 'Common bot settings';
comment on column common_preferences.KEY is 'Unique settings key';
comment on column common_preferences.string_value is 'Settings string value';
comment on column common_preferences.long_value is 'Settings numeric value';
comment on column common_preferences.create_date is 'Record create date';
comment on column common_preferences.update_date is 'Record update date';

-- hellfrog.settings.db.h2.ServerPreferencesDAOImpl
-- hellfrog.settings.db.entity.ServerPreference
create table server_preferences
(
    id           bigint      not null primary key,
    server_id    bigint      not null,
    key          varchar(60) not null,
    string_value varchar(64) not null,
    long_value   bigint      not null,
    bool_value   bigint      not null,
    create_date  timestamp   not null default sysdate,
    update_date  timestamp   not null default sysdate,
    constraint uniq_serv_key unique (key, server_id)
);

comment on table server_preferences is 'Settings for discord servers';
comment on column server_preferences.id is 'Unique record ID';
comment on column server_preferences.server_id is 'Discord server ID';
comment on column server_preferences.string_value is 'Settings string value';
comment on column server_preferences.long_value is 'Settings numeric value';
comment on column server_preferences.bool_value is 'Settings logical value';
comment on column server_preferences.create_date is 'Record create date';
comment on column server_preferences.update_date is 'Record update date';

create sequence server_preference_ids start with 1 increment by 50;

-- hellfrog.settings.db.h2.VotesDAOImpl
-- hellfrog.settings.db.entity.Vote
create table active_votes
(
    id             bigint    not null primary key,
    server_id      bigint    not null,
    text_chat_id   bigint    not null,
    message_id     bigint    not null default 0,
    vote_text      varchar(2000),
    has_timer      bigint    not null default 0,
    finish_date    timestamp,
    is_exceptional bigint    not null default 0,
    has_default    bigint    not null default 0,
    win_threshold  bigint    not null default 0,
    create_date    timestamp not null default sysdate,
    update_date    timestamp not null default sysdate
);

comment on table active_votes is 'Current active voices';
comment on column active_votes.id is 'Unique record ID';
comment on column active_votes.server_id is 'Discord server ID';
comment on column active_votes.text_chat_id is 'Discord text chat ID, what contain vote message';
comment on column active_votes.message_id is 'Discord message ID, what contain vote';
comment on column active_votes.vote_text is 'Voting text';
comment on column active_votes.has_timer is 'A flag indicating the presence of the voting expiration date (0 - no, 1 - yes)';
comment on column active_votes.finish_date is 'The date after which the voting will automatically stop';
comment on column active_votes.is_exceptional is 'A flag indicating that the user can choose only one item in the vote. (0 - no, 1 - yes)';
comment on column active_votes.has_default is 'A flag indicating that the first item in the vote is the default item. (0 - no, 1 - yes)';
comment on column active_votes.win_threshold is 'Numeric threshold for single choice with default vote point and second point';
comment on column active_votes.create_date is 'Record create date';
comment on column active_votes.update_date is 'Record update date';

create sequence active_vote_ids start with 1 increment by 50;

-- hellfrog.settings.db.h2.VotesDAOImpl
-- hellfrog.settings.db.entity.VotePoint
create table vote_points
(
    id              bigint        not null primary key,
    vote_id         bigint        not null,
    point_text      varchar(2000) not null,
    unicode_emoji   varchar(12),
    custom_emoji_id bigint,
    create_date     timestamp     not null default sysdate,
    update_date     timestamp     not null default sysdate,
    constraint vote_point_fk foreign key (vote_id) references active_votes (id)
);

comment on table vote_points is 'Voting points';
comment on column vote_points.id is 'Unique record ID';
comment on column vote_points.vote_id is 'Vote ID from active_votes.id';
comment on column vote_points.point_text is 'Text describing the voting point';
comment on column vote_points.unicode_emoji is 'Reaction for selecting an item from a list of standard Unicode emojis';
comment on column vote_points.custom_emoji_id is 'Reaction for selecting an item from the server''s emoji list (Discord custom emoji ID)';
comment on column vote_points.create_date is 'Record create date';
comment on column vote_points.update_date is 'Record update date';

create sequence vote_point_ids start with 1 increment by 50;

-- hellfrog.settings.db.h2.VotesDAOImpl
-- hellfrog.settings.db.entity.VoteRoleFilter
create table vote_roles
(
    id          bigint    not null primary key,
    vote_id     bigint    not null,
    message_id  bigint    not null default 0,
    role_id     bigint    not null,
    create_date timestamp not null default sysdate,
    update_date timestamp not null default sysdate,
    constraint uniq_vote_role unique (vote_id,
                                      role_id),
    constraint vote_role_fk foreign key (vote_id) references active_votes (id)
);

comment on table vote_roles is 'Roles of members who can vote';
comment on column vote_roles.id is 'Unique record ID';
comment on column vote_roles.vote_id is 'Vote ID from active_votes.id';
comment on column vote_roles.message_id is 'Discord message ID, what contain vote (indexed)';
comment on column vote_roles.role_id is 'Discord member role ID';
comment on column vote_roles.create_date is 'Record create date';
comment on column vote_roles.update_date is 'Record update date';

create index vote_roles_msg on vote_roles (message_id);
create sequence vote_role_ids start with 1 increment by 50;

-- hellfrog.settings.db.h2.BotOwnersDAOImpl
-- hellfrog.settings.db.entity.BotOwner
create table bot_owners
(
    user_id     bigint    not null primary key,
    create_date timestamp not null default sysdate
);

comment on table bot_owners is 'Users who can control the bot with the same rights as the creator-owner';
comment on column bot_owners.user_id is 'Discord user ID';
comment on column bot_owners.create_date is 'Record create date';

-- hellfrog.settings.db.h2.UserRightsDAOImpl
-- hellfrog.settings.db.entity.UserRight
create table user_rights
(
    id             bigint      not null primary key,
    server_id      bigint      not null,
    user_id        bigint      not null,
    command_prefix varchar(20) not null,
    create_date    timestamp   not null default sysdate,
    constraint uniq_user_right unique (server_id, command_prefix, user_id)
);

comment on table user_rights is 'List of members who have access to any commands of the bot';
comment on column user_rights.id is 'Unique record ID';
comment on column user_rights.server_id is 'Discord server ID';
comment on column user_rights.user_id is 'Discord member ID';
comment on column user_rights.command_prefix is 'Bot command prefix';
comment on column user_rights.create_date is 'Record create date';

create sequence user_right_ids start with 1 increment by 50;

-- hellfrog.settings.db.h2.RoleRightsDAOImpl
-- hellfrog.settings.db.entity.RoleRight
create table role_rights
(
    id             bigint      not null primary key,
    server_id      bigint      not null,
    role_id        bigint      not null,
    command_prefix varchar(20) not null,
    create_date    timestamp   not null default sysdate,
    constraint uniq_role_right unique (server_id, command_prefix, role_id)
);

comment on table role_rights is 'List of roles who have access to any commands of the bot';
comment on column role_rights.id is 'Unique record ID';
comment on column role_rights.server_id is 'Discord server ID';
comment on column role_rights.role_id is 'Discord members role ID';
comment on column role_rights.command_prefix is 'Bot command prefix';
comment on column role_rights.create_date is 'Record create date';

create sequence role_right_ids start with 1 increment by 50;

-- hellfrog.settings.db.h2.ChannelRightsDAOImpl
-- hellfrog.settings.db.entity.ChannelRight
create table channel_rights
(
    id             bigint      not null primary key,
    server_id      bigint      not null,
    channel_id     bigint      not null,
    command_prefix varchar(20) not null,
    create_date    timestamp   not null default sysdate,
    constraint uniq_channel_right unique (server_id, command_prefix, channel_id)
);

comment on table channel_rights is 'List of server channels where allowed execute any commands of the bot';
comment on column channel_rights.id is 'Unique record ID';
comment on column channel_rights.server_id is 'Discord server ID';
comment on column channel_rights.channel_id is 'Discord channel ID';
comment on column channel_rights.command_prefix is 'Bot command prefix';
comment on column channel_rights.create_date is 'Record create date';

create sequence channel_right_ids start with 1 increment by 50;

-- hellfrog.settings.db.h2.ChannelCategoryRightsDAOImpl
-- hellfrog.settings.db.entity.CategoryRight
create table category_rights
(
    id             bigint      not null primary key,
    server_id      bigint      not null,
    category_id    bigint      not null,
    command_prefix varchar(20) not null,
    create_date    timestamp   not null default sysdate,
    constraint uniq_category_right unique (server_id, command_prefix, category_id)
);

comment on table category_rights is 'List of server channels categories where allowed execute any commands of the bot';
comment on column category_rights.id is 'Unique record ID';
comment on column category_rights.server_id is 'Discord server ID';
comment on column category_rights.category_id is 'Discord channels category ID';
comment on column category_rights.command_prefix is 'Bot command prefix';
comment on column category_rights.create_date is 'Record create date';

create sequence category_right_ids start with 1 increment by 50;

-- hellfrog.settings.db.h2.WtfAssignDAOImpl
-- hellfrog.settings.db.entity.WtfEntry
create table wtf_assigns
(
    id          bigint    not null primary key,
    server_id   bigint    not null,
    author_id   bigint    not null,
    target_id   bigint    not null,
    description varchar(2000),
    create_date timestamp not null default sysdate,
    update_date timestamp not null default sysdate,
    constraint uniq_wft_assign unique (server_id,
                                       author_id,
                                       target_id)
);

comment on table wtf_assigns is 'Comments left by members about other members on the server';
comment on column wtf_assigns.id is 'Unique record ID';
comment on column wtf_assigns.server_id is 'Discord server ID';
comment on column wtf_assigns.author_id is 'Discord member ID by comment author';
comment on column wtf_assigns.target_id is 'Discord member ID by member about whom the comment was written';
comment on column wtf_assigns.description is 'Comment message';
comment on column wtf_assigns.create_date is 'Record create date';
comment on column wtf_assigns.update_date is 'Record update date';

create sequence wtf_assign_ids start with 1 increment by 50;

-- hellfrog.settings.db.h2.WtfAssignDAOImpl
-- hellfrog.settings.db.entity.WtfEntryAttach
create table wtf_assigns_attaches
(
    id          bigint        not null primary key,
    entry_id    bigint        not null,
    uri         varchar(2000) not null,
    create_date timestamp     not null default sysdate,
    constraint wtf_attach_fk foreign key (entry_id) references wtf_assigns (id),
    constraint wtf_assigns_attaches unique (entry_id, uri)
);

comment on table wtf_assigns_attaches is 'Attaches for comments that left by members';
comment on column wtf_assigns_attaches.id is 'Unique record ID';
comment on column wtf_assigns_attaches.entry_id is 'Comment ID. See wtf_assigns.id';
comment on column wtf_assigns_attaches.uri is 'Attachment URI';
comment on column wtf_assigns_attaches.create_date is 'Record create date';

create sequence wtf_assigns_attach_ids start with 1 increment by 50;

-- hellfrog.settings.db.h2.EmojiTotalStatisticDAOImpl
-- hellfrog.settings.db.entity.EmojiTotalStatistic
create table emoji_total_statistics
(
    id           bigint    not null primary key,
    server_id    bigint    not null,
    emoji_id     bigint    not null,
    usages_count bigint    not null default 0,
    last_usage   timestamp not null default sysdate,
    create_date  timestamp not null default sysdate,
    update_date  timestamp not null default sysdate,
    constraint uniq_total_emoji_stat unique (server_id,
                                             emoji_id)
);

comment on table emoji_total_statistics is 'Custom Discord emoji total statistics by servers';
comment on column emoji_total_statistics.id is 'Unique record ID';
comment on column emoji_total_statistics.server_id is 'Discord server ID';
comment on column emoji_total_statistics.emoji_id is 'Discord custom emoji ID';
comment on column emoji_total_statistics.usages_count is 'Custom emoji usages count';
comment on column emoji_total_statistics.last_usage is 'Custom emoji last usage';
comment on column emoji_total_statistics.create_date is 'Record create date';
comment on column emoji_total_statistics.update_date is 'Record update date';

create sequence emoji_total_stat_ids start with 1 increment by 50;