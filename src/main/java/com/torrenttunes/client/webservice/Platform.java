package com.torrenttunes.client.webservice;

import static com.torrenttunes.client.db.Tables.LIBRARY;
import static com.torrenttunes.client.db.Tables.QUEUE_VIEW;
import static spark.Spark.get;
import static spark.Spark.post;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.codehaus.jackson.JsonNode;
import org.javalite.activejdbc.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frostwire.jlibtorrent.TorrentAlertAdapter;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.torrenttunes.client.DataSources;
import com.torrenttunes.client.LibtorrentEngine;
import com.torrenttunes.client.ScanDirectory;
import com.torrenttunes.client.Tools;
import com.torrenttunes.client.TorrentStats;
import com.torrenttunes.client.ScanDirectory.ScanInfo;
import com.torrenttunes.client.ScanDirectory.ScanStatus;
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
				log.info(json);

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
				log.info(req.body());
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
				log.info(uploadPath);
				
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
					
					// Fetch the .torrent file
//					byte[] bytes = Tools.httpGetBytes(DataSources.TORRENT_DOWNLOAD_URL(infoHash));
					String torrentPath = DataSources.TORRENTS_DIR() + "/" + infoHash + ".torrent";
					Tools.httpGetBytesV2(DataSources.TORRENT_DOWNLOAD_URL(infoHash), torrentPath);
					
					// Fetch the .torrent file json info
					String trackJson = Tools.httpGetString(DataSources.TORRENT_INFO_DOWNLOAD_URL(infoHash));
					log.info("track json = " + trackJson);
					
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
					
					
					// convert it to a file, save it to the torrents dir
					
//					FileOutputStream fos = new FileOutputStream(torrentPath);
//					fos.write(bytes);
//					fos.close();
					
					
					// add the torrent file(saving to the cache dir), scan info, and start seeding it					
					TorrentHandle torrent = lte.addTorrent(
							new File(DataSources.CACHE_DIR()), new File(torrentPath));
					
					String audioFilePath = DataSources.CACHE_FILE(torrent.getName());
					
					// Set up the scanInfo
					ScanInfo si = ScanInfo.create(new File(audioFilePath));
					si.setStatus(ScanStatus.Seeding);
					si.setMbid(songMbid);
					lte.getScanInfos().add(si);
					
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
					
					json = newTrack.toJson(false);
					
					final CountDownLatch signal = new CountDownLatch(1);
					
					// Only return after the torrent has finished, so wait for it
					lte.getSession().addListener(new TorrentAlertAdapter(torrent) {
						@Override
						public void torrentFinished(TorrentFinishedAlert alert) {
							TorrentStats ts = TorrentStats.create(torrent);
							log.info(ts.toString());
							super.torrentFinished(alert);
							signal.countDown();
						}
						
					});
					
					signal.await();
					
					
					
					
				}
				
				

				
				

				return json;
				

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} finally {
				Tools.dbClose();
			}




		});

	}






}
