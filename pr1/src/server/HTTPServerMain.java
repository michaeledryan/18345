package server;

public class HTTPServerMain {

	public static void main(String[] args) {
		HTTPServer server = new HTTPServer(18345);
		server.run();
	}

}
