package com.torrenttunes.client;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicbrainz.mp3.tagger.Tools.Song;
import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.common.Torrent;

public class ScanDirectory {

	static final Logger log = LoggerFactory.getLogger(ScanDirectory.class);

	public File dir;



	public static ScanDirectory start(File dir) {
		return new ScanDirectory(dir);
	}

	private ScanDirectory(File dir) {
		this.dir = dir;

		scan();
	}

	private void scan() {

		// List all the music files in the sub or sub directories
		String[] types = {"mp3"};

		Collection<File> files = FileUtils.listFiles(dir, types , true);


		System.out.println(files);


		// Use ScanInfo to keep track of operations and messages while you're doing them
		Set<ScanInfo> scanInfos = new LinkedHashSet<ScanInfo>();

		for (File file : files) {
			scanInfos.add(ScanInfo.create(file));
		}

		// The main scanning loop
		for (ScanInfo si : scanInfos) {

			// Tag the MBID
			try {
				si.setStatus(ScanStatus.Scanning);
				Song song = Song.fetchSong(si.getFile());

				// Fetch the song MBID
				si.setMbid(song.getRecordingMBID());
				si.setStatus(ScanStatus.MusicBrainzFound);

				// Create a torrent for the file, put it in the /.app/torrents dir
				si.setStatus(ScanStatus.CreatingTorrent);
				String torrentFileName = Tools.constructTrackTorrentFilename(
						si.getFile(), song.getRecordingMBID());
				File torrentFile = new File(DataSources.TORRENTS_DIR() + "/" + torrentFileName + ".torrent");
				

				List<List<URI>> announceList = Arrays.asList(DataSources.ANNOUNCE_LIST());
		
				Torrent torrent = Torrent.create(si.getFile().getParentFile(), 
						Arrays.asList(si.getFile()), 
						announceList, 
						System.getProperty("user.name"));
				
				OutputStream os = new FileOutputStream(torrentFile);
				torrent.save(os);
				
				// Upload the torrent to the tracker
				si.setStatus(ScanStatus.UploadingTorrent);
				
				// Start seeding it
				si.setStatus(ScanStatus.Completed);
				Client client = new Client(InetAddress.getLocalHost(),
						SharedTorrent.fromFile(torrentFile, si.getFile().getParentFile()));
				// TODO eventually do this from a settings table
				client.setMaxDownloadRate(50.0);
				client.setMaxUploadRate(50.0);
				client.share();
				
				os.close();
				



				// Upload it to the main tracker server



			} 

			// Couldn't find the song
			catch (NoSuchElementException | InterruptedException | IOException e) {
				e.printStackTrace();
				si.setStatus(ScanStatus.MusicBrainzError);
				continue;
			}





		}

		log.info("Done scanning");





	}



	/**
	 * An enum list of states and messages while scanning
	 * @author tyler
	 *
	 */
	public enum ScanStatus {
		Pending(" "), 
		Scanning("Scanning"), 
		MusicBrainzFound("Found MusicBrainz ID (MBID)"), 
		MusicBrainzError("Couldn't Find MusicBrainz ID(MBID)"),
		CreatingTorrent("Creating a torrent file"),
		UploadingTorrent("Uploading torrent file to server"),
		Completed("Completed, and seeding file");


		private String s;

		ScanStatus(String s) {
			this.s = s;
		}
		@Override 
		public String toString() { return s; }
	}

	public static class ScanInfo {
		private File file;

		private ScanStatus status;
		private String mbid;

		public static ScanInfo create(File file) {
			return new ScanInfo(file);
		}
		private ScanInfo(File file) {
			this.file = file;
			this.status = ScanStatus.Pending;
		}
		public File getFile() {
			return file;
		}
		public ScanStatus getStatus() {
			return status;
		}
		public void setStatus(ScanStatus status) {
			log.debug("Status for " + file.getName() + " : " + status.toString());
			this.status = status;
		}
		public String getMbid() {
			return mbid;
		}
		public void setMbid(String mbid) {
			this.mbid = mbid;
		}




	}



}
