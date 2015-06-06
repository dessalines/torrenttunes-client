package com.torrenttunes.client;

import static com.torrenttunes.client.db.Tables.LIBRARY;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frostwire.jlibtorrent.LibTorrent;
import com.frostwire.jlibtorrent.Session;
import com.frostwire.jlibtorrent.TorrentAlertAdapter;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.alerts.StatsAlert;
import com.torrenttunes.client.ScanDirectory.ScanInfo;
import com.torrenttunes.client.ScanDirectory.ScanStatus;
import com.torrenttunes.client.db.Tables.Library;


public enum LibtorrentEngine  {

	INSTANCE;



	private final Logger log = LoggerFactory.getLogger(LibtorrentEngine.class);


	private Session session;
	private Set<ScanInfo> scanInfos;

	private LibtorrentEngine() {
		log.info("Starting up libtorrent with version: " + LibTorrent.version());

		session = new Session();
		this.scanInfos = new LinkedHashSet<ScanInfo>();



//		session.addListener(new AlertListener() {
//
//			@Override
//			public int[] types() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public void alert(Alert<?> alert) {
//				System.out.println(alert.getType() + " - " + alert.getSwig().what() + " - " + alert.getSwig().message());
//			}
//		});

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

		shareTorrent(torrent);

		return torrent;



	}


	private void shareTorrent(TorrentHandle torrent) {



		// Add the listeners
		session.addListener(new TorrentAlertAdapter(torrent) {

			@Override
			public void stats(StatsAlert alert) {
//				TorrentStats ts = TorrentStats.create(torrent);
//				log.info(ts.toString());

				super.stats(alert);
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




}
