package edu.cmu.ece;

import java.io.File;

import server.HTTPServer;

public class Vodserver {

	public static void main(String[] args) {
		int port = 18345;

		File here = new File(".");
		System.out.println(here.getAbsolutePath());

		if (args.length > 0) {
			if (args.length > 1) {
				System.out.println("Discarding extra args.");
			}

			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.out.println("Port must be an integer");
				System.exit(1);
			}
		}

		HTTPServer server = new HTTPServer(port);
		server.run();
	}

}
