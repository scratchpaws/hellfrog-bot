update emoji_total_statistics
set usages_count = usages_count - 1,
    last_usage   = ?,
    update_date  = ?
where server_id = ?
  and emoji_id = ?
  and usages_count > 0