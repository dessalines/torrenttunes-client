package com.torrenttunes.client.tools;

import java.io.File;
import java.util.Collection;

import com.torrenttunes.client.ScanDirectory;

// java -cp target/torrenttunes-client.jar com.torrenttunes.client.tools.UntaggedSongs /media/tyler/Tyhous_HD/Music/
public class UntaggedSongs {

	public static void main(String[] args) {
		File dir = new File(args[0]);
		Collection<File> untagged = ScanDirectory.fetchUntaggedSongsFromDir(dir);
		
		for (File e : untagged) {
			System.out.println(e.getParentFile().getAbsolutePath() + " / " + e.getName());
		}
		
	}
	
	
}
