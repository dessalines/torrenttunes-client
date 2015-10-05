package com.torrenttunes.client;

import static com.torrenttunes.client.db.Tables.LIBRARY;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frostwire.jlibtorrent.LibTorrent;
import com.frostwire.jlibtorrent.StatsMetric;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.musicbrainz.mp3.tagger.Tools.Song;
import com.torrenttunes.client.ScanDirectory.ScanInfo;
import com.torrenttunes.client.db.Tables.Library;
import com.torrenttunes.client.tools.WriteMultilingualHTMLFiles;
import com.torrenttunes.client.tools.DataSources;
import com.torrenttunes.client.tools.Strings;
import com.torrenttunes.client.tools.Tools;

public class DerpTest extends TestCase {

	static final Logger log = LoggerFactory.getLogger(DerpTest.class);

	public void derp() throws JsonGenerationException, JsonMappingException, IOException {
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

	public void derp2() throws InterruptedException {
		LibtorrentEngine lte = LibtorrentEngine.INSTANCE;

		Tools.dbInit();
		Library track = LIBRARY.findById(1);



		TorrentHandle torrent = lte.addTorrent(
				new File(DataSources.DEFAULT_MUSIC_STORAGE_PATH()), new File(track.getString("torrent_path")),
				false);


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

	public void derp3() {
		log.info("Checking for update...");
		String htmlStr = Tools.httpGetString(DataSources.FETCH_LATEST_RELEASE_URL());
		log.info(DataSources.FETCH_LATEST_RELEASE_URL());
		log.info(htmlStr);

		String tagName = htmlStr.split("/tchoulihan/torrenttunes-client/releases/tag/")[1].split("\"")[0];
		log.info("Latest Tag #: " + tagName);

		if (!DataSources.VERSION.equals(tagName)) {
			//			downloadAndInstallJar(tagName);

		} else {
			log.info("No updates found");

		}
	}

	public void derp4() {

		ScanInfo si = ScanInfo.create(new File(DataSources.SAMPLE_SONG));

		Song song = Song.fetchSong(si.getFile());
		si.setMbid(song.getRecordingMBID());

		ScanDirectory.createAndSaveTorrent(si, song);


	}

	public void derp5() throws UnknownHostException {
		scan(InetAddress.getByName(DataSources.EXTERNAL_IP));
	}

	public static void scan(final InetAddress remote) {


		int port=0;
		String hostname = remote.getHostName();

		for ( port = 3000; port < 65536; port++) {
			try {
				Socket s = new Socket(remote,port);
				System.out.println("Server is listening on port " + port+ " of " + hostname);
				s.close();
			}
			catch (IOException ex) {
				// The remote host is not listening on this port
//				System.out.println("Server is not listening on port " + port+ " of " + hostname);
			}
		}
	}
	
	public static void testDerp6() throws JsonGenerationException, JsonMappingException, IOException {
//		Song song = Song.fetchSong(new File(DataSources.SAMPLE_SONG));
//		
//		String songJson = Tools.MAPPER.writeValueAsString(song);
//		
//		// Add the mac_address
//		ObjectNode on = Tools.MAPPER.valueToTree(Tools.jsonToNode(songJson));
//		on.put("uploader_ip_hash", DataSources.IP_HASH);
//		
//		String songUploadJson = Tools.nodeToJson(on);
//		log.info("song upload json:\n" + songUploadJson);
		
//		System.out.println(Tools.GSON2.toJson(Strings.EN.map));
		
//		Map<String, String> map = Strings.EN.map;
//		
//		for (Entry<String, String> e : map.entrySet()) {
//			System.out.println(e.getKey() + " : " + e.getValue());
//		}
		
//		WriteMultilingualHTMLFiles.write();
		
		
		
	}

}

