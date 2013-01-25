package edu.cmu.ece;

import server.HTTPServer;

public class Vodserver {

	public static void main(String[] args) {
		HTTPServer server = new HTTPServer(18345);
		server.run();
	}

}
