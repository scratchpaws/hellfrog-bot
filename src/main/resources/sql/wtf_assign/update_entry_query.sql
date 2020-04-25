update wtf_assigns
set description = ?,
    image_url   = ?,
    update_date = ?
where server_id = ?
  and target_id = ?
  and author_id = ?