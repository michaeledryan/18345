package edu.cmu.ece.frontend;

import java.util.Map;
import java.util.UUID;

public class NameAndNeighbors {

	private String name;
	private Map<UUID, Integer> neighbors;

	public NameAndNeighbors(String name, Map<UUID, Integer> map) {
		this.setNeighbors(map);
		this.setName(name);
	}

	public Map<UUID, Integer> getNeighbors() {
		return neighbors;
	}

	public void setNeighbors(Map<UUID, Integer> neighbors) {
		this.neighbors = neighbors;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
