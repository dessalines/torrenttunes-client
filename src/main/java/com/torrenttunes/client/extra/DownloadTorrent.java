package com.torrenttunes.client.extra;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import com.frostwire.jlibtorrent.LibTorrent;
import com.frostwire.jlibtorrent.Session;
import com.frostwire.jlibtorrent.TorrentAlertAdapter;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.alerts.BlockDownloadingAlert;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.StateChangedAlert;
import com.frostwire.jlibtorrent.alerts.StatsAlert;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.torrenttunes.client.tools.DataSources;
import com.torrenttunes.client.tools.Tools;


public final class DownloadTorrent {

	public static void main(String[] args) throws Throwable {

		File torrentFile = DataSources.SAMPLE_TORRENT;

		

		final Session s = new Session();

		final TorrentHandle th = s.addTorrent(torrentFile, torrentFile.getParentFile());

		
		

//		s.startDHT();
//		s.startLSD();
//		s.startNATPMP();
//		s.startUPnP();


		s.addListener(new TorrentAlertAdapter(th) {


			@Override
			public void blockFinished(BlockFinishedAlert alert) {


			}

			@Override
			public void torrentFinished(TorrentFinishedAlert alert) {
				System.out.println("Torrent finished");

			}
			@Override
			public void blockDownloading(BlockDownloadingAlert alert) {
				super.blockDownloading(alert);
			}
			
			@Override
			public void stateChanged(StateChangedAlert alert) {
				System.out.println(th.getStatus().getState().toString());
				super.stateChanged(alert);
			}
			
			@Override
			public void stats(StatsAlert alert) {
				String progress = Tools.NUMBER_FORMAT.format(th.getStatus().getProgressPpm()*1E-4) + "%";
				System.out.println("Progress: " + progress);
				
				String downloadSpeed = Tools.humanReadableByteCount(th.getStatus().getDownloadRate(), true)+ "/s";
				String uploadSpeed = Tools.humanReadableByteCount(th.getStatus().getUploadRate(), true)+ "/s";

				System.out.println("Download Speed: " + downloadSpeed);
				System.out.println("Upload Speed: " + uploadSpeed);

				System.out.println("Peers: " + th.getStatus().getNumConnections() + "/" + 
						th.getStatus().getConnectCandidates());
				
				super.stats(alert);
			}
			
			
			
			
			
		});

		th.resume();

	
	}
}