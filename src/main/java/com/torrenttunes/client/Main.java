package com.torrenttunes.client;

import java.io.File;


public class Main {
	
	public static void main(String[] args) {
		new Main().doMain(args);

	}
	
	public void doMain(String[] args) {
		
		// Initialize
		Tools.setupDirectories();
		
		Tools.copyResourcesToHomeDir(true);
		
//		InitializeTables.initializeTables();
		
		ScanDirectory.start(new File(DataSources.SAMPLE_MUSIC_DIR));
		
	}
}
