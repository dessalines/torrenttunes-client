package com.torrenttunes.client.webservice;

import static com.torrenttunes.client.db.Tables.LIBRARY;
import static com.torrenttunes.client.db.Tables.PLAYLIST;
import static com.torrenttunes.client.db.Tables.PLAYLIST_TRACK_VIEW;
import static com.torrenttunes.client.db.Tables.QUEUE_VIEW;
import static com.torrenttunes.client.db.Tables.SETTINGS;
import static spark.Spark.get;
import static spark.Spark.post;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.torrenttunes.client.LibtorrentEngine;
import com.torrenttunes.client.db.Actions;
import com.torrenttunes.client.db.Tables.Library;
import com.torrenttunes.client.tools.DataSources;
import com.torrenttunes.client.tools.ScanDirectory;
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

		get("/get_playlists", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);

				Tools.dbInit();
				String json = PLAYLIST.findAll().toJson(false);


				return json;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} finally {
				Tools.dbClose();
			}




		});

		get("/get_playlist/:playlistId", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);

				String playlistId = req.params(":playlistId");
				Tools.dbInit();
				String json = PLAYLIST_TRACK_VIEW.find("playlist_id = ?", playlistId).toJson(false);


				return json;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} finally {
				Tools.dbClose();
			}




		});

		post("/create_playlist", (req, res) -> {
			try {
				Tools.allowAllHeaders(req, res);
				Tools.logRequestInfo(req);

				Map<String, String> vars = Tools.createMapFromAjaxPost(req.body());

				String name = vars.get("name");

				Tools.dbInit();
				String playlistId = Actions.createPlaylist(name);

				return playlistId;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} finally {
				Tools.dbClose();
			}

		});

		post("/add_to_playlist/:playlistId/:infoHash", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);

				Tools.dbInit();

				String playlistId = req.params(":playlistId");
				String infoHash = req.params(":infoHash");

				String message = Actions.addToPlaylist(playlistId, infoHash);


				return message;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} finally {
				Tools.dbClose();
			}



		});

		post("/remove_from_playlist/:playlistId/:infoHash", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);

				Tools.dbInit();

				String playlistId = req.params(":playlistId");
				String infoHash = req.params(":infoHash");

				String message = Actions.removeFromPlaylist(playlistId, infoHash);


				return message;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} finally {
				Tools.dbClose();
			}



		});

		post("/delete_playlist/:playlistId", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);

				Tools.dbInit();

				String playlistId = req.params(":playlistId");

				String message = Actions.deletePlaylist(playlistId);


				return message;

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
						throw new NoSuchElementException("Not enough storage space, "
								+ "change your cache size in settings,"
								+ " or delete some songs from your library");
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


				//				Runtime.getRuntime().exit(0);
				log.info("Powering off...");
				System.exit(0);
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

		get("/get_audio_file/:encodedPath", (req, res) -> {
			res.type("audio/mpeg");
			//			res.header("Content-Disposition", "filename=\"music.mp3\"");
			
			HttpServletResponse raw = res.raw();
			
			try {
				Tools.allowAllHeaders(req, res);

				String path = URLDecoder.decode(req.params(":encodedPath"), "UTF-8");
				res.header("Content-Disposition", "attachment; filename=" + path);
				
				File mp3 = new File(path);


			
				//				raw.getOutputStream().write(Files.readAllBytes(Paths.get(path)));
				//				raw.getOutputStream().flush();
				//				raw.getOutputStream().close();


				//				BufferedOutputStream bos = new BufferedOutputStream(raw.getOutputStream(), 1024);
				//				byte[] fileBytes = Files.readAllBytes(Paths.get(path));
				//				bos.write(fileBytes);
				//				bos.flush();
				//				bos.close();
				
				
				// This one works, but doesn't stream
				ServletOutputStream stream = raw.getOutputStream();
				res.header("Content-Length", String.valueOf(mp3.length())); 
				
			

				FileInputStream input = new FileInputStream(mp3);
				BufferedInputStream buf = new BufferedInputStream(input);
				int readBytes = 0;
				
				//read from the file; write to the ServletOutputStream
				while ((readBytes = buf.read()) != -1) {
					stream.write(readBytes);
				}


				stream.close();
				buf.close();
				
				String range = req.headers("Range");
				
				String[] ranges = range.split("=")[1].split("-");
				
				Integer chunkSize = 1000000;
				Integer from = Integer.parseInt(ranges[0]);
				Integer to = chunkSize + from;
		        if (to >= mp3.length()) {
		            to = (int) (mp3.length() - 1);
		        }
		        if (ranges.length == 2) {
		            to = Integer.parseInt(ranges[1]);
		        }
			
				
				String responseRange = "bytes " + from + "-" + to + "/" + mp3.length();
				res.header("Accept-Ranges",  "bytes");
				res.header("Content-Range", responseRange);

//				return buildStream(mp3, range);

				log.info("headers: " + req.headers());
				Iterator<String> it = req.headers().iterator();
				while (it.hasNext()) {
					String h = it.next();
					log.info("Header: " + h + " = " + req.headers(h));
				}


				return res.raw();

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








