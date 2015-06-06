package com.torrenttunes.client;

import static com.torrenttunes.client.db.Tables.LIBRARY;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.swig.create_torrent;
import com.frostwire.jlibtorrent.swig.error_code;
import com.frostwire.jlibtorrent.swig.file_storage;
import com.frostwire.jlibtorrent.swig.libtorrent;
import com.musicbrainz.mp3.tagger.Tools.CoverArt;
import com.musicbrainz.mp3.tagger.Tools.Song;
import com.torrenttunes.client.db.Actions;
import com.torrenttunes.client.db.Tables.Library;


public class ScanDirectory {

	static final Logger log = LoggerFactory.getLogger(ScanDirectory.class);

	private File dir;

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

		// Remove all that aren't already in the library(you don't need to upload or seed them)
		Set<File> torrentDBFiles = loadTorrentsFromDB();

		log.info("New torrent files: " + files);

		Set<ScanInfo> scanInfos = LibtorrentEngine.INSTANCE.getScanInfos();
		// Use ScanInfo to keep track of operations and messages while you're doing them
		for (File file : files) {
			scanInfos.add(ScanInfo.create(file));
		}

		// The main scanning loop
		for (ScanInfo si : scanInfos) {

			try {

				// Fetch the song MBID
				si.setStatus(ScanStatus.Scanning);
				si.setStatus(ScanStatus.FetchingMusicBrainzId);
				Song song = Song.fetchSong(si.getFile());
				si.setMbid(song.getRecordingMBID());

				// Fetch the cover art, not absolutely necessary
				String coverArtURL = null, coverArtLargeThumbnail = null, coverArtSmallThumbnail = null;
				try {
					CoverArt coverArt = CoverArt.fetchCoverArt(song);
					coverArtURL = coverArt.getImageURL();
					coverArtLargeThumbnail = coverArt.getLargeThumbnailURL();
					coverArtSmallThumbnail = coverArt.getSmallThumbnailURL();
				} catch(NoSuchElementException e) {}


				// Create a torrent for the file, put it in the /.app/torrents dir
				si.setStatus(ScanStatus.CreatingTorrent);
				File torrentFile = createAndSaveTorrent(si, song);

				// If that file already exists in the DB, you don't need to do anything to it
				if (torrentDBFiles.contains(torrentFile)) {
					log.info(torrentFile + " was already in the DB");
					si.setStatus(ScanStatus.AlreadyUploaded);
					continue;
				}

				// Upload the torrent to the tracker
				si.setStatus(ScanStatus.UploadingTorrent);
				Tools.uploadFileToTracker(torrentFile);


				// Start seeding it
				si.setStatus(ScanStatus.Seeding);
				log.info(si.getFile().getParentFile().getAbsolutePath());
				LibtorrentEngine.INSTANCE.addTorrent(si.getFile().getParentFile(), torrentFile);



				// Save it to the DB
				Tools.dbInit();				
				Library track = Actions.saveSongToLibrary(song.getRecordingMBID(), 
						torrentFile.getAbsolutePath(), 
						si.getFile().getAbsolutePath(), 
						song.getArtist(), 
						song.getArtistMBID(),
						song.getRelease(),
						song.getReleaseMBID(),
						song.getRecording(), 
						coverArtURL,
						coverArtLargeThumbnail, 
						coverArtSmallThumbnail,
						song.getDuration(),
						song.getTrackNumber(),
						song.getYear());
						
				Tools.dbClose();

				Tools.uploadTorrentInfoToTracker(track.toJson(false));

			} 

			// Couldn't find the song
			catch (NoSuchElementException e) {
				e.printStackTrace();
				si.setStatus(ScanStatus.MusicBrainzError);
				continue;
			}


		}

		log.info("Done scanning");

	}

	private static Set<File> loadTorrentsFromDB() {
		Tools.dbInit();
		List<Library> library = LIBRARY.findAll();
		library.isEmpty();
		Set<File> libraryFiles = new HashSet<File>();
		for (Library track : library) {
			libraryFiles.add(new File(track.getString("torrent_path")));
		}
		Tools.dbClose();

		return libraryFiles;
	}



	private File createAndSaveTorrent(ScanInfo si, Song song) {
		
		String torrentFileName = Tools.constructTrackTorrentFilename(
				si.getFile(), song.getRecordingMBID());
		File torrentFile = new File(DataSources.TORRENTS_DIR() + "/" + torrentFileName + ".torrent");



		file_storage fs = new file_storage();

		// Add the file
		libtorrent.add_files(fs, si.getFile().getAbsolutePath());
//		fs.add_file(si.getFile().getAbsolutePath(), si.getFile().length());
//		fs.add_file(DataSources.SAMPLE_TORRENT.getAbsolutePath(), DataSources.SAMPLE_TORRENT.getAbsolutePath().length());
//		libtorrent.add_files(fs, DataSources.SAMPLE_TORRENT.getAbsolutePath());
		
//		fs.set_name(song.getArtist() + " - " + song.getRelease() + " - " + song.getRecording() 
//				+ "- tt[" + torrentFileName + "]");
		
		
		

		create_torrent t = new create_torrent(fs);
		for (URI announce : DataSources.ANNOUNCE_LIST()) {
			t.add_tracker(announce.toASCIIString());
		}
		
		
		
		t.set_creator(System.getProperty("user.name"));

		error_code ec = new error_code();


		// reads the files and calculates the hashes
		libtorrent.set_piece_hashes(t, si.getFile().getParent(), ec);

		if (ec.value() != 0) {
			log.info(ec.message());
		}

		// Get the bencode and write the file
		Entry entry =  new Entry(t.generate());
		Map<String, Entry> entryMap = entry.dictionary();
		Entry entryFromUpdatedMap = Entry.fromMap(entryMap);
		final byte[] bencode = entryFromUpdatedMap.bencode();
		try {
			FileOutputStream fos;

			fos = new FileOutputStream(torrentFile);

			BufferedOutputStream bos = new BufferedOutputStream(fos);
			bos.write(bencode);
			bos.flush();
			bos.close();
		} catch (IOException e) {
			log.error("Couldn't write file");
			e.printStackTrace();
		}
		
		return torrentFile;

	}



	/**
	 * An enum list of states and messages while scanning
	 * @author tyler
	 *
	 */
	public enum ScanStatus {
		Pending(" "), 
		Scanning("Scanning"), 
		FetchingMusicBrainzId("Found MusicBrainz ID"), 
		MusicBrainzError("Couldn't Find MusicBrainz ID"),
		CreatingTorrent("Creating a torrent file"),
		AlreadyUploaded("Already uploaded"),
		UploadingTorrent("Uploading torrent file to server"),
		UploadingError("Couldn't upload the torrent file"),
		Seeding("Completed, and seeding file");


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

		public String getFileName() {
			return file.getName();
		}

		public ScanStatus getStatus() {
			return status;
		}

		public String getStatusString() {
			return status.toString();
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

		public String toJson() {
			String json = null;
			try {
				json = Tools.MAPPER.writeValueAsString(this);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return json;
		}




	}


	public File getDir() {
		return dir;
	}






}
