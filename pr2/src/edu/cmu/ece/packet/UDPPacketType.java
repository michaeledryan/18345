package edu.cmu.ece.packet;

/*
 * This is a enum that self serializes so that it can be sent in the UDP header
 * automatically. Don't mind that this is probably awful, I don't really
 * understand java enums. Feel free to change it, please.
 * 
 * I don't get Java enums either... I added END to signify EOF.
 */
public enum UDPPacketType {
	NONE(0), REQUEST(1), DATA(2), CONFIG(3), END (4), ACK(5), NAK(6);
	
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
		default:
			return UDPPacketType.NONE;
		}
	}

    public int getValue() {
        return value;
    }
}
