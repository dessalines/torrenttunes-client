package com.torrenttunes.client;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.torrenttunes.client.tools.DataSources;
import com.torrenttunes.client.tools.Tools;

public class Updater {

	static final Logger log = LoggerFactory.getLogger(Updater.class);

	public static void main(String[] args) {
		checkForUpdate();
	}


	public static void checkForUpdate() {

		log.info("Checking for update...");
		String jsonStr = Tools.httpGetString(DataSources.FETCH_LATEST_RELEASE_URL());
		log.info(DataSources.FETCH_LATEST_RELEASE_URL());
		log.info(jsonStr);
		
		
		JsonNode json = Tools.jsonToNode(jsonStr);

		String tagName = json.get("tag_name").asText();

		if (!DataSources.TAG_NAME.equals(tagName)) {
			downloadAndInstallJar(json);

		} else {
			log.info("No updates found");

		}





	}
	public static void downloadAndInstallJar(JsonNode json) {
		log.info("Update found, downloading jar and installing update");

		try {
			// Download the jar
			String downloadUrl = json.get("assets").get(0).get("browser_download_url").asText();
			log.info(downloadUrl);

			Tools.httpSaveFile(downloadUrl, DataSources.TEMP_JAR_PATH());

			
			// Run the shortcut install script, recopying the source files, and only installing
			String cmd = "java -jar " + DataSources.TEMP_JAR_PATH() + " -recopy -installonly";
			Process p = Runtime.getRuntime().exec(cmd);
			
			p.waitFor();
			
			// Delete the temp download filefile
			new File(DataSources.TEMP_JAR_PATH()).delete();
			
			Tools.restartApplication();
			
			


		} catch (IOException | URISyntaxException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
