select
    *
from
    active_votes
    left join
        vote_points
        on vote_points.vote_id = active_votes.vote_id
where
    active_votes.server_id = ?
	and active_votes.finish_date <= ?
	and active_votes.has_timer > 0