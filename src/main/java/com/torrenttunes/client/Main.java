package com.torrenttunes.client;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import com.torrenttunes.client.db.InitializeTables;
import com.torrenttunes.client.webservice.WebService;

import static com.torrenttunes.client.db.Tables.*;


public class Main {
	
	static Logger log = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	
	@Option(name="-uninstall",usage="Uninstall torrenttunes-client.(WARNING, this deletes your library)")
	private boolean uninstall;
	
	@Option(name="-loglevel", usage="Sets the log level [INFO, DEBUG, etc.]")     
	private String loglevel = "INFO";
	
	
	public void doMain(String[] args) {
		
		parseArguments(args);
		
		// See if the user wants to uninstall it
		if (uninstall) {
			Tools.uninstall();
		}
		
		
		log.setLevel(Level.toLevel(loglevel));
		
		
		// Initialize
		Tools.setupDirectories();
		
		Tools.copyResourcesToHomeDir(true);
		
		Tools.addExternalWebServiceVarToTools();

		InitializeTables.initializeTables();
		
		// Set the music storage location
		setupMusicStoragePath();
		
		WebService.start();
		
		Tools.pollAndOpenStartPage();
		
		LibtorrentEngine.INSTANCE.startSeedingLibrary();
		
	
	
		
	}

	private static void setupMusicStoragePath() {
		Tools.dbInit();
		Settings s = SETTINGS.findFirst("id = ?", 1);
		Tools.dbClose();
		String storagePath = s.getString("storage_path");
		DataSources.MUSIC_STORAGE_PATH = storagePath;
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
