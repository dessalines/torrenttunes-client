package com.torrenttunes.client;

import static com.torrenttunes.client.db.Tables.LIBRARY;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frostwire.jlibtorrent.TorrentHandle;
import com.musicbrainz.mp3.tagger.Tools.Song;
import com.torrenttunes.client.db.Actions;
import com.torrenttunes.client.db.Tables.Library;
import com.torrenttunes.client.tools.DataSources;
import com.torrenttunes.client.tools.Tools;


public class ScanDirectory {

	static final Logger log = LoggerFactory.getLogger(ScanDirectory.class);

	private File dir;





	public static Set<ScanInfo> start(File dir) {
		ScanDirectory sd = new ScanDirectory(dir);
		return sd.scan();
	}
	
	public ScanDirectory(File dir) {
		this.dir = dir;
	}



	private Set<ScanInfo> scan() {
		
		log.info("Scanning directory: " + dir.getAbsolutePath());
		
		Set<ScanInfo> newScanInfos = new LinkedHashSet<ScanInfo>();

		List<File> files = fetchUntaggedSongsFromDir(dir);

		log.info("New mp3 files: ");
		for (File file : files) {
			log.info(file.getAbsolutePath());
		};

		Set<ScanInfo> scanInfos = LibtorrentEngine.INSTANCE.getScanInfos();
		// Use ScanInfo to keep track of operations and messages while you're doing them


		// The main scanning loop
		for (File file : files) {

			// Create a scanInfo from it, check if its a new one added
			ScanInfo si = ScanInfo.create(file);
			
			// Add it to the new scan infos
			newScanInfos.add(si);
			boolean isNew = scanInfos.add(si);


			//			if (isNew) {

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
					torrentFile, true);
			//			torrent.pause();


			// upload it to the server
			try {
				String songJson = Tools.MAPPER.writeValueAsString(song);
				
				// Add the mac_address
				ObjectNode on = Tools.MAPPER.valueToTree(Tools.jsonToNode(songJson));
				on.put("uploader_ip_hash", DataSources.IP_HASH);
				
				String songUploadJson = Tools.nodeToJson(on);
				
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
		
		return newScanInfos;

	}
	
	public static String scanInfosReport(Set<ScanInfo> sis) {
		
		if (sis.size() == 0) {
			return "No .mp3 files were scanned";
		}
		
		StringBuilder sb = new StringBuilder();
		for (ScanInfo si : sis) {
			sb.append("ScanInfo: " + si.getMbid());
			sb.append(" | FileName: " + si.getFile().getAbsolutePath());
			sb.append(" | Status: " + si.getStatusString());
			sb.append(" | Scanned: " + si.getScanned());
			sb.append("\n");
		}
		
		return sb.toString();
		
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


		@Override
		public int hashCode() {
			return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
					// if deriving: appendSuper(super.hashCode()).
					append(file).
					append(mbid).
					toHashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ScanInfo))
				return false;
			if (obj == this)
				return true;

			ScanInfo rhs = (ScanInfo) obj;
			return new EqualsBuilder().
					// if deriving: appendSuper(super.equals(obj)).
					append(file, rhs.file).
					append(mbid, rhs.mbid).
					isEquals();
		}


	}


	public File getDir() {
		return dir;
	}








}
