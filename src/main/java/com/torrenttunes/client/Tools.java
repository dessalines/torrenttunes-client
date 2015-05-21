package com.torrenttunes.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.javalite.activejdbc.DB;
import org.javalite.activejdbc.DBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

public class Tools {
	
	static final Logger log = LoggerFactory.getLogger(Tools.class);

			
	public static FilenameFilter MUSIC_FILE_FILTER = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.endsWith(".mp3");
		}
	};
	
	
	public static String sha2FileChecksum(File file) {
		HashCode hc = null;
		try {
			hc = Files.hash(file, Hashing.sha256());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return hc.toString();
	}
	
	public static void setupDirectories() {
		if (!new File(DataSources.HOME_DIR()).exists()) {
			log.info("Setting up ~/." + DataSources.APP_NAME + " dirs");
			new File(DataSources.HOME_DIR()).mkdirs();
			new File(DataSources.TORRENTS_DIR()).mkdirs();
		} else {
			log.info("Home directory already exists");
		}
	}

	public static void copyResourcesToHomeDir(Boolean copyAnyway) {


		String zipFile = null;

		if (copyAnyway || !new File(DataSources.SOURCE_CODE_HOME()).exists()) {
			log.info("Copying resources to  ~/." + DataSources.APP_NAME + " dirs");

			try {
				if (new File(DataSources.SHADED_JAR_FILE).exists()) {
					java.nio.file.Files.copy(Paths.get(DataSources.SHADED_JAR_FILE), Paths.get(DataSources.ZIP_FILE()), 
							StandardCopyOption.REPLACE_EXISTING);
					zipFile = DataSources.SHADED_JAR_FILE;

				} else if (new File(DataSources.SHADED_JAR_FILE_2).exists()) {
					java.nio.file.Files.copy(Paths.get(DataSources.SHADED_JAR_FILE_2), Paths.get(DataSources.ZIP_FILE()),
							StandardCopyOption.REPLACE_EXISTING);
					zipFile = DataSources.SHADED_JAR_FILE_2;
				} else {
					log.info("you need to build the project first");
				}
			} catch (IOException e) {

				e.printStackTrace();
			}
			Tools.unzip(new File(zipFile), new File(DataSources.SOURCE_CODE_HOME()));
			//		new Tools().copyJarResourcesRecursively("src", configHome);
		} else {
			log.info("The source directory already exists");
		}
	}

	public static void unzip(File zipfile, File directory) {
		try {
			ZipFile zfile = new ZipFile(zipfile);
			Enumeration<? extends ZipEntry> entries = zfile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				File file = new File(directory, entry.getName());
				if (entry.isDirectory()) {
					file.mkdirs();
				} else {
					file.getParentFile().mkdirs();
					InputStream in = zfile.getInputStream(entry);
					try {
						copy(in, file);
					} finally {
						in.close();
					}
				}
			}

			zfile.close();


		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void copy(InputStream in, File file) throws IOException {
		OutputStream out = new FileOutputStream(file);
		try {
			copy(in, out);
		} finally {
			out.close();
		}
	}

	private static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		while (true) {
			int readCount = in.read(buffer);
			if (readCount < 0) {
				break;
			}
			out.write(buffer, 0, readCount);
		}
	}

	private static void copy(File file, OutputStream out) throws IOException {
		InputStream in = new FileInputStream(file);
		try {
			copy(in, out);
		} finally {
			in.close();
		}
	}

	public static void runSQLFile(Connection c,File sqlFile) {

		try {
			Statement stmt = null;
			stmt = c.createStatement();
			String sql;

			sql = Files.toString(sqlFile, Charset.defaultCharset());

			stmt.executeUpdate(sql);
			stmt.close();
		} catch (IOException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static final void dbInit() {
		try {
			new DB("default").open("org.sqlite.JDBC", "jdbc:sqlite:" + DataSources.DB_FILE(), "root", "p@ssw0rd");
		} catch (DBException e) {
			e.printStackTrace();
			dbClose();
			dbInit();
		}

	}

	public static final void dbClose() {
		new DB("default").close();
	}
	
	public static String constructTrackTorrentFilename(File file, String mbid) {
		return mbid.toLowerCase() + "_" + sha2FileChecksum(file);
	}
	
}
