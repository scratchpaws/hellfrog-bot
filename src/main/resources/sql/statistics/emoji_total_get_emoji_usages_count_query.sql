select usages_count
from emoji_total_statistics
where server_id = ?
  and emoji_id = ?