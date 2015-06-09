package com.torrenttunes.client;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import com.torrenttunes.client.db.InitializeTables;
import com.torrenttunes.client.webservice.WebService;


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
		
		EmbeddedBrowser.start();
		
		WebService.start();
		
//		LibtorrentEngine.INSTANCE.startSeedingLibrary();
		
		
//		ScanDirectory.start(new File(DataSources.SAMPLE_MUSIC_DIR), torrentClient);

//		TorrentHandle torrent = LibtorrentEngine.INSTANCE.addTorrent(new File(DataSources.HOME_DIR()), 
//				DataSources.SAMPLE_TORRENT);
		

//		log.info(TorrentStats.create(torrent).toString());
	
		
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
