package com.torrenttunes.client.db;
import static com.torrenttunes.client.db.Tables.*;

import org.apache.commons.lang3.StringEscapeUtils;
import org.codehaus.jackson.JsonNode;

import com.torrenttunes.client.db.Tables.Library;
public class Actions {
	
	public static Library saveSongToLibrary(String mbid, String torrentPath, String infoHash,
			String filePath, String artist, String artistMbid, String album, String albumMbid,
			String title, 
			String albumCoverArtUrl, String albumCoverArtThumbnailLarge,
			String albumCoverArtThumbnailSmall, Long durationMS, Integer trackNumber, String year) {
		
		Library library = LIBRARY.create("mbid", mbid,
				"torrent_path", torrentPath,
				"info_hash", infoHash,
				"file_path", filePath,
				"artist", StringEscapeUtils.escapeHtml4(artist),
				"artist_mbid", artistMbid,
				"album", StringEscapeUtils.escapeHtml4(album),
				"album_mbid", albumMbid,
				"title", StringEscapeUtils.escapeHtml4(title),
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
