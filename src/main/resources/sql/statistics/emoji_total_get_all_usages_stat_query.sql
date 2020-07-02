select emoji_id, usages_count, last_usage
from emoji_total_statistics
where server_id = ?