package com.torrenttunes.client;

import static com.torrenttunes.client.db.Tables.LIBRARY;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import com.frostwire.jlibtorrent.AlertListener;
import com.frostwire.jlibtorrent.DHT;
import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.Fingerprint;
import com.frostwire.jlibtorrent.LibTorrent;
import com.frostwire.jlibtorrent.Pair;
import com.frostwire.jlibtorrent.Session;
import com.frostwire.jlibtorrent.SettingsPack;
import com.frostwire.jlibtorrent.StatsMetric;
import com.frostwire.jlibtorrent.TorrentAlertAdapter;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.jlibtorrent.Vectors;
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import com.frostwire.jlibtorrent.alerts.BlockDownloadingAlert;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.BlockTimeoutAlert;
import com.frostwire.jlibtorrent.alerts.DhtReplyAlert;
import com.frostwire.jlibtorrent.alerts.FastresumeRejectedAlert;
import com.frostwire.jlibtorrent.alerts.FileErrorAlert;
import com.frostwire.jlibtorrent.alerts.HashFailedAlert;
import com.frostwire.jlibtorrent.alerts.PeerBanAlert;
import com.frostwire.jlibtorrent.alerts.PeerBlockedAlert;
import com.frostwire.jlibtorrent.alerts.PeerConnectAlert;
import com.frostwire.jlibtorrent.alerts.PeerDisconnectedAlert;
import com.frostwire.jlibtorrent.alerts.PeerErrorAlert;
import com.frostwire.jlibtorrent.alerts.PeerLogAlert;
import com.frostwire.jlibtorrent.alerts.PeerSnubbedAlert;
import com.frostwire.jlibtorrent.alerts.PeerUnsnubbedAlert;
import com.frostwire.jlibtorrent.alerts.PerformanceAlert;
import com.frostwire.jlibtorrent.alerts.RequestDroppedAlert;
import com.frostwire.jlibtorrent.alerts.SaveResumeDataAlert;
import com.frostwire.jlibtorrent.alerts.SaveResumeDataFailedAlert;
import com.frostwire.jlibtorrent.alerts.ScrapeFailedAlert;
import com.frostwire.jlibtorrent.alerts.ScrapeReplyAlert;
import com.frostwire.jlibtorrent.alerts.SessionStatsAlert;
import com.frostwire.jlibtorrent.alerts.StateChangedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentAddedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentCheckedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentDeleteFailedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentDeletedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentErrorAlert;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentPausedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentPrioritizeAlert;
import com.frostwire.jlibtorrent.alerts.TorrentRemovedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentResumedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentUpdateAlert;
import com.frostwire.jlibtorrent.alerts.TrackerAnnounceAlert;
import com.frostwire.jlibtorrent.alerts.TrackerErrorAlert;
import com.frostwire.jlibtorrent.alerts.TrackerReplyAlert;
import com.frostwire.jlibtorrent.alerts.TrackerWarningAlert;
import com.frostwire.jlibtorrent.alerts.UnwantedBlockAlert;
import com.frostwire.jlibtorrent.swig.add_torrent_params;
import com.frostwire.jlibtorrent.swig.default_storage;
import com.frostwire.jlibtorrent.swig.libtorrent;
import com.frostwire.jlibtorrent.swig.settings_pack.bool_types;
import com.frostwire.jlibtorrent.swig.settings_pack.int_types;
import com.frostwire.jlibtorrent.swig.storage_mode_t;
import com.google.common.collect.Lists;
import com.torrenttunes.client.ScanDirectory.ScanInfo;
import com.torrenttunes.client.ScanDirectory.ScanStatus;
import com.torrenttunes.client.db.Tables.Library;
import com.torrenttunes.client.tools.DataSources;
import com.torrenttunes.client.tools.Tools;


public enum LibtorrentEngine  {

	INSTANCE;

	private final Logger log = LoggerFactory.getLogger(LibtorrentEngine.class);


	private Session session;
	private SettingsPack settings;
	private Map<String, TorrentHandle> infoHashToTorrentMap;

	private Set<ScanInfo> scanInfos;

	private List<String> sessionStatsHeaders;

