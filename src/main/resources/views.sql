CREATE VIEW queue_view AS 
SELECT library_id as id, * 
from queue_track
left join library on queue_track.library_id = library.id
;