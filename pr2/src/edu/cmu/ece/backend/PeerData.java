package edu.cmu.ece.backend;

public class PeerData {

	private String ip;
	private int port;
	private int client;
	private int request;
	
	public PeerData(String ip, int port, int client, int request) {
		this.ip = ip;
		this.port = port;
		this.client = client;
		this.request = request;
	}
	
	public String getIP() {
		return ip;
	}
	
	public int getPort() {
		return port;
	}
	
	public int getClient() {
		return client;
	}

	public int getRequest() {
		return request;
	}

	@Override
	public boolean equals(Object obj){
		if (!(obj instanceof PeerData)){
			return false;
		}
		else {
			PeerData pd = (PeerData) obj;
			return pd.port == port && pd.ip.equals(ip) && pd.client == client
					&& pd.request == request;
		}
	}
	
	@Override
	public int hashCode(){
		return ip.hashCode() + port * client + request;
	}
}
