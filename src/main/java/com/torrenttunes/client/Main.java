package com.torrenttunes.client;

import java.io.File;

import com.torrenttunes.client.db.InitializeTables;


public class Main {
	
	public TorrentClient torrentClient;
	
	public static void main(String[] args) {
		new Main().doMain(args);

	}
	
	public void doMain(String[] args) {
		
		// Initialize
		Tools.setupDirectories();
		
		Tools.copyResourcesToHomeDir(true);
		
		InitializeTables.initializeTables();
		
		torrentClient = TorrentClient.start();
		
		ScanDirectory.start(new File(DataSources.SAMPLE_MUSIC_DIR), torrentClient);
		
	}
}