	private long startTime;


	private LibtorrentEngine() {

		startTime = System.nanoTime();

		System.setProperty("jlibtorrent.jni.path", DataSources.LIBTORRENT_OS_LIBRARY_PATH());



		log.info("Starting up libtorrent with version: " + LibTorrent.version());

		Pair<Integer, Integer> prange = new Pair<Integer, Integer>(49154, 65535);
		String iface = "0.0.0.0";

		if (Main.log.getLevel().equals(Level.DEBUG)) {
			// Create a session stats file with headers
			createSessionStatsFile();
			default_storage.disk_write_access_log(true);
			libtorrent.set_utp_stream_logging(true);
			session = new Session(new Fingerprint(), prange, iface, defaultRouters(), true);
			addDefaultSessionAlerts();
		} else {
			session = new Session(new Fingerprint(), prange, iface, defaultRouters(), false);
		}


		settings = new SettingsPack();


		settings.setActiveDownloads(10);
		settings.setActiveSeeds(999999);
		settings.setInteger(int_types.active_limit.swigValue(), 999999);
		settings.setInteger(int_types.active_tracker_limit.swigValue(), 999999);

		settings.setUploadRateLimit(0);
		settings.setDownloadRateLimit(0);

		settings.setBoolean(bool_types.announce_double_nat.swigValue(), true);
		settings.setInteger(int_types.peer_connect_timeout.swigValue(), 60);

		settings.setInteger(int_types.file_pool_size.swigValue(), 200000);

		settings.setInteger(int_types.tracker_completion_timeout.swigValue(), 10);
		settings.setBoolean(bool_types.incoming_starts_queued_torrents.swigValue(), true);

		settings.setInteger(int_types.peer_timeout.swigValue(), 20);

		settings.setInteger(int_types.alert_queue_size.swigValue(), 1000000);


		DHT dht = new DHT(session);
		dht.stop();


		settings.broadcastLSD(false);
		
		settings.setMaxPeerlistSize(500);
		settings.setInteger(int_types.min_announce_interval.swigValue(), 1740);

//		settings.setBoolean(bool_types.utp_dynamic_sock_buf.swigValue(), false);
		//		settings.setBoolean(bool_types.enable_outgoing_utp.swigValue(), false);
		//		settings.setBoolean(bool_types.enable_incoming_utp.swigValue(), false);



		//		sessionSettings.setInteger(int_types.mixed_mode_algorithm.swigValue(), 
		//				bandwidth_mixed_algo_t.prefer_tcp.swigValue());



		//		sessionSettings.setInteger(int_types.bandwidth_mixed_algo_t., value);
		//		bandwidth_mixed_algo_t.prefer_tcp

		//		sessionSettings = SessionSettings.newDefaults();
		//		sessionSettings = SessionSettings.newMinMemoryUsage();
		//		sessionSettings = SessionSettings.newHighPerformanceSeed();
		//				sessionSettings.setTorrentConnectBoost(5);
		//		sessionSettings.setMinReconnectTime(1);
		//		session.stopDHT();
		//		sessionSettings.setActiveLimit(999999);
		//						sessionSettings.setActiveDHTLimit(5);
		//		sessionSettings.setActiveTrackerLimit(999999);

		//		sessionSettings.announceDoubleNAT(true);
		//		sessionSettings.setPeerConnectTimeout(60);
		//		sessionSettings.useReadCache(false);
		//				sessionSettings.setMaxPeerlistSize(20);
		//		sessionSettings.setSeedChokingAlgorithm(SeedChokingAlgorithm.ROUND_ROBIN);
		//		sessionSettings.setChokingAlgorithm(ChokingAlgorithm.AUTO_EXPAND_CHOKER);
		//		sessionSettings.setHalgOpenLimit(5);
		//		sessionSettings.setMixedModeAlgorithm(BandwidthMixedAlgo.PEER_PROPORTIONAL);

		//		//		sessionSettings.setDHTAnnounceInterval(3600);
		//						sessionSettings.setMinAnnounceInterval(1740);

		//		sessionSettings.setLocalServiceAnnounceInterval(3600);


		//		sessionSettings.setNoConnectPrivilegedPorts(true);

		//		sessionSettings.setTrackerBackoff(10);


		//		sessionSettings.setAutoManageInterval(600);
		//		sessionSettings.setRateLimitIPOverhead(true);
		//		sessionSettings.setFreeTorrentHashes(true);
		//		sessionSettings.setFileChecksDelayPerBlock(1000);
		//		sessionSettings.setSuggestMode(SuggestMode.SUGGEST_READ_CACHE);
		//		sessionSettings.setFilePoolSize(200000);
		//		sessionSettings.setOptimizeHashingForSpeed(false);
		//		sessionSettings.setOptimisticDiskRetry(5);
		//		sessionSettings.setDiskCacheAlgorithm(DiskCacheAlgo.LRU);
		//		sessionSettings.setIncomingStartsQueuedTorrents(true);
		//		sessionSettings.setSeedTimeLimit(360);
		//		sessionSettings.setTrackerReceiveTimeout(1);
		//		sessionSettings.setTrackerCompletionTimeout(10);
		//		sessionSettings.setStopTrackerTimeout(1);
		//		sessionSettings.setActiveLsdLimit(1);

		//		sessionSettings.setPeerTimeout(5);
		//		sessionSettings.setInactivityTimeout(5);

		//				sessionSettings.setMaxPeerlistSize(10);


		//		sessionSettings.setMaxPausedPeerlistSize(0);
		//				sessionSettings.setChokingAlgorithm(ChokingAlgorithm.RATE_BASED_CHOKER);
		//		sessionSettings.setCacheSize(999999);


		//		sessionSettings.setPeerConnectTimeout(35);

		//		sessionSettings.allowMultipleConnectionsPerIp(true);

		//		sessionSettings.setPeerTimeout(15);
		//				sessionSettings.setInactivityTimeout(30);

		//				sessionSettings.setConnectionsLimit(100000);

		//				sessionSettings.setConnectionSpeed(3000);


		// Performance settings




		//		sessionSettings.setAutoManageInterval(10);
		//		sessionSettings.setAutoScrapeInterval(5);
		//		sessionSettings.setMinAnnounceInterval(5);

		//		sessionSettings.setAnnounceToAllTrackers(false);
		//		sessionSettings.setDHTAnnounceInterval(5);
		//		sessionSettings.setMaxAllowedInRequestQueue(9999);
		//		sessionSettings.setUnchokeSlotsLimit(800);
		//		sessionSettings.setCacheExpiry(9999);





		//		sessionSettings.setAutoManagePreferSeeds(true);

		//		sessionSettings.setSendBufferLowWatermark(50);

		//		session.setSettings(sessionSettings);





		session.applySettings(settings);


		log.info("Is DHT Running? " + session.isDHTRunning());

		this.scanInfos = new LinkedHashSet<ScanInfo>();
		this.infoHashToTorrentMap = new ConcurrentHashMap<String, TorrentHandle>();






	}


