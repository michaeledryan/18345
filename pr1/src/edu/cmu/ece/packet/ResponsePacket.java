package edu.cmu.ece.packet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.Date;

/**
 * Generates a response packet to be sent back to the client.
 * 
 * @author Michaels
 * 
 */
public class ResponsePacket {
	private RequestPacket request;
	private OutputStream out;
	private PrintWriter textOut;
	private File target;

	private int clientId;
	private String header;
	private static int BUF_MAX = 1 << 10;

	/**
	 * Constructor. Sets necessary fields.
	 * 
	 * @param id
	 * @param request
	 * @param output
	 * @param textoutput
	 */
	public ResponsePacket(int id, RequestPacket request, OutputStream output,
			PrintWriter textoutput) {
		this.clientId = id;
		this.request = request;
		this.out = output;
		this.textOut = textoutput;
	}

	/**
	 * Sends a response back to the client, returning whether or not the
	 * resposne was successful.
	 * 
	 * @returns whether or not a valid response is sent.
	 */
	public Boolean sendResponse() {
		String filename = "content/" + request.getRequest();
		target = new File(filename);
		if (!target.exists()) {
			this.send404();
			return true;
		}

		String[] range;
		int lowerLimit = 0;
		int length = (int) target.length();

		// Send either 200 or 206 response
		if (request.getHeader("Range") == null) {
			header = "HTTP/1.1 200 OK\r\n";
		} else {
			header = "HTTP/1.1 206 Partial Content\r\n";
			range = request.getHeader("Range").split("=")[1].split("-");
			lowerLimit = Integer.parseInt(range[0]);
			if (range.length > 1)
				length = Integer.parseInt(range[1]) - lowerLimit + 1;
			else
				length -= lowerLimit;
		}

		// Generate and write headers to client.
		this.makeHeaders(lowerLimit, length, (int) target.length());
		textOut.write(header);
		textOut.flush();

		// Write content to the client. Break up the content to avoid OOMing
		// over large loads.

		int currentLength = (length < BUF_MAX) ? length : BUF_MAX;
		int bytesRead = 0;
		byte[] buffer = new byte[currentLength];

		FileInputStream file = null;
		try {
			file = new FileInputStream(target);
			file.skip(lowerLimit);
		} catch (FileNotFoundException e1) {
			System.out.println("Couldn't find file. Should have 404'd.");
		} catch (IOException e) {
			System.out.println("Could not read/write file: " + e.getMessage());
		}

		// Read from file, write to client. Smallish buffer size keeps memory
		// use reasonable.
		while (bytesRead < length) {
			try {
				file.read(buffer, 0, currentLength);
				out.write(buffer, 0, currentLength);
				bytesRead += currentLength;
				if (length - bytesRead < currentLength)
					currentLength = length - bytesRead;
			} catch (FileNotFoundException e) {
				System.out.println("Couldn't find file. Should have 404'd.");
			} catch (IndexOutOfBoundsException e) {
				System.out.println("Failed to copy to buffer: "
						+ e.getMessage());
			} catch (SocketException e) {
				System.out.println("Failed to write to socket: "
						+ e.getMessage());
				return false;
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
			System.out.println("Could not read/write file: " + e.getMessage());
		}

		return true;
	}

	/**
	 * Sends a 404 to the client.
	 */
	private void send404() {
		header = "HTTP/1.1 404 Not Found\r\n";
		String connection = request.getHeader("Connection");
		if (connection != null && connection.equalsIgnoreCase("close"))
			header += "Connection: Close\r\n";
		else
			header += "Connection: Keep-Alive\r\n";
		header += "Date: " + new Date().toString() + "\r\n";

		textOut.write(header);
		textOut.flush();
	}

	/**
	 * Returns the valid html content type when given a filename.
	 * 
	 * @param filename
	 *            the filename to be parsed
	 * @return the html content type
	 */
	private String getContentType(String filename) {
		String ext = filename.substring(filename.lastIndexOf(".") + 1);
		String type = "application/octet-stream";

		// Parse out required types
		if (ext.equals("txt"))
			type = "text/plain";
		else if (ext.equals("css"))
			type = "text/css";
		else if (ext.equals("htm") || ext.equals("html"))
			type = "text/html";
		else if (ext.equals("jpg") || ext.equals("jpeg"))
			type = "image/jpeg";
		else if (ext.equals("gif"))
			type = "image/gif";
		else if (ext.equals("png"))
			type = "image/png";
		else if (ext.equals("js"))
			type = "application/js";
		else if (ext.equals("webm"))
			type = "video/webm";
		else if (ext.equals("ogg") || ext.equals("ogv"))
			type = "video/ogg";
		else if (ext.equals("mp4"))
			type = "video/mp4";
		return type;
	}

	/**
	 * Composes headers for a response.
	 * 
	 * @param start
	 *            The starting byte of the content
	 * @param quantity
	 *            The number of bytes being sent as content
	 * @param length
	 *            The length of the file being written
	 */
	private void makeHeaders(int start, int quantity, int length) {
		
		String connection = request.getHeader("Connection");
		if (connection != null && connection.equals("Close"))
			header += "Connection: Close\r\n";
		else
			header += "Connection: Keep-Alive\r\n";
		header += "Date: " + new Date().toString() + "\r\n";
		header += "Cache-Control: max-age=0\r\n";
		header += "Last-Modified: "
				+ new Date(target.lastModified()).toString() + "\r\n";
		header += "Content-Length: " + quantity + "\r\n";
		header += "Content-Range: bytes " + start + "-"
				+ (start + quantity - 1) + "/" + length + "\r\n";
		header += "Content-Type: " + this.getContentType(target.getName())
				+ "\r\n";
		header += "Accept-Ranges: bytes\r\n";
		header += "\r\n";
	}

	/**
	 * Getter for the response's header.
	 * 
	 * @return the header.
	 */
	public String getHeader() {
		return header;
	}

}
