package com.torrenttunes.client.tools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.jlibtorrent.swig.create_torrent;
import com.torrenttunes.client.LibtorrentEngine;

import static com.torrenttunes.client.db.Tables.*;

// java -cp target/torrenttunes-client.jar com.torrenttunes.client.tools.ChangeTorrentTracker
public class ChangeTorrentTracker {

	static final Logger log = LoggerFactory.getLogger(ChangeTorrentTracker.class);


	public static void saveTorrents() {
		try {
			LibtorrentEngine lte = LibtorrentEngine.INSTANCE;
			Tools.dbInit();
			List<Library> torrentFiles = LIBRARY.findAll();

			for (Library l : torrentFiles) {
				String torrentPath = l.getString("torrent_path");
				log.info("Editing torrent_path: " + torrentPath);
				File torrentFile = new File(torrentPath);
				try {
					updateTrackerForTorrent(torrentFile);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}

		} catch(Exception e) {
			e.printStackTrace();			
		} finally {
			Tools.dbClose();
		}


	}

	public static void updateTrackerForTorrent(File torrentFile) {

		TorrentInfo ti = new TorrentInfo(torrentFile);

		create_torrent t = new create_torrent(ti.getSwig());

		// Changing the tracker
		for (URI announce : DataSources.ANNOUNCE_LIST()) {
			t.add_tracker(announce.toASCIIString());
		}
		

		// Get the bencode and write the file
		Entry entry =  new Entry(t.generate());

		Map<String, Entry> entryMap = entry.dictionary();
		Entry entryFromUpdatedMap = Entry.fromMap(entryMap);
		final byte[] bencode = entryFromUpdatedMap.bencode();

		FileOutputStream fos;

		try {
			fos = new FileOutputStream(torrentFile);

			BufferedOutputStream bos = new BufferedOutputStream(fos);
			bos.write(bencode);
			bos.flush();
			bos.close();

			log.info("Trackers updated for torrent: " + torrentFile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	public static void main(String[] args) {
		saveTorrents();
	}

}
