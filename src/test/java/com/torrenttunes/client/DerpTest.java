package com.torrenttunes.client;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.node.ObjectNode;

import com.esotericsoftware.minlog.Log;
import com.frostwire.jlibtorrent.TorrentAlertAdapter;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.alerts.StateChangedAlert;
import com.frostwire.jlibtorrent.alerts.StatsAlert;
import com.musicbrainz.mp3.tagger.Tools.Song;
import com.musicbrainz.mp3.tagger.Tools.Tagger.MusicBrainzQuery;
import com.torrenttunes.client.ScanDirectory.ScanInfo;

import static com.torrenttunes.client.db.Tables.*;
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
	
	public void testDerp2() throws InterruptedException {
		LibtorrentEngine lte = LibtorrentEngine.INSTANCE;
		
		Tools.dbInit();
		Library track = LIBRARY.findById(1);
		
		
		
		TorrentHandle torrent = lte.addTorrent(
				new File(DataSources.CACHE_DIR()), new File(track.getString("torrent_path")));
		
		
		ObjectNode on = Tools.MAPPER.valueToTree(Tools.jsonToNode(track.toJson(false)));
		
		System.out.println(torrent.getStatus().getConnectCandidates());
		System.out.println(torrent.getStatus().getListPeers());
		System.out.println(torrent.getStatus().getListSeeds());
		on.put("seeders", torrent.getStatus().getConnectCandidates());
		
		String json = Tools.nodeToJson(on);
		Tools.dbClose();
		
		System.out.println(json);
		
		Thread.sleep(10000);
	}
}
