package com.torrenttunes.client.webservice;

import static spark.Spark.get;
import static spark.Spark.port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.torrenttunes.client.tools.DataSources;
import com.torrenttunes.client.tools.Tools;

public class WebService {
static final Logger log = LoggerFactory.getLogger(WebService.class);

	
	public static void start() {

		port(DataSources.SPARK_WEB_PORT);	
		
		Platform.setup();		
	
		get("/hello", (req, res) -> {
			Tools.allowOnlyLocalHeaders(req, res);
			return "hi from the torrenttunes-client web service";
		});

		
		get("/*", (req, res) -> {
			Tools.allowAllHeaders(req, res);
//			Tools.set15MinuteCache(req, res);
			
			String pageName = req.splat()[0];
			
			String webHomePath = DataSources.WEB_HOME() + "/" + pageName;
			
			Tools.setContentTypeFromFileName(pageName, res);
			
			return Tools.writeFileToResponse(webHomePath, res);
			
		});

	
	}
}
