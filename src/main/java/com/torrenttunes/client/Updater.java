package com.torrenttunes.client;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
		log.info("Current Tag #: " + DataSources.VERSION);
		log.info("Latest Tag #: " + tagName);
			
		if (!DataSources.VERSION.equals(tagName)) {
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
			
			Thread.sleep(1000);
			log.info("sleeping");
			
			// Run the shortcut install script, recopying the source files, and only installing
			ArrayList<String> cmd = new ArrayList<String>();
			cmd.add("java");
			cmd.add("-jar");
			cmd.add(DataSources.TEMP_JAR_PATH());
			cmd.add("-recopy");
//			cmd.add("&>log.out");
//			cmd.add("");
			
//			String cmd = "java -jar " + DataSources.TEMP_JAR_PATH() + " -recopy -installonly";
			ProcessBuilder b = new ProcessBuilder(cmd);
			Process p = b.start();
//			new File(DataSources.JAR_FILE()).deleteOnExit();
			System.exit(0);
//			p.waitFor();

//			
//			
////			 Delete the temp download filefile
//			new File(DataSources.TEMP_JAR_PATH()).delete();
//			
//			cmd.clear();
//			cmd.add("java");
//			cmd.add("-jar");
//			cmd.add(DataSources.JAR_FILE());
////			cmd = "java -jar " + DataSources.JAR_FILE();
//			ProcessBuilder b2 = new ProcessBuilder(cmd);
//			b2.start();
//		
//			System.exit(0);
			



		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
