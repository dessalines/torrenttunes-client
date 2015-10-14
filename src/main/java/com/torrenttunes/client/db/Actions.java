package com.torrenttunes.client.db;
import static com.torrenttunes.client.db.Tables.LIBRARY;
import static com.torrenttunes.client.db.Tables.SETTINGS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringEscapeUtils;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frostwire.jlibtorrent.TorrentAlertAdapter;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.torrenttunes.client.LibtorrentEngine;
import com.torrenttunes.client.ScanDirectory.ScanInfo;
import com.torrenttunes.client.ScanDirectory.ScanStatus;
import com.torrenttunes.client.db.Tables.Library;
import com.torrenttunes.client.db.Tables.Settings;
import com.torrenttunes.client.extra.TorrentStats;
import com.torrenttunes.client.tools.DataSources;
import com.torrenttunes.client.tools.Tools;
public class Actions {

	static final Logger log = LoggerFactory.getLogger(Actions.class);

	// 15 minute download timeout
	public static final Long DOWNLOAD_TIMEOUT = TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES);


	public static Library saveSongToLibrary(String mbid, String torrentPath, String infoHash,
			String filePath, String artist, String artistMbid,
			String title, Long durationMS) {

		log.info("Saving song " + infoHash + " to library");

		Library library = LIBRARY.createIt("mbid", mbid,
				"torrent_path", torrentPath,
				"info_hash", infoHash,
				"file_path", filePath,
				"artist", StringEscapeUtils.escapeHtml4(artist),
				"artist_mbid", artistMbid,
				"title", StringEscapeUtils.escapeHtml4(title),
				"duration_ms", durationMS);


		return library;

	}

	public static String saveSettings(String storagePath, Integer maxDownloadSpeed,
			Integer maxUploadSpeed, Integer maxCacheSize) {

		Settings s = SETTINGS.findFirst("id = ?", 1);

		StringBuilder message = new StringBuilder();

		// If storage path has changed, you need to move all the torrents in that cache directory,
		// to wherever they want, and change their 

		String currentStoragePath = s.getString("storage_path");
		if (!storagePath.equals(currentStoragePath)) {
			message.append("Moved all music files from " + currentStoragePath  + " to " + storagePath);

			s.set("storage_path", storagePath);

			for (Library track : getCachedTracks()) {

				File trackFile = new File(track.getString("file_path"));

				// moves the file to the new dir
				trackFile.renameTo(new File(storagePath + "/" + trackFile.getName()));

				// Save its new directory
				track.set("file_path", storagePath).saveIt();
			}
		}

		s.set("max_download_speed", maxDownloadSpeed,
				"max_upload_speed", maxUploadSpeed,
				"max_cache_size_mb", maxCacheSize);
		s.saveIt();

		updateLibtorrentSettings(s);

		message.append("Settings Saved");

		return message.toString();


	}


	public static void updateLibtorrentSettings(Settings s) {

		log.info("Applying libtorrent Settings...");
		LibtorrentEngine lte = LibtorrentEngine.INSTANCE;
		Integer maxDownloadSpeed = s.getInteger("max_download_speed");
		Integer maxUploadSpeed = s.getInteger("max_upload_speed");

		maxDownloadSpeed = (maxDownloadSpeed != -1) ? maxDownloadSpeed : 0;
		maxUploadSpeed = (maxUploadSpeed != -1) ? maxUploadSpeed : 0;

		lte.getSettings().setUploadRateLimit(maxUploadSpeed*1000);
		lte.getSettings().setDownloadRateLimit(maxDownloadSpeed*1000);

		lte.getSession().applySettings(lte.getSettings());
	}

	public static void setupMusicStoragePath(Settings s) {
		String storagePath = s.getString("storage_path");
		DataSources.MUSIC_STORAGE_PATH = storagePath;
		log.info("Storage path = " + DataSources.MUSIC_STORAGE_PATH);
	}

	public static String downloadTorrent(String infoHash) throws IOException, InterruptedException {

		log.info("Downloading infohash: " + infoHash);

		String json = null;
		LibtorrentEngine lte = LibtorrentEngine.INSTANCE;

		Library track;
		String torrentPath = DataSources.TORRENTS_DIR() + "/" + infoHash + ".torrent";

		// Fetch the .torrent file it to a file, save it to the torrents dir
		Tools.httpSaveFile(DataSources.TORRENT_DOWNLOAD_URL(infoHash), torrentPath);

		// Fetch the .torrent file json info
		String trackJson = Tools.httpGetString(DataSources.TORRENT_INFO_DOWNLOAD_URL(infoHash));

		JsonNode jsonNode = Tools.jsonToNode(trackJson);

		// Set up all the necessary vars from the jsonInfo
		String songMbid = jsonNode.get("song_mbid").asText();
		String songTitle = jsonNode.get("title").asText();
		Long duration = jsonNode.get("duration_ms").asLong();
		Integer trackNumber = jsonNode.get("track_number").asInt();
		String album = jsonNode.get("album").asText();
		String albumMbid = jsonNode.get("release_group_mbid").asText();
		String artist = jsonNode.get("artist").asText();
		String artistMbid = jsonNode.get("artist_mbid").asText();
		String year = jsonNode.get("year").asText();



		// add the torrent file(saving to the storage dir), scan info, and start seeding it					
		TorrentHandle torrent = lte.addTorrent(
				new File(DataSources.MUSIC_STORAGE_PATH), new File(torrentPath), false);

		String audioFilePath = DataSources.AUDIO_FILE(torrent.getName());

		// Set up the scanInfo
		ScanInfo si = ScanInfo.create(new File(audioFilePath));
		si.setStatus(ScanStatus.Seeding);
		si.setMbid(songMbid);
		lte.getScanInfos().add(si); 


		// Need to add the # of peers, and block IO until download is done, or times out
		final CountDownLatch signal = new CountDownLatch(1);


		// If it takes more than 30 seconds to download a file, then set no peers,
		// and throw an error
		Timer timer = new Timer();

		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				signal.countDown();
			}

		}, DOWNLOAD_TIMEOUT);

		lte.getSession().addListener(new TorrentAlertAdapter(torrent) {
			private Timer timer;

			@Override
			public void torrentFinished(TorrentFinishedAlert alert) {

				log.info("torrent finished: " + infoHash);

				// Save the track to your DB
				try {
					Tools.dbInit();
					Actions.saveSongToLibrary(songMbid, 
							torrentPath, 
							infoHash,
							audioFilePath, 
							artist, 
							artistMbid,
							songTitle, 
							duration);

				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					Tools.dbClose();
				}

				TorrentStats ts = TorrentStats.create(torrent);
				log.info(ts.toString());



				// Once the torrent's finished, save the number of peers:
				String resp = Tools.httpGetString(DataSources.SEEDER_INFO_UPLOAD(
						infoHash, ts.getPeers()));
				log.info("Seeder count :" + ts.getPeers());
				log.info("Seeder post response: " + resp);
				signal.countDown();
				timer.cancel();

			}


			private TorrentAlertAdapter init(Timer t) {
				timer = t;
				return this;
			}



		}.init(timer));



		signal.await();

		// Get the json for the saved track
		Tools.dbInit();
		track = LIBRARY.findFirst("info_hash = ?", infoHash);
		Tools.dbClose();

		// if it wasn't successful(IE no peers found or > 40 seconds)
		if (track == null) {
			throw new NoSuchElementException("No peers found for " + 
					artist + " - " + songTitle + ", or download took too long. Check to make sure your firewall"
					+ " is turned off.");
		} else {
			json = track.toJson(false);
		}
		return json;
	}



	public static Boolean spaceFreeInStoragePath() {

		// Check to make sure you have space in the cache
		Tools.dbInit();
		Settings settings = SETTINGS.findFirst("id = ?", 1);
		Tools.dbClose();

		Integer settingsFreeSpaceMB = settings.getInteger("max_cache_size_mb");
		settingsFreeSpaceMB = (settingsFreeSpaceMB != -1) ? settingsFreeSpaceMB : Integer.MAX_VALUE;

		Long temp = Math.round(Tools.folderSize(new File(DataSources.MUSIC_STORAGE_PATH)) * 0.000001);
		Integer storageFolderSizeMB = temp.intValue();

		Boolean spaceFree = (storageFolderSizeMB < settingsFreeSpaceMB) && 
				(new File("/").getUsableSpace() > 0);

		return spaceFree;
	}

	public static String deleteSong(String infoHash) {

		String message = null;

		try {
			// Stop the torrent, remove it from the session
			TorrentHandle torrent = LibtorrentEngine.INSTANCE.getInfoHashToTorrentMap().get(infoHash);
			LibtorrentEngine.INSTANCE.getSession().removeTorrent(torrent);

			// Remove it from the DB
			Library song = LIBRARY.findFirst("info_hash = ?", infoHash);
			String torrentPath = song.getString("torrent_path");
			String filePath = song.getString("file_path");
			song.delete();

			// delete the .torrent file and the file
			new File(torrentPath).delete();
			new File(filePath).delete();

			message = filePath + " has been deleted";
		} catch(NullPointerException e) {
			e.printStackTrace();
			throw new NoSuchElementException("Song has already been deleted");
		}

		return message;
	}



	public static String removeArtist(String artistMBID) {

		List<Library> songs = LIBRARY.find("artist_mbid = ?", artistMBID);
		String cacheDir = SETTINGS.findFirst("id = ?", 1).getString("storage_path");

		for (Library song : songs) {
			removeSong(song, cacheDir);
		}

		return "Artist : " + artistMBID + " deleted from library";
	}

	public static String removeSong(String songMBID) {

		Library song = LIBRARY.findFirst("mbid = ?", songMBID);
		if (song != null) {
			removeSong(song);

			return "Song : " + songMBID + " deleted from library";
		} else {
			return "Song mbid = " + songMBID + " wasn't found";
		}
	}

	public static String removeSong(Library song) {
		String cacheDir = SETTINGS.findFirst("id = ?", 1).getString("storage_path");
		return removeSong(song, cacheDir);
	}

	public static String removeSong(Library song, String cacheDir) {

		LibtorrentEngine lt = LibtorrentEngine.INSTANCE;

		String songMBID = song.getString("mbid");

		// remove the infohash from the session
		String infoHash = song.getString("info_hash");
		try {
			lt.getSession().removeTorrent(lt.getInfoHashToTorrentMap().get(infoHash));
		} catch(NullPointerException e) {
			log.error("Torrent infohash " + infoHash + " was not in Libtorrent Session");
		}

		// delete the torrent file
		new File(song.getString("torrent_path")).delete();

		// delete the save resume data file
		String srDataPath = DataSources.TORRENTS_DIR() + "/srdata_" + 
				new File(song.getString("file_path")).getName();
		new File(srDataPath).delete();

		// delete the song(only if its a cached one)
		File songFile = new File(song.getString("file_path"));
		String parent = songFile.getParentFile().getAbsolutePath();

		if (parent.equals(cacheDir)) {
			log.info("file: " + songFile.getAbsolutePath() + " deleted");
			songFile.delete();
		}

		// delete the row
		song.delete();

		return "Track : " + songMBID + " deleted from library.";
	}


	public static String clearCache() {

		List<Library> cachedTracks = getCachedTracks();
		String cacheDir = SETTINGS.findFirst("id = ?", 1).getString("storage_path");

		for (Library song : cachedTracks) {
			log.info("Song removed from cache: " + song.getString("file_path"));
			removeSong(song, cacheDir);
		}

		String msg = "Songs removed from cache: " + cachedTracks.size();
		log.info(msg);

		return msg;




	}

	public static String clearDatabase() {
		LIBRARY.deleteAll();

		return "Database cleared";
	}

	public static List<Library> getCachedTracks() {

		String cacheDir = SETTINGS.findFirst("id = ?", 1).getString("storage_path");

		log.info("cache dir = " + cacheDir);

		// remove the tracks that aren't in the cache directory from the list
		List<Library> tracks = LIBRARY.findAll();

		List<Library> cachedTracks = new ArrayList<>();

		for (int i = 0; i < tracks.size(); i++) {
			Library song = tracks.get(i);
			File filePath = new File(song.getString("file_path"));
			String parent = filePath.getParentFile().getAbsolutePath();


			// IE, if this is in the cache dir, this gets added
			if (parent.equals(cacheDir)) {
				cachedTracks.add(song);
			}
		}

		return cachedTracks;
	}



}
