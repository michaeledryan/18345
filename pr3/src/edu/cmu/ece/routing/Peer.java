package edu.cmu.ece.routing;

import java.util.UUID;

public class Peer implements Comparable<Peer> {
	private UUID uuid;
	private int distanceMetric; // -1 means infinite... no connection D:

	public Peer(UUID uuid, int metric) {
		this.uuid = uuid;
		this.distanceMetric = metric;
	}

	public UUID getUuid() {
		return uuid;
	}

	public int getDistanceMetric() {
		return distanceMetric;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public void setDistanceMetric(int distanceMetric) {
		this.distanceMetric = distanceMetric;
	}

	@Override
	public int compareTo(Peer o) {
		return uuid.compareTo(o.uuid);
	}
}