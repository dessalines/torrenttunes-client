package com.torrenttunes.client.webservice;

import static com.torrenttunes.client.db.Tables.LIBRARY;
import static com.torrenttunes.client.db.Tables.QUEUE_VIEW;
import static spark.Spark.get;
import static spark.Spark.post;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.torrenttunes.client.ScanDirectory;
import com.torrenttunes.client.ScanDirectory.ScanInfo;
import com.torrenttunes.client.Tools;
import com.torrenttunes.client.TorrentClient;
import com.torrenttunes.client.db.Actions;
public class Platform {

	static final Logger log = LoggerFactory.getLogger(Platform.class);

	public static void setup(TorrentClient torrentClient) {

		get("/get_library", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);

				Tools.dbInit();
				String json = LIBRARY.findAll().toJson(false);
				Tools.dbClose();

				return json;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} 



		});

		get("/get_play_queue", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);

				Tools.dbInit();
				String json = QUEUE_VIEW.findAll().toJson(false);
				log.info(json);
				Tools.dbClose();

				return json;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} 



		});

		post("/save_play_queue", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);
				log.info(req.body());
				JsonNode on = Tools.jsonToNode(req.body());
				
				Tools.dbInit();
				Actions.clearAndSavePlayQueue(on);
				Tools.dbClose();

				return "Play queue saved";

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} 



		});
		
		post("/upload_music_directory", (req, res) -> {

			try {
				Tools.allowAllHeaders(req, res);
				Map<String, String> vars = Tools.createMapFromAjaxPost(req.body());
				
				String uploadPath = vars.get("upload_path");
				log.info(uploadPath);
				
				ScanDirectory.start(new File(uploadPath), torrentClient);
				
				

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

				String json = Tools.MAPPER.writeValueAsString(torrentClient.getScanInfos());

				return json;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} 



		});

	}






}
