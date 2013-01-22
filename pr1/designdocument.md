# Project 1 - HTTP Server Design Documentation
By Michael Nye and Michael Ryan

## General Design

Our server is designed as a multithreaded application that launches one thread
to handle each client request. It has a single listener that passes off each
connection request to a new thread. This thread parses the request header,
then passes the file request off to a file manager. This manager finds the
file and sets up a file buffer to send back to the client.

- - -

### HTTPServerMain

Our main function simply starts an instance of our `HTTPServer` class.

### HTTPServer

This class sets up a socket listener, and blocks while waiting for a client.
When a client comes up, it spins off a new `ClientHandler` thread, and then
blocks while waiting for the next client request.

### ClientHandler

The `ClientHandler` class is glue for our main functionality. Once it receives
a client, it sets up a read/write stream, then creates a `RequestPacket` to
parse the incoming response. It then passes this `RequestPacket` to a
`FileManager` that searches for the file and returns to our handler a data
stream. Finally, this stream is given to a `ResponsePacket` that packages
the data and sends it back to the client.

### RequestPacket

The `RequestPacket` is a class for parsing and reading the data out of a request
from the client. It reads the socket input to construct the packet, and then
makes the packet information available.

### FileManager

The `FileManager` is responsible for finding a file, determining its size
and other metadata, and establishing a file handle at the requested location
for the `ResponsePacket` to send back data.

### ResponsePacket

The `ResponsePacket` constructs a response to the client with a given data
handle and sends it back out the socket.