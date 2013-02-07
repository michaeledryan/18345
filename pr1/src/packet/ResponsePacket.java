package packet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.Date;

/**
 * Assembles a response to the GET request.
 */
public class ResponsePacket {
	private RequestPacket request;
	private OutputStream out;
	private PrintWriter textOut;
	private File target;

	private String header;

	public ResponsePacket(RequestPacket request, OutputStream output,
			PrintWriter textoutput) {
		this.request = request;
		this.out = output;
		this.textOut = textoutput;
	}

	public Boolean sendResponse() {
		String filename = "www/" + request.getRequest();
		target = new File(filename);
		if (!target.exists()) {
			this.send404();
			return true;
		}


		/*
		 * TODO: find a better way to copy then reading the entire array into a
		 * buffer then pushing back out
		 */

		String[] range;
		int lowerLimit = 0;
		int length = (int) target.length();


		if (request.getHeader("Range") != null) {
			header = "HTTP/1.1 206 Partial Content\r\n";
			System.out.println("Range header:" + request.getHeader("Range"));
			range = request.getHeader("Range").split("=")[1].split("-");
			lowerLimit = Integer.parseInt(range[0]);
			if (range.length > 1)
				length = Integer.parseInt(range[1]) - lowerLimit;
			else
				length -= lowerLimit;
			System.out.format("Computed range: %d-%d\n\n", lowerLimit,
					lowerLimit + length - 1);
		} else {
			header = "HTTP/1.1 200 OK\r\n";
		}

		this.makeHeaders(lowerLimit, length, (int) target.length());
		System.out.println(header);
		textOut.write(header);
		textOut.flush();

		byte[] buffer = new byte[length];
		try {
			FileInputStream file = new FileInputStream(target);
			file.skip(lowerLimit);
			file.read(buffer, 0, length);
			out.write(buffer, 0, length);
			out.flush();
			file.close();
		} catch (FileNotFoundException e) {
			System.out.println("Couldn't find file. Should have 404'd.");
		} catch (IndexOutOfBoundsException e) {
			System.out.println("Failed to copy to buffer: " + e.getMessage());
		} catch (SocketException e) {
			System.out.println("Failed to write to socket: " + e.getMessage());
			return false;
		} catch (IOException e) {
			System.out.println("Could not read/write file: " + e.getMessage());
		}
		return true;
	}

	private void send404() {
		header = "HTTP/1.1 404 Not Found\r\n";
		String connection = request.getHeader("Connection");
		if (connection != null && connection.equalsIgnoreCase("close"))
			header += "Connection: Close\r\n";
		else
			header += "Connection: Keep-Alive\r\n";
		header += "Date: " + new Date().toString() + "\r\n";

		System.out.println("\n\nPrinting 404 header:");
		System.out.println(header);
		textOut.write(header);
		textOut.flush();
	}

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

	/*
	 * Not sure if we need this, but I figured I'd mimic breaking down the other
	 * packet
	 */
	private void makeHeaders(int start, int quantity, int length) {
		System.out.println("\n\nPrinting response header:");

		String connection = request.getHeader("Connection");
		if (connection != null && connection.equals("Close"))
			header += "Connection: Close\r\n";
		else
			header += "Connection: Keep-Alive\r\n";
		header += "Date: " + new Date().toString() + "\r\n";
		header += "Cache-Control: max-age=0\r\n";
		header += "Content-Length: " + length + "\r\n";
		// if (length > 0)
		// header += "X-Content-Duration:" + 30.0 + "\r\n";
		header += "Content-Range: bytes " + start + "-"
				+ (start + quantity - 1) + "/" + length + "\r\n";
		header += "Content-Type: " + this.getContentType(target.getName())
				+ "\r\n";
		header += "Accept-Ranges: bytes\r\n";
		header += "\r\n";
	}

	public String getHeader() {
		return header;
	}

}