	private void createSessionStatsFile() {

		try {

			File file = new File(DataSources.SESSION_STATS_FILE());
			if (file.exists()) file.delete();

			file.createNewFile();

			StatsMetric[] ssm = LibTorrent.sessionStatsMetrics();

			sessionStatsHeaders = new ArrayList<>();

			// fill with dummy values, have no idea how many
			for (int i = 0; i < 400; i++) {sessionStatsHeaders.add(null);}


			sessionStatsHeaders.set(0, "second");
			// Create the headers using set
			for (int i = 0; i < ssm.length; i++) {
				StatsMetric sm = ssm[i];
				//				log.info("i = " + i + " valueindex = " + sm.valueIndex + " name = " + sm.name);
				sessionStatsHeaders.set(sm.valueIndex+1, sm.name);
			}


			String delim = "";
			for (String header : sessionStatsHeaders) {

				if (header != null) {
					String headerStr = delim + header;

					Files.write(Paths.get(file.getAbsolutePath()), 
							headerStr.getBytes(), StandardOpenOption.APPEND);
					delim = ":";
				}

			}



			Files.write(Paths.get(file.getAbsolutePath()), 
					"\n".getBytes(), StandardOpenOption.APPEND);




		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	private void addDefaultSessionAlerts() {

		session.addListener(new AlertListener() {

			@Override
			public int[] types() {
				return new int[]{AlertType.SESSION_STATS.getSwig(), AlertType.PEER_LOG.getSwig()};

			}

			@Override
			public void alert(Alert<?> alert) {
				try {
					if (alert instanceof SessionStatsAlert) {
						//						log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

						File file = new File(DataSources.SESSION_STATS_FILE());

						String timeElapsedStr = String.valueOf((System.nanoTime() - startTime)/1000);
						Files.write(Paths.get(file.getAbsolutePath()), 
								timeElapsedStr.getBytes(), StandardOpenOption.APPEND);

						for (int i = 0; i < sessionStatsHeaders.size(); i++) {
							String header = sessionStatsHeaders.get(i);

							if (header != null) {
								long val = ((SessionStatsAlert) alert).value(i);
								String valStr = "\t" + val;

								Files.write(Paths.get(file.getAbsolutePath()), 
										valStr.getBytes(), StandardOpenOption.APPEND);

							}
						}

						Files.write(Paths.get(file.getAbsolutePath()), 
								"\n".getBytes(), StandardOpenOption.APPEND);

					} else if (alert instanceof PeerLogAlert) {
						log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});


	}

	public void startSeedingLibraryVersion1() {

		Tools.dbInit();
		List<Library> library = LIBRARY.findAll();
		library.isEmpty();
		Tools.dbClose();

		// start sharing them
		Integer i = 0;

		// working at 7k
		while (i < library.size()) {
			log.info("File #" + i + "/" + library.size() + " songs in library");
			Library track = library.get(i++);
			TorrentHandle torrent = seedTorrent(track);

		}
		log.info("Done seeding library, total of " + session.getTorrents().size() + " torrents shared");
	}


	public void startSeedingLibraryVersion2() {

		Tools.dbInit();
		List<Library> library = LIBRARY.findAll();
		library.isEmpty();
		Tools.dbClose();

		// start sharing them
		Integer i = 0;
		// working at 7k
		while (i < 100 && !library.isEmpty() && i < library.size()) {
			log.info("File #" + i.toString() + "/" + library.size() + " songs in library");
			Library track = library.get(i);
			TorrentHandle torrent = seedTorrent(track);



			// Do increments of every x, but only after the x'th torrent announce was a success
			if (i % 1 == 0) {
				//				log.info("active torrents:" + session.getStatus().get)
				//				log.info("status : " + getActiveTorrents());
				//				torrent.setAutoManaged(false);
				//				torrent.resume();
				//				torrent.queuePositionTop();
				//				torrent.forceReannounce();
				//							torrent.forceRecheck();

				try {
					final CountDownLatch signal = new CountDownLatch(1);

					session.addListener(new TorrentAlertAdapter(torrent) {

						//						@Override
						//						public void addTorrent(AddTorrentAlert alert) {
						//							log.info("Add torrent received for torrent " + torrent.getName());
						//							signal.countDown();
						//						}
						//						

						@Override
						public void torrentChecked(TorrentCheckedAlert alert) {
							log.info("torrent checked received for torrent " + torrent.getName());
							//							signal.countDown();
						}

						@Override
						public void trackerReply(TrackerReplyAlert alert) {
							log.info("Tracked reply received for torrent " + torrent.getName());
							signal.countDown();
						}

						@Override
						public void peerConnect(PeerConnectAlert alert) {
							log.info("Peer connect alert received for torrent " + torrent.getName());
							signal.countDown();
						}



						@Override
						public void torrentError(TorrentErrorAlert alert) {
							log.info("Torrent error: couldn't add torrent " + torrent.getName());
							signal.countDown();
						}



					});


					signal.await(2, TimeUnit.MINUTES);
					//					session.removeTorrent(torrent);


					torrent.pause();
					//					session.pause()



				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			i++;

		}

		log.info("Done seeding library, total of " + session.getTorrents().size() + " torrents shared");

		//		session.resume();
		int j = 0;
		for (TorrentHandle t : session.getTorrents()) {

			log.info("Setting torrent# " + j++ +  " "  + t.getName() + " to resume" );
			//			t.setAutoManaged(true);
			//			t.flushCache();
			t.resume();

		}

		//		for (TorrentHandle t : infoHashToTorrentMap.values()) {
		//			log.info("Setting torrent# " + j++ +  " "  + t.getName() + " to adding" );
		//			
		//		}



	}


	private TorrentHandle seedTorrent(Library track) {
		String torrentPath = track.getString("torrent_path");
		String filePath = track.getString("file_path");


		File outputParent = new File(filePath).getParentFile();

		// Set the seed_mode flag
		TorrentHandle torrent = addTorrent(outputParent, new File(torrentPath), true);
		
		// Set up the scanInfo
		ScanInfo si = ScanInfo.create(new File(filePath));
		si.setStatus(ScanStatus.Seeding);
		si.setMbid(track.getString("mbid"));
		scanInfos.add(si);

		return torrent;
	}


	public TorrentHandle addTorrent(File outputParent, File torrentFile, Boolean seedMode) {



		// Check to see if saveResume data exists:
		File saveResumeData = constructSaveResumeFile(torrentFile);
		if (saveResumeData.exists()) {
			log.info("Save resume data found: " + saveResumeData.getAbsolutePath());
		}

		//		TorrentHandle torrent = (saveResumeData.exists()) ? session.addTorrent(torrentFile, outputParent, saveResumeData)
		//				: session.addTorrent(torrentFile, outputParent);

		// always set automanage to false
		add_torrent_params p = add_torrent_params.create_instance();
		TorrentInfo ti = new TorrentInfo(torrentFile);
		String savePath = outputParent.getAbsolutePath();
		p.setTi(ti.getSwig().copy());
		p.setSave_path(savePath);
		p.setStorage_mode(storage_mode_t.storage_mode_sparse);
	
		long flags = p.getFlags();

		//		log.info("flags = " + Long.toBinaryString(flags));
		// default flags = 10001001110000
		
		
		// Set seed mode
		if (seedMode) {
			flags += add_torrent_params.flags_t.flag_seed_mode.swigValue();
		}


		// Turn off automanage
		flags -= add_torrent_params.flags_t.flag_auto_managed.swigValue();

		// Turn on override resume data
		//		flags += add_torrent_params.flags_t.flag_override_resume_data.swigValue();

		if (saveResumeData.exists()) {
			byte[] data;
			try {
				data = Files.readAllBytes(Paths.get(saveResumeData.getAbsolutePath()));
				p.setResume_data(Vectors.bytes2char_vector(data));
				flags += add_torrent_params.flags_t.flag_use_resume_save_path.swigValue();
				log.info("flags = " + flags);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		//		log.info("flags final = " + Long.toBinaryString(flags));

		p.setFlags(flags);
		TorrentHandle torrent = new TorrentHandle(session.getSwig().add_torrent(p));


		String infoHash = torrent.getInfoHash().toString().toLowerCase();

		log.info("added torrent: " + torrent.getName() + 
				"\npath: " + torrentFile.getAbsolutePath() + 
				"\ninfo_hash: " + infoHash);

		torrent.replaceTrackers(DataSources.ANNOUNCE_ENTRIES());
		
		
		
		
		shareTorrent(torrent);
		addDefaultListeners(torrent);

		infoHashToTorrentMap.put(infoHash, torrent);


		return torrent;

	}

	private static File constructSaveResumeFile(File outputFile) {
		return new File(DataSources.TORRENTS_DIR() + "/srdata_" + outputFile.getName());
	}


	private void addDefaultListeners(TorrentHandle torrent) {



		// Add the listeners
		session.addListener(new TorrentAlertAdapter(torrent) {

			//			@Override
			//			public void stats(StatsAlert alert) {
			//				TorrentStats ts = TorrentStats.create(torrent);
			//				log.info(ts.toString());
			//
			//				super.stats(alert);
			//			}

			@Override
			public void stateChanged(StateChangedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void scrapeFailed(ScrapeFailedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}


			@Override
			public void blockFinished(BlockFinishedAlert alert) {
				log.info(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void torrentFinished(TorrentFinishedAlert alert) {
				log.info(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
				torrent.saveResumeData();
			}

			@Override
			public void blockDownloading(BlockDownloadingAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void peerConnect(PeerConnectAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}
			@Override
			public void peerSnubbed(PeerSnubbedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}

			@Override
			public void peerUnsnubbe(PeerUnsnubbedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void requestDropped(RequestDroppedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void saveResumeData(SaveResumeDataAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
				Entry srdata = alert.getResumeData();
				try {
					String srDataPath = DataSources.TORRENTS_DIR() + "/srdata_" + torrent.getName();
					Files.write(Paths.get(srDataPath), 
							srdata.bencode());
					log.info("Wrote save_resume_data: " + srDataPath);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}



			}


			@Override
			public void saveResumeDataFailed(SaveResumeDataFailedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void peerDisconnected(PeerDisconnectedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void peerBan(PeerBanAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}

			@Override
			public void peerError(PeerErrorAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}

			@Override
			public void addTorrent(AddTorrentAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void peerBlocked(PeerBlockedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}

			@Override
			public void trackerAnnounce(TrackerAnnounceAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}

			@Override
			public void trackerReply(TrackerReplyAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
				//				torrent.setAutoManaged(false);
				//				torrent.pause();
				//				torrent.saveResumeData();
			}

			@Override
			public void trackerWarning(TrackerWarningAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}

			@Override
			public void trackerError(TrackerErrorAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}


			@Override
			public void dhtReply(DhtReplyAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
				//				torrent.setAutoManaged(true);
			}

			@Override
			public void torrentPaused(TorrentPausedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

				torrent.saveResumeData();
				//				torrent.resume();
			}

			@Override
			public void torrentError(TorrentErrorAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void torrentResumed(TorrentResumedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void torrentUpdate(TorrentUpdateAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}


			@Override
			public void torrentChecked(TorrentCheckedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void torrentRemoved(TorrentRemovedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void torrentAdded(TorrentAddedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void torrentDeleted(TorrentDeletedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void torrentDeleteFailed(TorrentDeleteFailedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}


			@Override
			public void fastresumeRejected(FastresumeRejectedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void blockTimeout(BlockTimeoutAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void fileError(FileErrorAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void hashFailed(HashFailedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void performance(PerformanceAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void scrapeReply(ScrapeReplyAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void torrentPrioritize(TorrentPrioritizeAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void unwantedBlock(UnwantedBlockAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}







		});







	}

	private void shareTorrent(TorrentHandle torrent) {
		//		torrent.setAutoManaged(false);
		torrent.resume();
		//		
	}




	public Set<ScanInfo> getScanInfos() {
		return scanInfos;
	}

	public List<ScanInfo> getScanInfosLastForty() {
		int size = scanInfos.size();

		List<ScanInfo> subset = new ArrayList<ScanInfo>(scanInfos);

		if (size > 40) {
			subset =  subset.subList(size-40, size);
		}

		return Lists.reverse(subset);
	}


	public Session getSession() {
		return session;
	}

	public SettingsPack getSettings() {
		return settings;
	}

	public void updateSettings() {
		session.applySettings(settings);
		log.info("Libtorrent settings updated");
	}

	public Map<String, TorrentHandle> getInfoHashToTorrentMap() {
		return infoHashToTorrentMap;
	}

	private static List<Pair<String, Integer>> defaultRouters() {
		List<Pair<String, Integer>> list = new LinkedList<Pair<String, Integer>>();

		list.add(new Pair<String, Integer>("router.bittorrent.com", 6881));
		list.add(new Pair<String, Integer>("dht.transmissionbt.com", 6881));

		return list;
	}

	public String getUploadDownloadTotals() {
		long uploadPayloadBytes = session.getStats().uploadPayload();
		long downloadPayloadBytes = session.getStats().downloadPayload();

		StringBuilder s = new StringBuilder();

		s.append("Uploaded:   " + Tools.humanReadableByteCount(uploadPayloadBytes, true) + "\n");
		s.append("Downloaded: " + Tools.humanReadableByteCount(downloadPayloadBytes, true));

		return s.toString();

	}



}
