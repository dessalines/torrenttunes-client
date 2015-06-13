package com.torrenttunes.client.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.torrenttunes.client.db.Tables.Settings;
import com.torrenttunes.client.tools.DataSources;
import com.torrenttunes.client.tools.Tools;

import static com.torrenttunes.client.db.Tables.*;


public class InitializeTables {

	static final Logger log = LoggerFactory.getLogger(InitializeTables.class);


	public static void initializeTables() {
		log.info("Using database located at : " + DataSources.DB_FILE());
		createTables(false);
	}

	public static void createTables(Boolean delete) {
		Connection c = null;

		try {
			if (delete == true) {
				new File(DataSources.DB_FILE()).delete();
				log.info("DB deleted");
			}

			Class.forName("org.sqlite.JDBC");
			if (!new File(DataSources.DB_FILE()).exists()) {
				new File(DataSources.DB_FILE()).getParentFile().mkdirs();
				new File(DataSources.DB_FILE());
				c = DriverManager.getConnection("jdbc:sqlite:" + DataSources.DB_FILE());
				log.info("Opened database successfully");

				Tools.runSQLFile(c, new File(DataSources.SQL_FILE()));
				Tools.runSQLFile(c, new File(DataSources.SQL_VIEWS_FILE()));

				c.close();

				log.info("Table created successfully");
				
				fillTables();
			} else {
				log.info("DB already exists");

			}
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.exit(0);
		}
	}
	
	public static void fillTables() {
		Tools.dbInit();
		fillSettingsTable();
		Tools.dbClose();
	}
	
	public static void fillSettingsTable() {
		
		SETTINGS.createIt("storage_path", DataSources.DEFAULT_MUSIC_STORAGE_PATH(),
				"max_download_speed", -1,
				"max_upload_speed", -1,
				"max_cache_size_mb", -1);
		
	}

}

