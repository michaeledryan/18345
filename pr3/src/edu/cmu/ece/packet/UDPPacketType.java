package edu.cmu.ece.packet;

/*
 * This is a enum that self serializes so that it can be sent in the UDP header
 * automatically.
 * 
 * Packet types are as follows:
 * 		NONE: unused, mostly signals an error
 * 		REQUEST: a frontend server is requesting a file from the backend
 * 		DATA:    a backend server is sending data back to the frontend
 * 		END:     the final data message, no further in the sequence will come
 * 		CONFIG:  a message to a server to modify its internal configuration
 * 		ACK/NAK: positive/negative acknowledgments of a UDP packet received
 * 		KILL:    signal a client that hung up, kills his requests
 *      PEERING_REQUEST:
 *      	a neighbor in the peer graph wants to know this client's TCP name
 *          and port to negotiate a peer connection
 *      PEERING_RESPONSE:
 *      	a neighbor in the peer graph responds with the requester's UUID
 *          and the responder's port for that neighbor
 */
public enum UDPPacketType {
	NONE(0), REQUEST(1), DATA(2), CONFIG(3), END(4), ACK(5), NAK(6), KILL(7), PEERING_REQUEST(
			8), PEERING_RESPONSE(9);
	
	private final int value;
  
  private UDPPacketType(int value) {
      this.value = value;
  }

	public static UDPPacketType fromInt(int from) {
		switch (from) {
		case 1:
			return UDPPacketType.REQUEST;
		case 2:
			return UDPPacketType.DATA;
		case 3:
			return UDPPacketType.CONFIG;
		case 4:
			return UDPPacketType.END;
		case 5:
			return UDPPacketType.ACK;
		case 6:
			return UDPPacketType.NAK;
		case 7:
			return UDPPacketType.KILL;
		case 8:
			return UDPPacketType.PEERING_REQUEST;
		case 9:
			return UDPPacketType.PEERING_RESPONSE;
		default:
			return UDPPacketType.NONE;
		}
	}

    public int getValue() {
        return value;
    }
}
