package com.torrenttunes.client.webservice;

import static spark.Spark.get;
import static spark.SparkBase.externalStaticFileLocation;
import static spark.SparkBase.setPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.torrenttunes.client.DataSources;
import com.torrenttunes.client.Tools;

public class WebService {
static final Logger log = LoggerFactory.getLogger(WebService.class);

	
	public static void start() {
		


//				setupSSL();

		// Add external web service url to beginning of javascript tools
		//		Tools.addExternalWebServiceVarToTools();

		setPort(DataSources.SPARK_WEB_PORT) ;

		externalStaticFileLocation(DataSources.WEB_HOME());
		
		Platform.setup();
		
//		API.setup(tracker);
		
	
		get("/hello", (req, res) -> {
			Tools.allowOnlyLocalHeaders(req, res);
			return "hi from the torrenttunes-client web service";
		});
		
		get("/:page", (req, res) -> {
			Tools.allowOnlyLocalHeaders(req, res);	
			String pageName = req.params(":page");
			return Tools.readFile(DataSources.PAGES(pageName));
		});

	
	}
}
