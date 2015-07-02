package com.torrenttunes.client.tools;

import java.io.File;
import java.util.Collection;

import com.torrenttunes.client.ScanDirectory;

public class UntaggedSongs {

	public static void main(String[] args) {
		File dir = new File("/media/tyler/Tyhous_HD/Music/COLDPLAY - DISCOGRAPHY (1998-14) [CHANNEL NEO]");
		Collection<File> untagged = ScanDirectory.fetchUntaggedSongsFromDir(dir);
		
		for (File e : untagged) {
			System.out.println(e.getParentFile().getName() + " / " + e.getName());
		}
		
	}
	
	
}
