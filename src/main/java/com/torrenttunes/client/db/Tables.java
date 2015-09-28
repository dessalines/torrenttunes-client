package com.torrenttunes.client.db;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.DbName;
import org.javalite.activejdbc.annotations.Table;


public class Tables {

	@DbName("ttc")
	@Table("settings")
	public static class Settings extends Model {}
	public static final Settings SETTINGS = new Settings();
	
	@DbName("ttc")
	@Table("library")
	public static class Library extends Model {}
	public static final Library LIBRARY = new Library();
	
	
	

}
