package com.torrenttunes.client.tools;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.torrenttunes.client.webservice.Platform;

public class DataSources {
	
	static final Logger log = LoggerFactory.getLogger(DataSources.class);

	public static String APP_NAME = "torrenttunes-client";
	
	public static Integer SPARK_WEB_PORT = 4568;
	
	public static final String WEB_SERVICE_URL = "http://localhost:" + SPARK_WEB_PORT + "/";
	

	
	public static final File SAMPLE_TORRENT = new File("/home/tyler/Downloads/[kat.cr]devious.maids.s03e01.hdtv.x264.asap.ettv.torrent");
	
	// The path to the torrenttunes dir
	public static String HOME_DIR() {
		String userHome = System.getProperty( "user.home" ) + "/." + APP_NAME;
		return userHome;
	}
	
	public static final String TORRENTS_DIR() {return HOME_DIR() + "/torrents";}
	
	public static final String DEFAULT_MUSIC_STORAGE_PATH() {return HOME_DIR() + "/cache";}
	
	public static String MUSIC_STORAGE_PATH = DEFAULT_MUSIC_STORAGE_PATH();
	
	public static final String CACHE_FILE(String fileName) {return MUSIC_STORAGE_PATH + "/" + fileName;}
	
	public static final String SAMPLE_TORRENT_FILE() {return TORRENTS_DIR() + 
			"/[kat.cr]fugazi.studio.discography.1989.2001.flac.torrent";
	}
	
	public static final String SAMPLE_MUSIC_DIR = "/home/tyler/Downloads";
	
	public static final String DB_FILE() {return HOME_DIR() + "/db/db.sqlite";}
	
	
	// This should not be used, other than for unzipping to the home dir
	public static final String CODE_DIR = System.getProperty("user.dir");
	
	public static final String SOURCE_CODE_HOME() {return HOME_DIR() + "/src";}
	
	public static final String SQL_FILE() {return SOURCE_CODE_HOME() + "/ddl.sql";}
	public static final String SQL_VIEWS_FILE() {return SOURCE_CODE_HOME() + "/views.sql";}
	
	public static final String SHADED_JAR_FILE = CODE_DIR + "/target/" + APP_NAME + ".jar";

	public static final String SHADED_JAR_FILE_2 = CODE_DIR + "/" + APP_NAME + ".jar";
	
	public static final String ZIP_FILE() {return HOME_DIR() + "/" + APP_NAME + ".zip";}
	
	public static final String TOOLS_JS() {return SOURCE_CODE_HOME() + "/web/js/tools.js";}

	// Web pages
	public static final String WEB_HOME() {return SOURCE_CODE_HOME() + "/web";}

	public static final String WEB_HTML() {return WEB_HOME() + "/html";}
	
	public static final String WEB_SERVICE_STARTED_URL() {return WEB_SERVICE_URL + "hello";}
	
	public static final String MAIN_PAGE_URL() {return "file://" + WEB_HTML() + "/main.html";}
	

	public static final String PAGES(String pageName) {
		return WEB_HTML() + "/" + pageName + ".html";
	}
	
	
	public static final String TRACKER_IP = "104.236.44.89";
//	public static final String TRACKER_IP = "127.0.0.1";
	
	public static final String TRACKER_WEB_PORT = "4567";
	
	public static final String TRACKER_URL = "http://" + TRACKER_IP + ":" + TRACKER_WEB_PORT + "/";
	
	public static final String TORRENT_UPLOAD_URL = TRACKER_URL + "torrent_upload";
	
	public static final String TORRENT_INFO_UPLOAD_URL = TRACKER_URL + "torrent_info_upload";

	public static final String TORRENT_DOWNLOAD_URL(String infoHash) {
		return TRACKER_URL + "download_torrent/" + infoHash;
	}
	
	public static final String TORRENT_INFO_DOWNLOAD_URL(String infoHash) {
		return TRACKER_URL + "download_torrent_info/" + infoHash;
	}
	
	public static final String TRACKER_ANNOUNCE = "http://" + TRACKER_IP + ":6969/announce";

	public static final String SEEDER_INFO_UPLOAD(String infoHash, String seeders) {
		return TRACKER_URL + "seeder_upload/" + infoHash + "/" + seeders;
	}
	
	
	

	public static final String LIBTORRENT_OS_LIBRARY_PATH() {
		String osName = System.getProperty("os.name");
		log.info("Operating system " + osName);
		
		String ret = SOURCE_CODE_HOME() + "/lib/libjlibtorrent.so";
		log.info(ret);
		return ret;
	}
	
	public static final List<URI> ANNOUNCE_LIST() {
		List<URI> list = null;
		try {
			list = Arrays.asList(
//				new URI(TRACKER_ANNOUNCE),
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
