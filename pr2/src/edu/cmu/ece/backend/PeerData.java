package edu.cmu.ece.backend;

public class PeerData {

	private String ip;
	private int port;
	private int rate;
	
	public PeerData(String ip, int port, int rate) {
		this.ip = ip;
		this.port = port;
		this.rate = rate;
	}
	
	
	public int getRate() {
		return rate;
	}
	
	public int getPort() {
		return port;
	}
	
	public String getIP() {
		return ip;
	}
}
