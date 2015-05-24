package com.torrenttunes.client.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.torrenttunes.client.DataSources;
import com.torrenttunes.client.Tools;


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
			} else {
				log.info("DB already exists");

			}
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.exit(0);
		}
	}

}

