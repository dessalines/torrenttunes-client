CREATE VIEW queue_view AS 
SELECT library_id as id, * 
from queue_track
left join library on queue_track.library_id = library.id
;

CREATE VIEW playlist_track_view AS 
SELECT * from library
inner join playlist_track
on library.id = playlist_track.library_id;