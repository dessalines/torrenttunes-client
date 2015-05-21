package com.torrenttunes.client;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;

@Deprecated
public class TorrentClientService extends AbstractScheduledService {

	@Override
	protected void startUp() {


		try {
			Client client = new Client( InetAddress.getLocalHost(),
					SharedTorrent.fromFile(
							new File("/path/to/your.torrent"),
							new File("/path/to/output/directory")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		

	}


	@Override
	protected void runOneIteration() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	protected Scheduler scheduler() {
		// TODO Auto-generated method stub
		return null;
	}


}
