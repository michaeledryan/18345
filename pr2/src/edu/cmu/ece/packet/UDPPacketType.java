package edu.cmu.ece.packet;

/*
 * This is a enum that self serializes so that it can be sent in the UDP header
 * automatically. Don't mind that this is probably awful, I don't really
 * understand java enums. Feel free to change it, please.
 */
public enum UDPPacketType {
	NONE(0), REQUEST(1), DATA(2);
	
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
		default:
			return UDPPacketType.NONE;
		}
	}

    public int getValue() {
        return value;
    }
}
