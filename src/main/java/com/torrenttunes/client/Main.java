package com.torrenttunes.client;

import java.io.File;
import java.util.Arrays;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import com.torrenttunes.client.db.InitializeTables;
import com.torrenttunes.client.tools.DataSources;
import com.torrenttunes.client.tools.Tools;
import com.torrenttunes.client.webservice.WebService;

import static com.torrenttunes.client.db.Tables.*;


public class Main {

	static Logger log = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

	@Option(name="-uninstall",usage="Uninstall torrenttunes-client.(WARNING, this deletes your library)")
	private boolean uninstall;

	@Option(name="-recopy",usage="Recopies your source folders")
	private boolean recopy;

	@Option(name="-installonly",usage="Only installs it, doesn't run it")
	private boolean installOnly;

	@Option(name="-loglevel", usage="Sets the log level [INFO, DEBUG, etc.]")     
	private String loglevel = "INFO";

	@Option(name="-sharedirectory", usage="Scans a directory to share")     
	private String shareDirectory = null;


	public void doMain(String[] args) {

		log.info(Arrays.toString(args));
		parseArguments(args);

		// See if the user wants to uninstall it
		if (uninstall) {
			Tools.uninstall();
		}

		log.setLevel(Level.toLevel(loglevel));

		// Install Shortcuts
		Tools.setupDirectories();

		// set recopy to default
		Tools.copyResourcesToHomeDir(recopy);

		Tools.addExternalWebServiceVarToTools();

		InitializeTables.initializeTables();

		if (installOnly) {
			System.exit(0);
		}
		
		setupSettings();

		WebService.start();

		Tools.pollAndOpenStartPage();

		if (shareDirectory != null) {
			ScanDirectory.start(new File(shareDirectory));
		}

		LibtorrentEngine.INSTANCE.startSeedingLibrary();




	}

	public static void setupSettings() {
		Tools.dbInit();
		Settings s = SETTINGS.findFirst("id = ?", 1);
		Tools.dbClose();

		setupMusicStoragePath(s);
		setupLibTorrentSettings(s);
	}


	public static void setupMusicStoragePath(Settings s) {
		String storagePath = s.getString("storage_path");
		DataSources.MUSIC_STORAGE_PATH = storagePath;
		log.info("Storage path = " + DataSources.MUSIC_STORAGE_PATH);
	}

	private static void setupLibTorrentSettings(Settings s) {
		LibtorrentEngine lte = LibtorrentEngine.INSTANCE;
		Integer maxDownloadSpeed = s.getInteger("max_download_speed");
		Integer maxUploadSpeed = s.getInteger("max_upload_speed");
		maxDownloadSpeed = (maxDownloadSpeed != -1) ? maxDownloadSpeed : 0;
		maxUploadSpeed = (maxUploadSpeed != -1) ? maxUploadSpeed : 0;
		//		lte.getSessionSettings().setDownloadRateLimit(1000 * maxDownloadSpeed);
		//		lte.getSessionSettings().setUploadRateLimit(1000 * maxUploadSpeed);
		//		lte.updateSettings();
	}





	private void parseArguments(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);

		try {

			parser.parseArgument(args);

		} catch (CmdLineException e) {
			// if there's a problem in the command line,
			// you'll get this exception. this will report
			// an error message.
			System.err.println(e.getMessage());
			System.err.println("java -jar torrenttunes-client.jar [options...] arguments...");
			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();
			System.exit(0);


			return;
		}
	}


	public static void main(String[] args) {
		new Main().doMain(args);

	}


}
