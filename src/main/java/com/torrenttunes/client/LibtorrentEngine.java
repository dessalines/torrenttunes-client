package com.torrenttunes.client;

import static com.torrenttunes.client.db.Tables.LIBRARY;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frostwire.jlibtorrent.Address;
import com.frostwire.jlibtorrent.AlertListener;
import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.LibTorrent;
import com.frostwire.jlibtorrent.Session;
import com.frostwire.jlibtorrent.SessionSettings;
import com.frostwire.jlibtorrent.SessionSettings.BandwidthMixedAlgo;
import com.frostwire.jlibtorrent.SessionSettings.ChokingAlgorithm;
import com.frostwire.jlibtorrent.SessionSettings.DiskCacheAlgo;
import com.frostwire.jlibtorrent.SessionSettings.SuggestMode;
import com.frostwire.jlibtorrent.TorrentAlertAdapter;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import com.frostwire.jlibtorrent.alerts.BlockDownloadingAlert;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.BlockTimeoutAlert;
import com.frostwire.jlibtorrent.alerts.DhtAnnounceAlert;
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
import com.frostwire.jlibtorrent.alerts.ScrapeReplyAlert;
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
import com.frostwire.jlibtorrent.swig.address;
import com.frostwire.jlibtorrent.swig.ip_filter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
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
	private SessionSettings sessionSettings;
	private Map<String, TorrentHandle> infoHashToTorrentMap;
	private List<TorrentHandle> torrents = new ArrayList<TorrentHandle>();

	private Set<ScanInfo> scanInfos;

	private LibtorrentEngine() {
		log.info("Starting up libtorrent with version: " + LibTorrent.version());

		session = new Session();
		


		sessionSettings = SessionSettings.newDefaults();
		//		sessionSettings = SessionSettings.newMinMemoryUsage();
		//		sessionSettings = SessionSettings.newHighPerformanceSeed();

		//				sessionSettings.setTorrentConnectBoost(5);
		//		sessionSettings.setMinReconnectTime(1);
		session.stopDHT();

		//		sessionSettings.setActiveDownloads(10);
		sessionSettings.setActiveLimit(-1);
		sessionSettings.setActiveSeeds(-1);


		//				sessionSettings.setActiveDHTLimit(5);
		//				sessionSettings.setActiveTrackerLimit(10);

		sessionSettings.setUploadRateLimit(0);
		sessionSettings.setDownloadRateLimit(0);

		// These worked great!
		//		sessionSettings.setMixedModeAlgorithm(BandwidthMixedAlgo.);
		session.stopLSD();
		//		session.stopDHT();
		sessionSettings.announceDoubleNAT(true);
		sessionSettings.setPeerConnectTimeout(60);

		sessionSettings.useReadCache(false);
		//		sessionSettings.setMaxPeerlistSize(500);
		//		sessionSettings.setMaxPeerlistSize(20);
		sessionSettings.setHalgOpenLimit(5);



//		//		sessionSettings.setDHTAnnounceInterval(3600);
//				sessionSettings.setMinAnnounceInterval(3600);

		//		sessionSettings.setLocalServiceAnnounceInterval(3600);


		//		sessionSettings.setNoConnectPrivilegedPorts(true);

		sessionSettings.setTrackerBackoff(10);
		

		//		sessionSettings.setAutoManageInterval(600);
		//		sessionSettings.setRateLimitIPOverhead(true);
		//		sessionSettings.setFreeTorrentHashes(true);
		//		sessionSettings.setFileChecksDelayPerBlock(1000);
		//		sessionSettings.setSuggestMode(SuggestMode.SUGGEST_READ_CACHE);
		sessionSettings.setFilePoolSize(200000);
		//		sessionSettings.setOptimizeHashingForSpeed(false);
		//		sessionSettings.setOptimisticDiskRetry(5);
		//		sessionSettings.setDiskCacheAlgorithm(DiskCacheAlgo.LRU);
		//		sessionSettings.setIncomingStartsQueuedTorrents(true);
		//		sessionSettings.setTrackerReceiveTimeout(1);
		//		sessionSettings.setTrackerCompletionTimeout(1);
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

		session.setSettings(sessionSettings);


		log.info("active seed limit: " + String.valueOf(session.getSettings().getActiveLimit()));


		this.scanInfos = new LinkedHashSet<ScanInfo>();
		this.infoHashToTorrentMap = new HashMap<String, TorrentHandle>();


		session.addListener(new AlertListener() {

			@Override
			public int[] types() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void alert(Alert<?> alert) {
				//				log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());


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
		while (i < library.size()) {
			log.info("File #" + i.toString() + "/" + library.size() + " songs in library");
			Library track = library.get(i);
			String torrentPath = track.getString("torrent_path");
			String filePath = track.getString("file_path");

			File outputFile = new File(filePath);

			TorrentHandle torrent = addTorrent(outputFile, new File(torrentPath));

			torrents.add(torrent);
			addDefaultListeners(torrent);


			


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
				torrent.queuePositionTop();
				//				torrent.forceReannounce();
				//							torrent.forceRecheck();

				try {
					final CountDownLatch signal = new CountDownLatch(1);

					session.addListener(new TorrentAlertAdapter(torrent) {

						

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

//						@Override
//						public void peerDisconnected(PeerDisconnectedAlert alert) {
//							log.info("Peer disconnect alert received for torrent " + torrent.getName());
//							signal.countDown();
//						}
						
						@Override
						public void torrentError(TorrentErrorAlert alert) {
							log.info("Torrent error: couldn't add torrent " + torrent.getName());
							signal.countDown();
						}



					});

					
					signal.await(2, TimeUnit.MINUTES);
//					session.removeTorrent(torrent);
					
					// pause the last 50
//					for (int k = i - 50; k < i; k++) {
//						log.info("k = " + k);
//						torrents.get(k).pause();
//					}
					torrent.pause();
//					session.pause();
					
					

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

	public TorrentHandle addTorrent(File outputFile, File torrentFile) {

		File outputParent = outputFile.getParentFile();

		// Check to see if saveResume data exists:
		File saveResumeData = constructSaveResumeFile(outputFile);
		if (saveResumeData.exists()) {
			log.info("Save resume data found: " + saveResumeData.getAbsolutePath());
		}

		TorrentHandle torrent = (saveResumeData.exists()) ? session.addTorrent(torrentFile, outputParent, saveResumeData)
				: session.addTorrent(torrentFile, outputParent);


		log.info("added torrent: " + torrent.getName() + " , path: " + torrentFile.getAbsolutePath() + 
				" , output file: " + outputFile.getAbsolutePath());

		shareTorrent(torrent);
		
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

	public SessionSettings getSessionSettings() {
		return sessionSettings;
	}

	public void updateSettings() {
		session.setSettings(sessionSettings);
		log.info("Libtorrent settings updated");
	}

	public Map<String, TorrentHandle> getInfoHashToTorrentMap() {
		return infoHashToTorrentMap;
	}



}
