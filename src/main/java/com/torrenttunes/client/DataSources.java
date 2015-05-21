package com.torrenttunes.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class DataSources {

	public static String APP_NAME = "torrenttunes-client";
	
	
	// The path to the ytm dir
	public static String HOME_DIR() {
		String userHome = System.getProperty( "user.home" ) + "/." + APP_NAME;
		return userHome;
	}
	
	public static final String TORRENTS_DIR() {return HOME_DIR() + "/torrents";}
	
	public static final String SAMPLE_TORRENT_FILE() {return TORRENTS_DIR() + 
			"/[kat.cr]fugazi.studio.discography.1989.2001.flac.torrent";
	}
	
	public static final String SAMPLE_MUSIC_DIR = "/home/tyler/Downloads";
	
	public static final String DB_FILE() {return HOME_DIR() + "/db/db.sqlite";}
	
	
	// This should not be used, other than for unzipping to the home dir
	public static final String CODE_DIR = System.getProperty("user.dir");
	
	public static final String SOURCE_CODE_HOME() {return HOME_DIR() + "/src";}
	
	public static final String SQL_FILE() {return SOURCE_CODE_HOME() + "/ddl.sql";}
	
	public static final String SHADED_JAR_FILE = CODE_DIR + "/target/" + APP_NAME + ".jar";

	public static final String SHADED_JAR_FILE_2 = CODE_DIR + "/" + APP_NAME + ".jar";
	
	public static final String ZIP_FILE() {return HOME_DIR() + "/" + APP_NAME + ".zip";}
	
	
	
	public static final List<URI> ANNOUNCE_LIST() {
		List<URI> list = null;
		try {
			list = Arrays.asList(
				new URI("http://127.0.0.1:6969/announce"),
				new URI("http://9.rarbg.com:2710/announce"),
				new URI("http://announce.torrentsmd.com:6969/announce"),
				new URI("http://bt.careland.com.cn:6969/announce"),
				new URI("http://explodie.org:6969/announce"),
				new URI("http://mgtracker.org:2710/announce"),
				new URI("http://tracker.best-torrents.net:6969/announce"),
				new URI("http://tracker.tfile.me/announce"),
				new URI("http://tracker.torrenty.org:6969/announce"),
				new URI("http://tracker1.wasabii.com.tw:6969/announce"),
				new URI("udp://9.rarbg.com:2710/announce"),
				new URI("udp://9.rarbg.me:2710/announce"),
				new URI("udp://coppersurfer.tk:6969/announce"),
				new URI("udp://exodus.desync.com:6969/announce"),
				new URI("udp://open.demonii.com:1337/announce"),
				new URI("udp://tracker.btzoo.eu:80/announce"),
				new URI("udp://tracker.istole.it:80/announce"),
				new URI("udp://tracker.openbittorrent.com:80/announce"),
				new URI("udp://tracker.prq.to/announce"),
				new URI("udp://tracker.publicbt.com:80/announce"));
		} catch (URISyntaxException e) {}
		
		return list;
	}
	
	
}
