package com.torrenttunes.client;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;

import com.musicbrainz.mp3.tagger.Tools.Song;
import com.musicbrainz.mp3.tagger.Tools.Tagger.MusicBrainzQuery;
import com.torrenttunes.client.ScanDirectory.ScanInfo;

import junit.framework.TestCase;

public class DerpTest extends TestCase {

	public void testDerp() throws JsonGenerationException, JsonMappingException, IOException {
//		TorrentClient tc = TorrentClient.start();
//		ScanDirectory.start(new File(DataSources.SAMPLE_MUSIC_DIR), tc);

		// List all the music files in the sub or sub directories
		//		String[] types = {"mp3"};
		//
		//		Collection<File> files = FileUtils.listFiles(new File(DataSources.SAMPLE_MUSIC_DIR), types , true);
		//
		//
		//		Set<ScanInfo> scanInfos = new LinkedHashSet<ScanInfo>();
		//
		//		for (File file : files) {
		//			scanInfos.add(ScanInfo.create(file));
		//		}
		//		
		//		String json = Tools.MAPPER.writeValueAsString(scanInfos);
		//		System.out.println(json);
		
		
		Song song = Song.fetchSong(new File("/home/tyler/.torrenttunes-client/cache/1-06 Raconte-Moi Une Histoire.mp3"));
		System.out.println(song.getRecording());
		
	}
}
