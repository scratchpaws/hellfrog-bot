select author_id, description, image_url, create_date, update_date
from wtf_assigns
where server_id = ?
  and target_id = ?
order by update_date desc, id desc