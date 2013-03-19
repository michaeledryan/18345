package edu.cmu.ece.packet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;

public class ResponseFileData {
	private static int BUF_MAX = 1 << 10;
	private File target;
	private HTTPRequestPacket request;
	int[] lowers;
	int[] uppers;

	public ResponseFileData(File in_target, HTTPRequestPacket in_request) {
		target = in_target;
		request = in_request;

		// Determine ranges
		request.parseRanges(target);
		lowers = request.getLowerRanges();
		uppers = request.getUpperRanges();
	}

	/*
	 * Given a maximum packet size, returns the number of packets it would take
	 * to send this file
	 */
	public int getNumPackets(int packetSize) {
		int result = 0;
		for(int i = 0; i < lowers.length; i++){
			int segmentSize = uppers[i]-lowers[i]+1;
			result += (int) Math.ceil(((float) segmentSize) / packetSize);
		}
		return result;
	}

	/*
	 * Given an output stream, a packet number and a packet size, gets the
	 * packetNum-th packet that is at most packetSize big, and writes that to
	 * the specified output stream.
	 */
	public void getPacketData(OutputStream out, int packetNum, int packetSize) {
		// Determine which segment this packet is in
		int segment = 0;
		for (int i = 0; i < lowers.length; i++) {
			segment = i;
			int segmentSize = uppers[i] - lowers[i] + 1;
			if (packetNum > segmentSize) {
				packetNum -= segmentSize;
			} else {
				break;
			}
		}

		// Determine the file range
		int lower = lowers[segment] + packetSize * packetNum;
		int length = packetSize;
		if (lower + packetSize > uppers[segment])
			length = uppers[segment] - lower + 1;

		// Open the file to the desired location
		FileInputStream file = null;
		try {
			file = new FileInputStream(target);
			file.skip(lower);
		} catch (FileNotFoundException e1) {
			System.out.println("Couldn't find file. Should have 404'd.");
		} catch (IOException e) {
			System.out.println("Could not read/write file: "
					+ e.getMessage());
		}

		// Write the file to the out buffer
		byte[] buffer = new byte[length];
		try {
			file.read(buffer, 0, length);
			out.write(buffer, 0, length);
		} catch (FileNotFoundException e) {
			System.out.println("Couldn't find file. Should have 404'd.");
		} catch (IndexOutOfBoundsException e) {
			System.out.println("Failed to copy to buffer: " + e.getMessage());
		} catch (SocketException e) {
			System.out.println("Failed to write to socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("Could not read/write file: " + e.getMessage());
		}

		// Close file
		try {
			file.close();
		} catch (IOException e) {
			System.out.println("Could not close file: "
					+ e.getMessage());
		}
	}

	/*
	 * Given an OutputStream for a socket, writes the entire file to that socket
	 * using a maximum buffer size.
	 */
	public void sendFileToStream(OutputStream out) {
		int numPackets = getNumPackets(BUF_MAX);
		for (int i = 0; i < numPackets; i++) {
			getPacketData(out, i, BUF_MAX);
		}
	}
}
