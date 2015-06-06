package com.torrenttunes.client.db;
import static com.torrenttunes.client.db.Tables.*;

import org.codehaus.jackson.JsonNode;

import com.torrenttunes.client.db.Tables.Library;
public class Actions {
	
	public static Library saveSongToLibrary(String mbid, String torrentPath, 
			String filePath, String artist, String artistMbid, String album, String albumMbid,
			String title, 
			String albumCoverArtUrl, String albumCoverArtThumbnailLarge,
			String albumCoverArtThumbnailSmall, Long durationMS, Integer trackNumber, String year) {
		
		Library library = LIBRARY.create("mbid", mbid,
				"torrent_path", torrentPath,
				"file_path", filePath,
				"artist", artist,
				"artist_mbid", artistMbid,
				"album", album,
				"album_mbid", albumMbid,
				"title", title,
				"duration_ms", durationMS,
				"track_number", trackNumber,
				"year", year,
				"album_coverart_url", albumCoverArtUrl,
				"album_coverart_thumbnail_large", albumCoverArtThumbnailLarge,
				"album_coverart_thumbnail_small", albumCoverArtThumbnailSmall);
		
		library.saveIt();
		
		return library;
		
	}
	
	public static void clearAndSavePlayQueue(JsonNode on) {
		QUEUE_TRACK.deleteAll();
		for (int i = 0; i < on.size(); i++) {
			JsonNode track = on.get(i);
			Integer libraryId = track.get("id").asInt();
			QUEUE_TRACK.createIt("library_id", libraryId);
		}
	}

}
