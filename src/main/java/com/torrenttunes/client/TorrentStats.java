package com.torrenttunes.client;

import com.frostwire.jlibtorrent.TorrentHandle;

public class TorrentStats {
	private TorrentHandle torrent;
	
	private String downloadSpeed, uploadSpeed, progress, peers, state, name;
	
	public static TorrentStats create(TorrentHandle torrent) {
		return new TorrentStats(torrent);
	}
	
	private TorrentStats(TorrentHandle torrent) {
		this.torrent = torrent;
		this.downloadSpeed = Tools.humanReadableByteCount(torrent.getStatus().getDownloadRate(), true)+ "/s";
		this.uploadSpeed = Tools.humanReadableByteCount(torrent.getStatus().getUploadRate(), true)+ "/s";
		this.progress = Tools.NUMBER_FORMAT.format(torrent.getStatus().getProgressPpm()*1E-4) + "%";
		this.peers = torrent.getStatus().getNumConnections() + "/" + 
				torrent.getStatus().getConnectCandidates();
		this.state = torrent.getStatus().getState().toString();
		this.name = torrent.getName();
	}
	
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("\nName: " + name);
		s.append("\nProgress: " + progress);
		s.append("\nDownload Speed: " + downloadSpeed);
		s.append("\nUpload Speed: " + uploadSpeed);
		s.append("\nPeers: " + peers);
		s.append("\nState: " + state);

		
		return s.toString();
		
	}

	public TorrentHandle getTorrent() {
		return torrent;
	}

	public String getDownloadSpeed() {
		return downloadSpeed;
	}

	public String getUploadSpeed() {
		return uploadSpeed;
	}

	public String getProgress() {
		return progress;
	}

	public String getPeers() {
		return peers;
	}
	
	public String getState() {
		return state;
	}

	public String getName() {
		return name;
	}
	

	
	
}