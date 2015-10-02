package com.torrenttunes.client;

import static com.torrenttunes.client.db.Tables.SETTINGS;
import static spark.Spark.awaitInitialization;

import java.io.File;
import java.util.Locale;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import com.torrenttunes.client.db.Actions;
import com.torrenttunes.client.db.InitializeTables;
import com.torrenttunes.client.db.Tables.Settings;
import com.torrenttunes.client.tools.DataSources;
import com.torrenttunes.client.tools.Tools;
import com.torrenttunes.client.webservice.WebService;


public class Main {

	public static Logger log = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

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
		

		parseArguments(args);

		// See if the user wants to uninstall it
		if (uninstall) {
			Tools.uninstall();
		}

		log.setLevel(Level.toLevel(loglevel));
		log.getLoggerContext().getLogger("org.eclipse.jetty").setLevel(Level.OFF);
		log.getLoggerContext().getLogger("spark.webserver").setLevel(Level.OFF);

		// Install Shortcuts
		Tools.setupDirectories();

		Tools.copyResourcesToHomeDir(recopy);

		Tools.addExternalWebServiceVarToTools();

		InitializeTables.initializeTables();

		if (installOnly) {
			System.exit(0);
		}
		
		setupSettings();

		WebService.start();
		
		awaitInitialization();

		openCorrectLanguageHomePage();
		
		if (shareDirectory != null) {
			ScanDirectory.start(new File(shareDirectory));
		}
	
		
		LibtorrentEngine.INSTANCE.startSeedingLibraryVersion1();




	}
	
	public static void openCorrectLanguageHomePage() {
		String lang2 = Locale.getDefault().getLanguage();
		String lang = System.getProperty("user.language");
		log.info("System language = " + lang + " or Locale language: " + lang2);
		
		if (lang.equals("es")) {
			Tools.openFileWebpage(DataSources.MAIN_PAGE_URL_ES());
		} else {
			Tools.openFileWebpage(DataSources.MAIN_PAGE_URL_EN());
		}
	}

	public static void setupSettings() {
		Tools.dbInit();
		Settings s = SETTINGS.findFirst("id = ?", 1);
		Tools.dbClose();

		Actions.setupMusicStoragePath(s);
		Actions.updateLibtorrentSettings(s);
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
			System.err.println("java -jar " + DataSources.APP_NAME + ".jar [options...] arguments...");
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
