package com.torrenttunes.client;

import static com.torrenttunes.client.db.Tables.LIBRARY;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frostwire.jlibtorrent.TorrentHandle;
import com.musicbrainz.mp3.tagger.Tools.Song;
import com.torrenttunes.client.db.Tables.Library;
import com.torrenttunes.client.tools.DataSources;
import com.torrenttunes.client.tools.Tools;

public class DerpTest extends TestCase {
	
	static final Logger log = LoggerFactory.getLogger(DerpTest.class);

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
				new File(DataSources.DEFAULT_MUSIC_STORAGE_PATH()), new File(track.getString("torrent_path")));
		
		
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
	
	public void testDerp3() {
		log.info("Checking for update...");
		String htmlStr = Tools.httpGetString(DataSources.FETCH_LATEST_RELEASE_URL());
		log.info(DataSources.FETCH_LATEST_RELEASE_URL());
		log.info(htmlStr);

		String tagName = htmlStr.split("/tchoulihan/torrenttunes-client/releases/tag/")[1].split("\"")[0];
		log.info("Latest Tag #: " + tagName);
			
		if (!DataSources.TAG_NAME.equals(tagName)) {
//			downloadAndInstallJar(tagName);

		} else {
			log.info("No updates found");

		}
	}
}
