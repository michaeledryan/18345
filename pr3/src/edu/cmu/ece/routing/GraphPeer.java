package edu.cmu.ece.routing;

import java.util.UUID;

/**
 * Stores UUID And bitrate data for a given file.
 * 
 * @author michaels
 *
 */
public class GraphPeer {

	private UUID uuid;
	private int bitrate;
	
	public GraphPeer(UUID u, int kbps) {
		setUuid(u);
		setBitrate(kbps << 10);
	}

	public int getBitrate() {
		return bitrate;
	}

	public void setBitrate(int bitrate) {
		this.bitrate = bitrate;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	
	
	
}
