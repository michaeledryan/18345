package edu.cmu.ece.packet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;

public class ResponseFileData {
	private static int BUF_MAX = 1 << 10;

	public static void sendFile(File target, HTTPRequestPacket request,
			OutputStream out) {
		// Store ranges to send as array of lower/upper bounds
		request.parseRanges(target);
		int[] lowers = request.getLowerRanges();
		int[] uppers = request.getUpperRanges();

		// Write content to the client. Break up the content to avoid OOMing
		// over large loads.
		for (int i = 0; i < lowers.length; i++) {
			int length = uppers[i] - lowers[i] + 1;
			int currentLength = (length < BUF_MAX) ? length : BUF_MAX;
			int bytesRead = 0;
			byte[] buffer = new byte[currentLength];

			FileInputStream file = null;
			try {
				file = new FileInputStream(target);
				file.skip(lowers[i]);
			} catch (FileNotFoundException e1) {
				System.out.println("Couldn't find file. Should have 404'd.");
			} catch (IOException e) {
				System.out.println("Could not read/write file: "
						+ e.getMessage());
			}

			// Read from file, write to client. Smallish buffer size keeps
			// memory
			// use reasonable.
			while (bytesRead < length) {
				try {
					file.read(buffer, 0, currentLength);
					out.write(buffer, 0, currentLength);
					bytesRead += currentLength;
					if (length - bytesRead < currentLength)
						currentLength = length - bytesRead;
				} catch (FileNotFoundException e) {
					System.out
							.println("Couldn't find file. Should have 404'd.");
				} catch (IndexOutOfBoundsException e) {
					System.out.println("Failed to copy to buffer: "
							+ e.getMessage());
				} catch (SocketException e) {
					System.out.println("Failed to write to socket: "
							+ e.getMessage());
				} catch (IOException e) {
					System.out.println("Could not read/write file: "
							+ e.getMessage());
				}
			}

			// Close file and output stream.
			try {
				out.flush();
				file.close();
			} catch (IOException e) {
				System.out.println("Could not read/write file: "
						+ e.getMessage());
			}
		}
	}
}
