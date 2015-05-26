package com.torrenttunes.client;

import static com.torrenttunes.client.db.Tables.LIBRARY;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.torrenttunes.client.ScanDirectory.ScanInfo;
import com.torrenttunes.client.db.Tables.Library;
import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;

public class TorrentClient {
	static final Logger log = LoggerFactory.getLogger(TorrentClient.class);

	
	private List<Client> clients;
	private Set<ScanInfo> scanInfos;


	public static TorrentClient start() {
		return new TorrentClient();
	}
	
	private TorrentClient() {
		this.clients = new ArrayList<Client>();
		this.scanInfos = new LinkedHashSet<ScanInfo>();
		startSeedingLibrary();
	}
	
	
	private void startSeedingLibrary() {
		
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
		}
		
		log.info("Done seeding library, total of " + clients.size() + " torrents shared");
		
	}
	
	public void addTorrent(File outputParent, File torrentFile) {
		
		try {
			Client client = shareTorrent(outputParent, torrentFile);
			clients.add(client);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	private static Client shareTorrent(File outputParent, File torrentFile)
			throws UnknownHostException, IOException {
		
		Client client = new Client(InetAddress.getLocalHost(),
				SharedTorrent.fromFile(torrentFile, outputParent));
		
		// TODO eventually do this from a settings table
		client.setMaxDownloadRate(50.0);
		client.setMaxUploadRate(50.0);
		client.share();
		
		return client;
	}
	
	
	public List<Client> getClients() {
		return clients;
	}
	
	public Set<ScanInfo> getScanInfos() {
		return scanInfos;
	}
	
}
