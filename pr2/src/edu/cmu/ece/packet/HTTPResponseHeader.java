package edu.cmu.ece.packet;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class HTTPResponseHeader {
	/**
	 * Returns the valid html content type when given a filename.
	 * 
	 * @param filename
	 *            the filename to be parsed
	 * @return the html content type
	 */
	public static String getContentType(String filename) {
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
	 * Get the date in proper network format.
	 * 
	 * @return the date
	 */
	public static String formatDate(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(date);
	}

	/**
	 * Composes headers for a response.
	 * 
	 * @param target
	 *            The target file we are sending
	 * @param length
	 *            The length of the file being written
	 */
	public static String makeHeader(File target, HTTPRequestPacket request) {
		String header;
		
		// Determine header type
		if (request.getHeader("Range") == null)
			header = "HTTP/1.1 200 OK\r\n";
		else
			header = "HTTP/1.1 206 Partial Content\r\n";

		// Handle connection type
		String connection = request.getHeader("Connection");
		if (connection != null && connection.equals("Close"))
			header += "Connection: Close\r\n";
		else
			header += "Connection: Keep-Alive\r\n";

		header += "Date: " + formatDate(new Date()) + "\r\n";
		header += "Cache-Control: max-age=0\r\n";
		header += "Last-Modified: "
				+ formatDate(new Date(target.lastModified())) + "\r\n";
		header += "Content-Length: " + request.getTotalRangeLength() + "\r\n";
		header += "Content-Range: bytes " + request.getFullRangeString()
				+ "\r\n";
		header += "Content-Type: " + getContentType(target.getName())
				+ "\r\n";
		header += "Accept-Ranges: bytes\r\n";
		header += "\r\n";

		return header;
	}
}
