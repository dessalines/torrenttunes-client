package com.torrenttunes.client;

import static com.torrenttunes.client.db.Tables.LIBRARY;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.swig.create_torrent;
import com.frostwire.jlibtorrent.swig.error_code;
import com.frostwire.jlibtorrent.swig.file_entry;
import com.frostwire.jlibtorrent.swig.file_storage;
import com.frostwire.jlibtorrent.swig.libtorrent;
import com.musicbrainz.mp3.tagger.Tools.Song;
import com.torrenttunes.client.db.Actions;
import com.torrenttunes.client.db.Tables.Library;
import com.torrenttunes.client.tools.DataSources;
import com.torrenttunes.client.tools.Tools;


public class ScanDirectory {

	static final Logger log = LoggerFactory.getLogger(ScanDirectory.class);

	private File dir;

	public static ScanDirectory start(File dir) {
		return new ScanDirectory(dir);
	}

	private ScanDirectory(File dir) {
		this.dir = dir;
		LibtorrentEngine.INSTANCE.getSession().pause();
		scan();
		LibtorrentEngine.INSTANCE.getSession().resume();
	}



	private void scan() {

		List<File> files = fetchUntaggedSongsFromDir(dir);

		log.info("New mp3 files: ");
		for (File file : files) {
			log.info(file.getAbsolutePath());
		};

		Set<ScanInfo> scanInfos = LibtorrentEngine.INSTANCE.getScanInfos();
		// Use ScanInfo to keep track of operations and messages while you're doing them


		// The main scanning loop
		for (File file : files) {

			// Create a scanInfo from it
			ScanInfo si = ScanInfo.create(file);
			scanInfos.add(si);




			// Fetch the song MBID
			si.setStatus(ScanStatus.Scanning);
			si.setStatus(ScanStatus.FetchingMusicBrainzId);



			log.info("Querying file: " + file.getAbsolutePath());
			Song song = null;
			try {
				song = Song.fetchSong(si.getFile());
				log.info("MusicBrainz query: " + song.getQuery());
				si.setMbid(song.getRecordingMBID());
			}
			// Couldn't find the song
			catch (NoSuchElementException | NullPointerException | NumberFormatException e) {
				log.error("Couldn't Find MusicBrainz ID for File: " + file.getAbsolutePath());

				si.setStatus(ScanStatus.MusicBrainzError);
				continue;
			}



			// Create a torrent for the file, put it in the /.app/torrents dir
			si.setStatus(ScanStatus.CreatingTorrent);
			File torrentFile = createAndSaveTorrent(si, song);

			// Upload the torrent to the tracker
			try {
				si.setStatus(ScanStatus.UploadingTorrent);
				Tools.uploadFileToTracker(torrentFile); 
			} catch(NoSuchElementException e) {
				e.printStackTrace();
				si.setStatus(ScanStatus.UploadingError);
				continue;
			}


			// Start seeding it
			si.setStatus(ScanStatus.Seeding);
			log.info(si.getFile().getParentFile().getAbsolutePath());
			TorrentHandle torrent = LibtorrentEngine.INSTANCE.addTorrent(si.getFile().getParentFile(), 
					torrentFile);
//			torrent.pause();


			// upload it to the server
			try {
				String songUploadJson = Tools.MAPPER.writeValueAsString(song);
				Tools.uploadTorrentInfoToTracker(songUploadJson);
			} catch(NoSuchElementException | IOException | NullPointerException e) {

				e.printStackTrace();
				si.setStatus(ScanStatus.UploadingError);
				continue;
			}
			

			// Save it to the DB
			Library track = null;

			try {
				Tools.dbInit();
				track = Actions.saveSongToLibrary(song.getRecordingMBID(), 
						torrentFile.getAbsolutePath(), 
						torrent.getInfoHash().toHex(),
						si.getFile().getAbsolutePath(), 
						song.getArtist(), 
						song.getArtistMBID(),
						song.getRecording(), 
						song.getDuration());
			} catch(Exception e) {
				e.printStackTrace();
				si.setStatus(ScanStatus.DBError);
				continue;
			} finally {
				Tools.dbClose();
			}





			// Set it as scanned
			si.setScanned(true);

		}



		log.info("Done scanning");

	}

