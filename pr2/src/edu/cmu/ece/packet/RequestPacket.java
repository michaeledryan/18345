package edu.cmu.ece.packet;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.cmu.ece.frontend.DCException;

/**
 * Parses an HTTP request. Creates a map of header fields to values.
 * 
 * @author Michaels
 * 
 */
public class RequestPacket {
	private int clientId;
	private String request;
	private Map<String, String> headers = new HashMap<String, String>();

	private String fullRangeString;
	private int[] lowerRanges;
	private int[] upperRanges;
	private int totalRangeLength;

	public RequestPacket(int id, BufferedReader packet) throws DCException,
			IOException {

		// First parse the header line to get the request
		clientId = id;
		String line = packet.readLine();

		// Get the request from the middle field
		if (line == null)
			throw new DCException();
		String[] tokens = line.split(" ");
		if (tokens.length < 3)
			throw new IOException("Received invalid header.");
		request = tokens[1];
		if (request.equals("/"))
			request = "index.html";

		// Loop through header, adding fields and values to map
		while (!(line = packet.readLine()).equals("")) {
			tokens = line.split(":");
			if (tokens.length >= 2)
				headers.put(tokens[0], tokens[1].trim());
		}
	}

	public String getRequest() {
		return request;
	}

	public String getHeader(String key) {
		return headers.get(key);
	}

	/* get the range query vales */
	public void parseRanges(File target) {
		if (this.getHeader("Range") == null) {
			lowerRanges = new int[1];
			upperRanges = new int[1];
			lowerRanges[0] = 0;
			upperRanges[0] = (int) target.length();
			totalRangeLength = (int) target.length();
			fullRangeString = lowerRanges[0] + "-" + upperRanges[0];
		} else {
			// Loop through each range and add to array
			String[] ranges = this.getHeader("Range").split("=")[1].split(",");
			lowerRanges = new int[ranges.length];
			upperRanges = new int[ranges.length];
			for (int i = 0; i < ranges.length; i++) {
				String[] range = ranges[i].split("-");
				// Get lower range
				if (range[0].equals(""))
					lowerRanges[i] = 0;
				else
					lowerRanges[i] = Integer.parseInt(range[0]);
				// Get upper range
				if (range.length > 1)
					upperRanges[i] = Integer.parseInt(range[1]);
				else
					upperRanges[i] = (int) target.length();

				// Update length
				totalRangeLength += upperRanges[i] - lowerRanges[i] + 1;

				// Update range string
				if (i != 0)
					fullRangeString += ",";
				fullRangeString += lowerRanges[i] + "-" + upperRanges[i];
			}
		}

	}

	public String getFullRangeString() {
		return fullRangeString;
	}

	public int[] getLowerRanges() {
		return lowerRanges;
	}

	public int[] getUpperRanges() {
		return upperRanges;
	}

	public int getTotalRangeLength() {
		return totalRangeLength;
	}
}
