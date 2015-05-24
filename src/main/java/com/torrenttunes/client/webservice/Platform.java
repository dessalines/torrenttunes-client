package com.torrenttunes.client.webservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.torrenttunes.client.Tools;

import static spark.Spark.*;
import static com.torrenttunes.client.db.Tables.*;
public class Platform {

	static final Logger log = LoggerFactory.getLogger(Platform.class);


	public static void setup() {

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
				String json = QUEUE_TRACK.findAll().toJson(false);
				log.info(json);
				Tools.dbClose();
				
				return json;

			} catch (Exception e) {
				res.status(666);
				e.printStackTrace();
				return e.getMessage();
			} 


			
		});

	}


	
}