	public static List<File> fetchUntaggedSongsFromDir(File dir) {
		// List all the music files in the sub or sub directories
		String[] types = {"mp3"};

		Collection<File> files = null;
		try {
			files = FileUtils.listFiles(dir, types , true);
		} catch(java.lang.IllegalArgumentException e) {
			throw new NoSuchElementException("Couldn't find directory: " + dir);
		}



		// Remove all that aren't already in the library(you don't need to upload or seed them)
		Set<File> dbFilePaths = loadFilePathsFromDB();
		files.removeAll(dbFilePaths);

		// sort the files
		List<File> sortedFiles = new ArrayList<File>(files);
		Collections.sort(sortedFiles);

		return sortedFiles;
	}

	public static Set<File> loadFilePathsFromDB() {
		Tools.dbInit();
		List<Library> library = LIBRARY.findAll();
		library.isEmpty();
		Set<File> libraryFiles = new HashSet<File>();
		for (Library track : library) {
			libraryFiles.add(new File(track.getString("file_path")));
		}
		Tools.dbClose();

		return libraryFiles;
	}

	
	
	

	public static File createAndSaveTorrent(ScanInfo si, Song song) {

		String torrentFileName = Tools.constructTrackTorrentFilename(
				si.getFile(), song);
		File torrentFile = new File(DataSources.TORRENTS_DIR() + "/" + torrentFileName + ".torrent");

		return Tools.createAndSaveTorrent(torrentFile, si.getFile());
		//
		//

		//		
		//		FileStorage ffs = new FileStorage(fs);
		////		ffs.addFile(si.getFileName(), si.getFile().length());
		//
		//		
		//		File fakeMulti = new File(si.getFile().getParent() + "/t");
		//		try {
		//			fakeMulti.createNewFile();
		//		} catch (IOException e1) {
		//			// TODO Auto-generated catch block
		//			e1.printStackTrace();
		//		}
		//		System.out.println(si.getFile().getParent());
		//		libtorrent.add_files(fs, si.getFile().getAbsolutePath());
		//		libtorrent.add_files(fs, fakeMulti.getAbsolutePath());
		//		
		////		fs.add_file(si.getFile().getAbsolutePath(), si.getFile().length());
		////		fs.add_file(fakeMulti.getAbsolutePath(), fakeMulti.length());
		//		
		////		ffs.setName(song.getArtist() + " - " + song.getRelease() + " - " + song.getRecording() 
		////				+ " - tt[" + torrentFileName + "]");
		//	
		////		libtorrent.add_files(fs, si.getFile().getAbsolutePath());
		////		fs.set_name(si.getFile().getParent());
		//		
		//	
		//		
		//		





		//				fs.add_file(si.getFile().getAbsolutePath(), si.getFile().length());
		//		fs.add_file(DataSources.SAMPLE_TORRENT.getAbsolutePath(), DataSources.SAMPLE_TORRENT.getAbsolutePath().length());
		//		libtorrent.add_files(fs, DataSources.SAMPLE_TORRENT.getAbsolutePath());


		//				ffs.setName(song.getArtist() + " - " + song.getRelease() + " - " + song.getRecording() 
		//						+ "- tt[" + torrentFileName + "]");


		

	}



	/**
	 * An enum list of states and messages while scanning
	 * @author tyler
	 *
	 */
	public enum ScanStatus {
		Pending(" "), 
		Scanning("Scanning"), 
		FetchingMusicBrainzId("Fetching MusicBrainz ID"), 
		MusicBrainzError("Couldn't Find MusicBrainz ID"),
		CreatingTorrent("Creating a torrent file"),
		AlreadyUploaded("Already uploaded"),
		UploadingTorrent("Uploading torrent file to server"),
		UploadingError("Couldn't upload the torrent file"),
		Seeding("Completed, and seeding file"),
		DBError("DB error, couldn't save");


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
		private Boolean scanned;


		public static ScanInfo create(File file) {
			return new ScanInfo(file);
		}
		private ScanInfo(File file) {
			this.file = file;
			this.status = ScanStatus.Pending;
			this.scanned = false;
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


		public void setScanned(Boolean scanned) {
			this.scanned = scanned;
		}
		public Boolean getScanned() {
			return scanned;
		}





	}


	public File getDir() {
		return dir;
	}








}
