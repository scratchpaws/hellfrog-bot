select
    *
from
    active_votes
    left join
        vote_points
        on vote_points.vote_id = active_votes.vote_id
where
    active_votes.vote_id = ?