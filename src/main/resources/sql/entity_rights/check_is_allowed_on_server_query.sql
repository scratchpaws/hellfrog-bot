select count(1)
  from %table_name%
 where server_id = ?
   and %column_name% = ?
   and command_prefix = ?