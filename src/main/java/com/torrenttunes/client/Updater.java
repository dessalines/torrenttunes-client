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
		String htmlStr = Tools.httpGetString(DataSources.FETCH_LATEST_RELEASE_URL());
//		log.info(DataSources.FETCH_LATEST_RELEASE_URL());
//		log.info(htmlStr);

		String tagName = htmlStr.split("/tchoulihan/torrenttunes-client/releases/tag/")[1].split("\"")[0];
		log.info("Latest Tag #: " + tagName);
			
		if (!DataSources.TAG_NAME.equals(tagName)) {
			downloadAndInstallJar(tagName);

		} else {
			log.info("No updates found");

		}





	}
	public static void downloadAndInstallJar(String tagName) {
		log.info("Update found, downloading jar and installing update");

		try {
			// Download the jar
			String downloadUrl = "https://github.com/tchoulihan/torrenttunes-client/releases/download/" + tagName + 
					"/torrenttunes-client.jar";
			log.info(downloadUrl);
			
			Tools.httpSaveFile(downloadUrl, DataSources.TEMP_JAR_PATH());

			
			// Run the shortcut install script, recopying the source files, and only installing
			String cmd = "java -jar " + DataSources.TEMP_JAR_PATH() + " -recopy";
			Process p = Runtime.getRuntime().exec(cmd);
		
			System.exit(0);
			
			// Delete the temp download filefile
//			new File(DataSources.TEMP_JAR_PATH()).delete();


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
