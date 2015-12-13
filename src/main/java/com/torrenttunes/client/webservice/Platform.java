package com.torrenttunes.client.webservice;

import static com.torrenttunes.client.db.Tables.LIBRARY;
import static com.torrenttunes.client.db.Tables.SETTINGS;
import static spark.Spark.get;
import static spark.Spark.post;




import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;



import java.util.Set;

import javax.servlet.http.HttpServletResponse;




import org.codehaus.jackson.map.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.TorrentStatus;
import com.frostwire.jlibtorrent.swig.default_storage;
import com.torrenttunes.client.LibtorrentEngine;
import com.torrenttunes.client.ScanDirectory;
import com.torrenttunes.client.ScanDirectory.ScanInfo;
import com.torrenttunes.client.db.Actions;
import com.torrenttunes.client.db.Tables.Library;
import com.torrenttunes.client.tools.DataSources;
import com.torrenttunes.client.tools.Tools;





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
		
		// Example : 
		// curl --data "/home/derp/Music/A Music Dir" http://localhost:4568/share_directory
		post("/share_directory", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);
				
				log.info(req.body());

				String path = req.body().trim();

				Set<ScanInfo> scanInfos = ScanDirectory.start(new File(path));

				String scanInfoReport = ScanDirectory.scanInfosReport(scanInfos) + "\n";

				return scanInfoReport;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} 



		});
		
		

		get("/get_upload_info", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);

				List<ScanInfo> sis = LibtorrentEngine.INSTANCE.getScanInfosLastForty();
				
				String json = null;
				try {
					json = Tools.MAPPER.writeValueAsString(sis);
				} catch(JsonMappingException e1) {
					json = " ";
				}
				

				return json;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} 



		});
		


		get("/fetch_or_download_song/:infoHash", (req, res) -> {

			try {

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

					if (Actions.spaceFreeInStoragePath()) {
						json = Actions.downloadTorrent(infoHash);
					} else {
						// TODO maybe clear cache
						Tools.dbInit();
						Actions.clearCache();
						Tools.dbClose();
						
						throw new NoSuchElementException("Not enough storage space, "
								+ "your cache has now been cleared");
					}



				}



				return json;


			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} 




		});
		
		get("/get_torrent_progress/:infoHash", (req, res) -> {
			try {

				Tools.allowAllHeaders(req, res);
				
				String infoHash = req.params(":infoHash");
				
//				log.info("progress info hash: " + infoHash);
				
				TorrentHandle th = LibtorrentEngine.INSTANCE.getInfoHashToTorrentMap().get(infoHash);
				log.info("Progress torrent: " + th.getName());
				
				
				Double progress = th.getStatus().getProgressPpm() / 1E6;
//				float progress = th.getStatus().getProgress();
				
				return progress;
				

			} catch (Exception e) {
				res.status(666);
//				e.printStackTrace();
				return e.getMessage();
			} 
			
		});
		
		get("/get_torrent_status/:infoHash", (req, res) -> {
			try {

				Tools.allowAllHeaders(req, res);
				
				String infoHash = req.params(":infoHash");
				
				log.info("progress info hash: " + infoHash);
				TorrentHandle th = LibtorrentEngine.INSTANCE.getInfoHashToTorrentMap().get(infoHash);
//				th.forceRecheck();
//				th.saveResumeData();
				if (th == null) {
					return "its null derp";
				}
				TorrentStatus status = th.getStatus();
				
				Tools.printTorrentStatus(status);
				
				return status.toString();
				

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} 
			
		});
		
		post("/clear_cache", (req, res) -> {
			try {

				Tools.allowOnlyLocalHeaders(req, res);

				Tools.dbInit();
				
				String json = Actions.clearCache();
				
				return json;
				

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} finally {
				Tools.dbClose();
			}
			
		});
		
		post("/clear_database", (req, res) -> {
			try {

				Tools.allowOnlyLocalHeaders(req, res);

				Tools.dbInit();
				
				String json = Actions.clearDatabase();
				
				return json;
				

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} finally {
				Tools.dbClose();
			}
			
		});
		

		
		get("/remove_artist/:artistMBID", (req, res) -> {
			try {

				Tools.allowOnlyLocalHeaders(req, res);
				
				String artistMBID = req.params(":artistMBID");
				Tools.dbInit();
				
				String json = Actions.removeArtist(artistMBID);
				
				return json;
				

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} finally {
				Tools.dbClose();
			}
			
		});
		
		get("/remove_song/:songMBID", (req, res) -> {
			try {

				Tools.allowOnlyLocalHeaders(req, res);
				
				String songMBID = req.params(":songMBID");
				Tools.dbInit();
				
				String json = Actions.removeSong(songMBID);
				
				return json;
				

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} finally {
				Tools.dbClose();
			}
			
		});

		

		post("/power_off", (req, res) -> {
			try {

				//				Runtime.getRuntime().exit(0);
				log.info("Powering off...");
				
				default_storage.disk_write_access_log(false);
//				LibtorrentEngine.INSTANCE.getSession().pause(); // should save all the resumeData
				LibtorrentEngine.INSTANCE.getSession().abort();

				
				System.exit(0);
				return "A yellow brick road";

			} catch (Exception e) {
				res.status(666);
				return e.getMessage();
			}

		});
		
		post("/uninstall", (req, res) -> {
			try {
				
				Tools.allowOnlyLocalHeaders(req, res);

				log.info("Uninstalling torrenttunes");
				Tools.uninstall();
				return "TorrentTunes Uninstalled";

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

		get("/get_settings", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);

				Tools.dbInit();
				String json = SETTINGS.findFirst("id = ?", 1).toJson(false);


				return json;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} finally {
				Tools.dbClose();
			}




		});

		get("/get_sample_song", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);

				HttpServletResponse raw = res.raw();
				raw.getOutputStream().write(Files.readAllBytes(Paths.get(DataSources.SAMPLE_SONG)));
				raw.getOutputStream().flush();
				raw.getOutputStream().close();

				return res.raw();

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			}

		});
		
		get("/get_upload_download_totals", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);
				
				String json = LibtorrentEngine.INSTANCE.getUploadDownloadTotals();


				return json;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			}




		});

		

		post("/save_settings", (req, res) -> {
			try {
				Tools.allowAllHeaders(req, res);
				Tools.logRequestInfo(req);

				Map<String, String> vars = Tools.createMapFromAjaxPost(req.body());

				Integer maxUploadSpeed = Integer.valueOf(vars.get("max_upload_speed"));
				Integer maxDownloadSpeed = Integer.valueOf(vars.get("max_download_speed"));
				Integer maxCacheSize = Integer.valueOf(vars.get("max_cache_size_mb"));
				String storagePath = vars.get("storage_path");


				Tools.dbInit();


				String message = Actions.saveSettings(storagePath,
						maxDownloadSpeed, 
						maxUploadSpeed,
						maxCacheSize);



				return message;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} finally {
				Tools.dbClose();
			}

		});

		post("/delete_song/:infoHash", (req, res) -> {
			try {
				Tools.allowAllHeaders(req, res);
				Tools.logRequestInfo(req);

				String infoHash = req.params(":infoHash");
				Tools.dbInit();
				String message = Actions.deleteSong(infoHash);


				return message;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} finally {
				Tools.dbClose();
			}

		});

	}


