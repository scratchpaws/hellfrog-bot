create sequence active_vote_ids start with 1 increment by 50;
create sequence server_preference_ids start with 1 increment by 50;
create sequence vote_point_ids start with 1 increment by 50;
create sequence vote_role_ids start with 1 increment by 50;

    create table active_votes (
       id bigint not null,
        create_date timestamp not null,
        is_exceptional bigint not null,
        finish_date timestamp,
        has_default bigint not null,
        has_timer bigint not null,
        message_id bigint not null,
        server_id bigint not null,
        text_chat_id bigint not null,
        update_date timestamp not null,
        vote_text varchar(255),
        win_threshold bigint not null,
        primary key (id)
    );

    create table bot_owners (
       user_id bigint not null,
        create_date timestamp not null,
        primary key (user_id)
    );

    create table common_preferences (
       key varchar(60) not null,
        create_date timestamp not null,
        long_value bigint not null,
        string_value varchar(64) not null,
        update_date timestamp not null,
        primary key (key)
    );

    create table server_preferences (
       id bigint not null,
        bool_value bigint not null,
        create_date timestamp not null,
        key varchar(60) not null,
        long_value bigint not null,
        server_id bigint not null,
        string_value varchar(64) not null,
        update_date timestamp not null,
        primary key (id)
    );

    create table vote_points (
       id bigint not null,
        create_date timestamp not null,
        custom_emoji_id bigint,
        point_text varchar(2000) not null,
        unicode_emoji varchar(12),
        update_date timestamp not null,
        vote_id bigint not null,
        primary key (id)
    );

    create table vote_roles (
       id bigint not null,
        create_date timestamp not null,
        message_id bigint not null,
        role_id bigint not null,
        update_date timestamp not null,
        vote_id bigint not null,
        primary key (id)
    );

    alter table server_preferences 
       add constraint uniq_serv_key unique (server_id, key);
create index vote_roles_msg on vote_roles (message_id);

    alter table vote_roles 
       add constraint uniq_vote_role unique (vote_id, role_id);

    alter table vote_points 
       add constraint FK2uo5e5aqffyy5lurfurec37bn 
       foreign key (vote_id) 
       references active_votes;

    alter table vote_roles 
       add constraint FKoxl13bgrkqmubom6b53lldwo2 
       foreign key (vote_id) 
       references active_votes;
create sequence active_vote_ids start with 1 increment by 50;
create sequence server_preference_ids start with 1 increment by 50;
create sequence vote_point_ids start with 1 increment by 50;
create sequence vote_role_ids start with 1 increment by 50;

    create table active_votes (
       id bigint not null,
        create_date timestamp not null,
        is_exceptional bigint not null,
        finish_date timestamp,
        has_default bigint not null,
        has_timer bigint not null,
        message_id bigint not null,
        server_id bigint not null,
        text_chat_id bigint not null,
        update_date timestamp not null,
        vote_text varchar(255),
        win_threshold bigint not null,
        primary key (id)
    );

    create table bot_owners (
       user_id bigint not null,
        create_date timestamp not null,
        primary key (user_id)
    );

    create table common_preferences (
       key varchar(60) not null,
        create_date timestamp not null,
        long_value bigint not null,
        string_value varchar(64) not null,
        update_date timestamp not null,
        primary key (key)
    );

    create table server_preferences (
       id bigint not null,
        bool_value bigint not null,
        create_date timestamp not null,
        key varchar(60) not null,
        long_value bigint not null,
        server_id bigint not null,
        string_value varchar(64) not null,
        update_date timestamp not null,
        primary key (id)
    );

    create table vote_points (
       id bigint not null,
        create_date timestamp not null,
        custom_emoji_id bigint,
        point_text varchar(2000) not null,
        unicode_emoji varchar(12),
        update_date timestamp not null,
        vote_id bigint not null,
        primary key (id)
    );

    create table vote_roles (
       id bigint not null,
        create_date timestamp not null,
        message_id bigint not null,
        role_id bigint not null,
        update_date timestamp not null,
        vote_id bigint not null,
        primary key (id)
    );

    alter table server_preferences 
       add constraint uniq_serv_key unique (server_id, key);
create index vote_roles_msg on vote_roles (message_id);

    alter table vote_roles 
       add constraint uniq_vote_role unique (vote_id, role_id);

    alter table vote_points 
       add constraint FK2uo5e5aqffyy5lurfurec37bn 
       foreign key (vote_id) 
       references active_votes;

    alter table vote_roles 
       add constraint FKoxl13bgrkqmubom6b53lldwo2 
       foreign key (vote_id) 
       references active_votes;
