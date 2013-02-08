package edu.cmu.ece;

import edu.cmu.ece.server.HTTPServer;

/**
 * Main class. Executes the program.
 * 
 * @author Michaels
 */
public class Vodserver {

	/**
	 * Main method. Sets up the server.
	 * 
	 * @param args
	 *            port number
	 */
	public static void main(String[] args) {
		int port = 18345;

		// Parse args

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

		// Run server
		HTTPServer server = new HTTPServer(port);
		server.run();
	}

}
