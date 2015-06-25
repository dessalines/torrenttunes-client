package com.torrenttunes.client;

import static com.torrenttunes.client.db.Tables.LIBRARY;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frostwire.jlibtorrent.AlertListener;
import com.frostwire.jlibtorrent.LibTorrent;
import com.frostwire.jlibtorrent.Session;
import com.frostwire.jlibtorrent.SessionSettings;
import com.frostwire.jlibtorrent.SessionSettings.BandwidthMixedAlgo;
import com.frostwire.jlibtorrent.SessionSettings.ChokingAlgorithm;
import com.frostwire.jlibtorrent.TorrentAlertAdapter;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.BlockDownloadingAlert;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.PeerBanAlert;
import com.frostwire.jlibtorrent.alerts.PeerBlockedAlert;
import com.frostwire.jlibtorrent.alerts.PeerConnectAlert;
import com.frostwire.jlibtorrent.alerts.PeerDisconnectedAlert;
import com.frostwire.jlibtorrent.alerts.PeerErrorAlert;
import com.frostwire.jlibtorrent.alerts.PeerSnubbedAlert;
import com.frostwire.jlibtorrent.alerts.PeerUnsnubbedAlert;
import com.frostwire.jlibtorrent.alerts.StateChangedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TrackerAnnounceAlert;
import com.frostwire.jlibtorrent.alerts.TrackerErrorAlert;
import com.frostwire.jlibtorrent.alerts.TrackerReplyAlert;
import com.frostwire.jlibtorrent.alerts.TrackerWarningAlert;
import com.torrenttunes.client.db.Tables.Library;
import com.torrenttunes.client.tools.ScanDirectory.ScanInfo;
import com.torrenttunes.client.tools.ScanDirectory.ScanStatus;
import com.torrenttunes.client.tools.Tools;


public enum LibtorrentEngine  {

	INSTANCE;



	private final Logger log = LoggerFactory.getLogger(LibtorrentEngine.class);


	private Session session;
	private SessionSettings sessionSettings;
	private Map<String, TorrentHandle> infoHashToTorrentMap;
	
	
	private Set<ScanInfo> scanInfos;

	private LibtorrentEngine() {
		log.info("Starting up libtorrent with version: " + LibTorrent.version());

		session = new Session();
	
		sessionSettings = SessionSettings.newDefaults();
//		sessionSettings = SessionSettings.newMinMemoryUsage();
//		sessionSettings = SessionSettings.newHighPerformanceSeed();

//		sessionSettings.setTorrentConnectBoost(5);
//		sessionSettings.setMinReconnectTime(1);
		sessionSettings.setActiveDownloads(-1);
		sessionSettings.setActiveLimit(-1);
		sessionSettings.setActiveSeeds(-1);
		sessionSettings.setActiveDHTLimit(-1);
		
//		sessionSettings.setMaxPeerlistSize(0);
//		sessionSettings.setMaxPausedPeerlistSize(0);
//		sessionSettings.setChokingAlgorithm(ChokingAlgorithm.AUTO_EXPAND_CHOKER);
		sessionSettings.setCacheSize(999999);
	

		sessionSettings.setPeerConnectTimeout(35);
		
//		sessionSettings.allowMultipleConnectionsPerIp(true);
		sessionSettings.announceDoubleNAT(true);
		sessionSettings.setUploadRateLimit(0);
//		sessionSettings.setPeerTimeout(15);
//		sessionSettings.setInactivityTimeout(30);
		sessionSettings.setDownloadRateLimit(0);
		sessionSettings.setConnectionsLimit(100000);
		sessionSettings.setHalgOpenLimit(5);
		sessionSettings.setConnectionSpeed(3000);
	
		
		
//		sessionSettings.setAutoManageInterval(10);
//		sessionSettings.setAutoScrapeInterval(5);
//		sessionSettings.setMinAnnounceInterval(5);
//		sessionSettings.setActiveTrackerLimit(9999);
//		sessionSettings.setAnnounceToAllTrackers(false);
//		sessionSettings.setDHTAnnounceInterval(5);
//		sessionSettings.setMaxAllowedInRequestQueue(9999);
//		sessionSettings.setUnchokeSlotsLimit(800);
//		sessionSettings.setCacheExpiry(9999);
//		sessionSettings.setMixedModeAlgorithm(BandwidthMixedAlgo.PREFER_TCP);

		
		
		
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
                log.debug(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
				
			}
		});
		


	}


	public void startSeedingLibrary() {

		Tools.dbInit();
		List<Library> library = LIBRARY.findAll();
		library.isEmpty();
		Tools.dbClose();

		// start sharing them
		for (Library track : library) {
			String torrentPath = track.getString("torrent_path");
			String filePath = track.getString("file_path");

			File outputParent = new File(filePath).getParentFile();
			
			
			addTorrent(outputParent, new File(torrentPath));

			// Set up the scanInfo
			ScanInfo si = ScanInfo.create(new File(filePath));
			si.setStatus(ScanStatus.Seeding);
			si.setMbid(track.getString("mbid"));
			scanInfos.add(si);
		}

		log.info("Done seeding library, total of " + session.getTorrents().size() + " torrents shared");


	}

	public TorrentHandle addTorrent(File outputParent, File torrentFile) {
		TorrentHandle torrent = session.addTorrent(torrentFile, outputParent);
//		torrent.setAutoManaged(true);
//		torrent.queuePositionTop();
		log.info("added torrent " + torrent.getName());

		shareTorrent(torrent);
		infoHashToTorrentMap.put(torrent.getInfoHash().toString().toLowerCase(), torrent);

		return torrent;



	}


	private void shareTorrent(TorrentHandle torrent) {

	


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
				log.info(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}
			
			@Override
			public void blockFinished(BlockFinishedAlert alert) {
				log.info(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}
			@Override
			public void torrentFinished(TorrentFinishedAlert alert) {
								log.info(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

				
			}
			@Override
			public void blockDownloading(BlockDownloadingAlert alert) {
				log.info(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}
			
			@Override
			public void peerConnect(PeerConnectAlert alert) {
				log.info(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}
			@Override
			public void peerSnubbed(PeerSnubbedAlert alert) {
				log.info(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}
			
			@Override
			public void peerUnsnubbe(PeerUnsnubbedAlert alert) {
				log.info(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}
			
			@Override
			public void peerDisconnected(PeerDisconnectedAlert alert) {
				log.info(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
			}
			
			@Override
			public void peerBan(PeerBanAlert alert) {
				log.info(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}
			
			@Override
			public void peerError(PeerErrorAlert alert) {
				log.info(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}
			
			@Override
			public void peerBlocked(PeerBlockedAlert alert) {
				log.info(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}
			
			@Override
			public void trackerAnnounce(TrackerAnnounceAlert alert) {
				log.info(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}
			
			@Override
			public void trackerReply(TrackerReplyAlert alert) {
				log.info(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}
			
			@Override
			public void trackerWarning(TrackerWarningAlert alert) {
				log.info(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}
			
			@Override
			public void trackerError(TrackerErrorAlert alert) {
				log.info(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());

			}
			

			


		});

		torrent.resume();


		
		
		

		
	}




	public Set<ScanInfo> getScanInfos() {
		return scanInfos;
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
