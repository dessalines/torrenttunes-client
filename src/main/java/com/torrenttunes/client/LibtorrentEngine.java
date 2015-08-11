package com.torrenttunes.client;

import static com.torrenttunes.client.db.Tables.LIBRARY;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frostwire.jlibtorrent.AlertListener;
import com.frostwire.jlibtorrent.DHT;
import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.LibTorrent;
import com.frostwire.jlibtorrent.Session;
import com.frostwire.jlibtorrent.SettingsPack;
import com.frostwire.jlibtorrent.StatsMetric;
import com.frostwire.jlibtorrent.TorrentAlertAdapter;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.alerts.AbstractAlert;
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
import com.frostwire.jlibtorrent.swig.default_storage;
import com.frostwire.jlibtorrent.swig.session_stats_alert;
import com.frostwire.jlibtorrent.swig.settings_pack.bool_types;
import com.frostwire.jlibtorrent.swig.settings_pack.int_types;
import com.frostwire.jlibtorrent.swig.settings_pack.string_types;
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
	private SettingsPack sessionSettings;
	private Map<String, TorrentHandle> infoHashToTorrentMap;
	private List<TorrentHandle> torrents = new ArrayList<TorrentHandle>();

	private Set<ScanInfo> scanInfos;

	private List<String> sessionStatsHeaders;

	private LibtorrentEngine() {

		System.load(DataSources.LIBTORRENT_OS_LIBRARY_PATH());

		// Create a session stats file with headers
		createSessionStatsFile();


		System.out.println("java library path: " + System.getProperty("java.library.path"));

//		default_storage.disk_write_access_log(true);

		log.info("Starting up libtorrent with version: " + LibTorrent.version());


		session = new Session();
		sessionSettings = new SettingsPack();

		sessionSettings.setActiveDownloads(10);
		sessionSettings.setActiveSeeds(999999);
		sessionSettings.setInteger(int_types.active_limit.swigValue(), 999999);
		sessionSettings.setInteger(int_types.active_tracker_limit.swigValue(), 999999);

		sessionSettings.setUploadRateLimit(0);
		sessionSettings.setDownloadRateLimit(0);
		
		sessionSettings.setBoolean(bool_types.announce_double_nat.swigValue(), true);
		sessionSettings.setInteger(int_types.peer_connect_timeout.swigValue(), 60);
		
		sessionSettings.setInteger(int_types.file_pool_size.swigValue(), 200000);
		
		sessionSettings.setInteger(int_types.tracker_completion_timeout.swigValue(), 10);
		sessionSettings.setBoolean(bool_types.incoming_starts_queued_torrents.swigValue(), true);
		
		sessionSettings.setInteger(int_types.peer_timeout.swigValue(), 20);
		
		

		DHT dht = new DHT(session);
		dht.stop();

		sessionSettings.broadcastLSD(false);
		sessionSettings.setMaxPeerlistSize(500);
		sessionSettings.setInteger(int_types.min_announce_interval.swigValue(), 1740);

		
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
	



		
		session.applySettings(sessionSettings);


		this.scanInfos = new LinkedHashSet<ScanInfo>();
		this.infoHashToTorrentMap = new ConcurrentHashMap<String, TorrentHandle>();


		addDefaultSessionAlerts();



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


			// Create the headers using set
			for (int i = 0; i < ssm.length; i++) {
				StatsMetric sm = ssm[i];
				//				log.info("i = " + i + " valueindex = " + sm.valueIndex + " name = " + sm.name);
				sessionStatsHeaders.set(sm.valueIndex, sm.name);
			}


			for (String header : sessionStatsHeaders) {

				//				log.info(header);

				if (header != null) {
					String headerStr = header + "\t";
					Files.write(Paths.get(file.getAbsolutePath()), 
							headerStr.getBytes(), StandardOpenOption.APPEND);
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
				return new int[]{AlertType.SESSION_STATS.getSwig()};
			}

			@Override
			public void alert(Alert<?> alert) {
				try {
					if (alert instanceof SessionStatsAlert) {
						log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

						File file = new File(DataSources.SESSION_STATS_FILE());

						for (int i = 0; i < sessionStatsHeaders.size(); i++) {
							String header = sessionStatsHeaders.get(i);

							if (header != null) {
								long val = ((SessionStatsAlert) alert).value(i);
								String valStr = val + "\t";

								Files.write(Paths.get(file.getAbsolutePath()), 
										valStr.getBytes(), StandardOpenOption.APPEND);



							}
						}

						Files.write(Paths.get(file.getAbsolutePath()), 
								"\n".getBytes(), StandardOpenOption.APPEND);

					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});


	}


	public void startSeedingLibrary() {

		Tools.dbInit();
		List<Library> library = LIBRARY.findAll();
		library.isEmpty();
		Tools.dbClose();

		// start sharing them
		Integer i = 0;
		// working at 7k
		while (i < 100) {
			log.info("File #" + i.toString() + "/" + library.size() + " songs in library");
			Library track = library.get(i);
			String torrentPath = track.getString("torrent_path");
			String filePath = track.getString("file_path");


			File outputParent = new File(filePath).getParentFile();

			TorrentHandle torrent = addTorrent(outputParent, new File(torrentPath));

			torrents.add(torrent);


			// Set up the scanInfo
			ScanInfo si = ScanInfo.create(new File(filePath));
			si.setStatus(ScanStatus.Seeding);
			si.setMbid(track.getString("mbid"));
			scanInfos.add(si);



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
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			i++;

		}

		log.info("Done seeding library, total of " + session.getTorrents().size() + " torrents shared");

		//		session.resume();
		int j = 0;
		for (TorrentHandle t : session.getTorrents()) {

			log.info("Setting torrent# " + j++ +  " "  + t.getName() + " to automanage" );
			//			t.setAutoManaged(true);
			//			t.flushCache();
			t.resume();

		}

		//		for (TorrentHandle t : infoHashToTorrentMap.values()) {
		//			log.info("Setting torrent# " + j++ +  " "  + t.getName() + " to adding" );
		//			
		//		}



	}

	public String getActiveTorrents() {
		try {
			return Tools.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(session.getStatus());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

	public TorrentHandle addTorrent(File outputParent, File torrentFile) {



		// Check to see if saveResume data exists:
		File saveResumeData = constructSaveResumeFile(torrentFile);
		if (saveResumeData.exists()) {
			log.info("Save resume data found: " + saveResumeData.getAbsolutePath());
		}

		TorrentHandle torrent = (saveResumeData.exists()) ? session.addTorrent(torrentFile, outputParent, saveResumeData)
				: session.addTorrent(torrentFile, outputParent);


		log.info("added torrent: " + torrent.getName() + " , path: " + torrentFile.getAbsolutePath());

		shareTorrent(torrent);
		addDefaultListeners(torrent);

		infoHashToTorrentMap.put(torrent.getInfoHash().toString().toLowerCase(), torrent);


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
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}

			@Override
			public void torrentFinished(TorrentFinishedAlert alert) {
				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
				//				torrent.saveResumeData();
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
		torrent.setAutoManaged(false);
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

	public SettingsPack getSessionSettings() {
		return sessionSettings;
	}

	public void updateSettings() {
		session.applySettings(sessionSettings);
		log.info("Libtorrent settings updated");
	}

	public Map<String, TorrentHandle> getInfoHashToTorrentMap() {
		return infoHashToTorrentMap;
	}



}
