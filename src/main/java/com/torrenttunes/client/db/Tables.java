package com.torrenttunes.client.db;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;


public class Tables {


	@Table("settings")
	public static class Settings extends Model {}
	public static final Settings SETTINGS = new Settings();
	
	@Table("library")
	public static class Library extends Model {}
	public static final Library LIBRARY = new Library();
	
	@Table("queue_track")
	public static class QueueTrack extends Model {}
	public static final QueueTrack QUEUE_TRACK = new QueueTrack();
	
	@Table("playlist_track")
	public static class PlaylistTrack extends Model {}
	public static final PlaylistTrack PLAYLIST_TRACK = new PlaylistTrack();
	
	@Table("playlist")
	public static class Playlist extends Model {}
	public static final Playlist PLAYLIST = new Playlist();
	
	

}
