package com.torrenttunes.client.webservice;

import static com.torrenttunes.client.db.Tables.LIBRARY;
import static com.torrenttunes.client.db.Tables.QUEUE_VIEW;
import static spark.Spark.get;
import static spark.Spark.post;

import java.io.File;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frostwire.jlibtorrent.TorrentAlertAdapter;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.torrenttunes.client.DataSources;
import com.torrenttunes.client.LibtorrentEngine;
import com.torrenttunes.client.ScanDirectory;
import com.torrenttunes.client.ScanDirectory.ScanInfo;
import com.torrenttunes.client.ScanDirectory.ScanStatus;
import com.torrenttunes.client.Tools;
import com.torrenttunes.client.TorrentStats;
import com.torrenttunes.client.db.Actions;
import com.torrenttunes.client.db.Tables.Library;





public class Platform {

	static final Logger log = LoggerFactory.getLogger(Platform.class);

	public static void setup() {
		
	
		get("/get_library", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);

				Tools.dbInit();
				String json = LIBRARY.findAll().toJson(false);


				return json;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} finally {
				Tools.dbClose();
			}




		});

		get("/get_play_queue", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);

				Tools.dbInit();
				String json = QUEUE_VIEW.findAll().toJson(false);


				return json;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} finally {
				Tools.dbClose();
			}




		});

		post("/save_play_queue", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);

				JsonNode on = Tools.jsonToNode(req.body());

				Tools.dbInit();
				Actions.clearAndSavePlayQueue(on);


				return "Play queue saved";

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} finally {
				Tools.dbClose();
			}



		});

		post("/upload_music_directory", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);
				Map<String, String> vars = Tools.createMapFromAjaxPost(req.body());

				String uploadPath = vars.get("upload_path");

				ScanDirectory.start(new File(uploadPath));



				return "Uploading complete";

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} 



		});

		get("/get_upload_info", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);

				String json = Tools.MAPPER.writeValueAsString(LibtorrentEngine.INSTANCE.getScanInfos());

				return json;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} 



		});

		get("/fetch_or_download_song/:infoHash", (req, res) -> {

			try {
				LibtorrentEngine lte = LibtorrentEngine.INSTANCE;

				Tools.allowAllHeaders(req, res);

				String json = null;
				String infoHash = req.params(":infoHash");

				// Fetch the song by its info hash, and return that row
				Tools.dbInit();
				Library track = LIBRARY.findFirst("info_hash = ?", infoHash);
				Tools.dbClose();

				if (track != null) {
					json = track.toJson(false);					

				}
				// If it doesn't exist, download the torrent to the cache dir
				else {


					//					byte[] bytes = Tools.httpGetBytes(DataSources.TORRENT_DOWNLOAD_URL(infoHash));
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
					String albumMbid = jsonNode.get("release_mbid").asText();
					String artist = jsonNode.get("artist").asText();
					String artistMbid = jsonNode.get("artist_mbid").asText();
					String year = jsonNode.get("year").asText();
					String coverArt = jsonNode.get("album_coverart_url").asText();
					String thumbnailLarge = jsonNode.get("album_coverart_thumbnail_large").asText();
					String thumbnailSmall = jsonNode.get("album_coverart_thumbnail_small").asText();



					// add the torrent file(saving to the cache dir), scan info, and start seeding it					
					TorrentHandle torrent = lte.addTorrent(
							new File(DataSources.CACHE_DIR()), new File(torrentPath));

					String audioFilePath = DataSources.CACHE_FILE(torrent.getName());

					// Set up the scanInfo
					ScanInfo si = ScanInfo.create(new File(audioFilePath));
					si.setStatus(ScanStatus.Seeding);
					si.setMbid(songMbid);
					lte.getScanInfos().add(si); // TODO not sure about this one





					// Need to add the # of peers, and block IO until download is done, or times out
					final CountDownLatch signal = new CountDownLatch(1);



					// If it takes more than 30 seconds to download a file, then set no peers,
					// and throw an error
					Timer timer = new Timer();
					timer.schedule(new TimerTask() {
						@Override
						public void run() {
							signal.countDown();
							String resp = Tools.httpSimplePost(DataSources.SEEDER_INFO_UPLOAD(infoHash, "0-0"));
							log.info("Seeder post response: " + resp);
						}

					}, 40000);

					lte.getSession().addListener(new TorrentAlertAdapter(torrent) {
						private Timer timer;

						@Override
						public void torrentFinished(TorrentFinishedAlert alert) {


							// Save the track to your DB
							Tools.dbInit();
							Library newTrack = Actions.saveSongToLibrary(songMbid, 
									torrentPath, 
									infoHash,
									audioFilePath, 
									artist, 
									artistMbid,
									album,
									albumMbid,
									songTitle, 
									coverArt,
									thumbnailLarge, 
									thumbnailSmall,
									duration,
									trackNumber,
									year);

							newTrack.saveIt();
							Tools.dbClose();

							TorrentStats ts = TorrentStats.create(torrent);
							log.info(ts.toString());



							// Once the torrent's finished, save the number of peers:
							String resp = Tools.httpGetString(DataSources.SEEDER_INFO_UPLOAD(
									infoHash, ts.getPeers()));
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

					// if it wasn't succesful(IE no peers found or > 40 seconds)
					if (track == null) {
						throw new NoSuchElementException("No peers found for " + 
								artist + " - " + songTitle);
					} else {
						json = track.toJson(false);
					}



				}



				return json;


			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} 




		});
		
		post("/power_off", (req, res) -> {
			try {
			

				Runtime.getRuntime().exit(0);
				return "A yellow brick road";

			} catch (Exception e) {
				res.status(666);
				return e.getMessage();
			}

		});
		
		get("/error_test", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);

				throw new NoSuchElementException("error testing");

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} 




		});

	}








}