//	public static class MediaStreamer implements StreamingOutput {
//		
//	    private int length;
//	    private RandomAccessFile raf;
//	    final byte[] buf = new byte[4096];
//
//	    public MediaStreamer(int length, RandomAccessFile raf) {
//	        this.length = length;
//	        this.raf = raf;
//	    }
//
//	    @Override
//	    public void write(OutputStream outputStream) throws IOException, WebApplicationException {
//	        try {
//	            while( length != 0) {
//	                int read = raf.read(buf, 0, buf.length > length ? length : buf.length);
//	                outputStream.write(buf, 0, read);
//	                length -= read;
//	            }
//	        } finally {
//	            raf.close();
//	        }
//	    }
//
//	    public int getLenth() {
//	        return length;
//	    }
//	}
//	
//	private static Response buildStream(final File asset, final String range) throws Exception {
//        // range not requested : Firefox, Opera, IE do not send range headers
//        if (range == null) {
//            StreamingOutput streamer = new StreamingOutput() {
//                @Override
//                public void write(final OutputStream output) throws IOException, WebApplicationException {
//
//                    final FileChannel inputChannel = new FileInputStream(asset).getChannel();
//                    final WritableByteChannel outputChannel = Channels.newChannel(output);
//                    try {
//                        inputChannel.transferTo(0, inputChannel.size(), outputChannel);
//                    } finally {
//                        // closing the channels
//                        inputChannel.close();
//                        outputChannel.close();
//                    }
//                }
//            };
//            return Response.ok(streamer).status(200).header(HttpHeaders.CONTENT_LENGTH, asset.length()).build();
//        }
//
//        String[] ranges = range.split("=")[1].split("-");
//        final int from = Integer.parseInt(ranges[0]);
//        /**
//         * Chunk media if the range upper bound is unspecified. Chrome sends "bytes=0-"
//         */
//        int chunk_size = 1024;
//        int to = chunk_size + from;
//        if (to >= asset.length()) {
//            to = (int) (asset.length() - 1);
//        }
//        if (ranges.length == 2) {
//            to = Integer.parseInt(ranges[1]);
//        }
//
//        final String responseRange = String.format("bytes %d-%d/%d", from, to, asset.length());
//        final RandomAccessFile raf = new RandomAccessFile(asset, "r");
//        raf.seek(from);
//
//        final int len = to - from + 1;
//        final MediaStreamer streamer = new MediaStreamer(len, raf);
//        
//        Response.ResponseBuilder res = Response.ok(streamer).status(206)
//                .header("Accept-Ranges", "bytes")
//                .header("Content-Range", responseRange)
//                .header(HttpHeaders.CONTENT_LENGTH, streamer.getLenth())
//                .header(HttpHeaders.LAST_MODIFIED, new Date(asset.lastModified()));
//        return res.build();
//    }
//	


}








