insert into active_votes
    (server_id,
    text_chat_id,
    message_id,
    finish_date,
    vote_text,
    has_timer,
    is_exceptional,
    has_default,
    win_threshold,
    create_date,
    update_date)
values
    (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)