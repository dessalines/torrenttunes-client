package com.torrenttunes.client.tools.watchservice;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.torrenttunes.client.LibtorrentEngine;

public class Watcher {

	static final Logger log = LoggerFactory.getLogger(Watcher.class);

	public static void watch(String dir) {
		try {

			File outputDirectory = new File(dir, "downloads");


			DirectoryWatchService watchService = new SimpleDirectoryWatchService(); // May throw
			watchService.register( // May throw
					new DirectoryWatchService.OnFileChangeListener() {
						@Override
						public void onFileCreate(String torrentFile) {
							LibtorrentEngine.INSTANCE.addTorrent(
									outputDirectory, new File(dir, torrentFile), false, false);
						}

						@Override
						public void onFileModify(String torrentFile) {
							// File modified
						}

						@Override
						public void onFileDelete(String torrentFile) {
							// don't do nothing yet. 							
						}
					},
					dir, // Directory to watch
					"*.torrent" // E.g. "*.log"
					);

			watchService.start();
		} catch (IOException e) {
			log.error("Unable to register file change listener for " + dir);
		}
	}
}
