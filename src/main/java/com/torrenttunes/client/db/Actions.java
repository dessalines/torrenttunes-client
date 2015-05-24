package com.torrenttunes.client.db;
import static com.torrenttunes.client.db.Tables.*;

import com.torrenttunes.client.db.Tables.Library;
public class Actions {
	
	public static void saveSongToLibrary(String mbid, String torrentPath, 
			String filePath, String artist, String album, String title, 
			String albumCoverArtUrl, String albumCoverArtThumbnailLarge,
			String albumCoverArtThumbnailSmall, Long durationMS) {
		
		Library library = LIBRARY.create("mbid", mbid,
				"torrent_path", torrentPath,
				"file_path", filePath,
				"artist", artist,
				"album", album,
				"title", title,
				"duration_ms", durationMS,
				"album_coverart_url", albumCoverArtUrl,
				"album_coverart_thumbnail_large", albumCoverArtThumbnailLarge,
				"album_coverart_thumbnail_small", albumCoverArtThumbnailSmall);
		
		library.saveIt();
		
	}

}
